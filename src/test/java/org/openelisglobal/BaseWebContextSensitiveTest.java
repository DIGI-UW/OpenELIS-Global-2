package org.openelisglobal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ExcludeTableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.security.WithDaemonUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = { BaseTestConfig.class, AppTestConfig.class })
@WebAppConfiguration
@TestPropertySource("classpath:common.properties")
@ActiveProfiles("test")
@WithDaemonUser
public abstract class BaseWebContextSensitiveTest extends AbstractTransactionalJUnit4SpringContextTests {

    Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Tables that are static seeds — fixture loads must never truncate or replace
     * them. {@code reference_tables} is populated by Liquibase at DB init with ~136
     * rows (PATIENT, PERSON, DICTIONARY, BARCODE_LABEL_INFO, ANALYSIS, NCE_EVENT,
     * etc.). Every audit-emitting service (post PR #3591) does an
     * {@code AuditTrailServiceImpl.saveNewHistory} lookup keyed on its
     * ref_table_name; if a fixture loader truncates the seed and re-inserts only
     * the fixture's handful of rows, every downstream test that audits an entity
     * blows up with "Reference Table is null". The bug is surefire-order-dependent
     * and was masked until PR #3591 (2026-05-13) opted 14 P0 services into
     * audit-emit. Filter at the loader so the seed is untouchable regardless of
     * which fixture declares which rows.
     *
     * <p>
     * {@code requester_type} is the same class of bug (2026-07-07):
     * {@code TableIdService} resolves {@code ORGANIZATION_REQUESTER_TYPE_ID} /
     * {@code PROVIDER_REQUESTER_TYPE_ID} /
     * {@code REQUESTOR_CONTACT_REQUESTER_TYPE_ID} once, from the Liquibase-seeded
     * rows, at Spring context startup. Several fixtures
     * ({@code testdata/facade-servicerequest.xml}, {@code testdata/requester.xml})
     * declare their own fictional {@code <requester_type>} rows (with ids/names
     * that don't match the real seed at all — e.g.
     * {@code LABORATORY/CLINIC/HOSPITAL}) purely so their own
     * {@code <sample_requester>} rows have *some* id to reference; without this
     * protection, loading one of those fixtures truncates the real seed
     * ({@code RESTART IDENTITY CASCADE}) and replaces it with the fixture's
     * fictional rows, permanently corrupting {@code requester_type} — including
     * deleting {@code requestor_contact} — for every later test class in the same
     * Surefire fork, since {@code TableIdService} never re-resolves. Protect the
     * seed the same way. Note the real seeded id for {@code requestor_contact} is
     * {@code 4}, not {@code 3} — the {@code 2.6.x.x/fix_sequences.xml} changeset
     * runs before {@code 3.5.x.x/053-requester-element-revamp.xml} in the changelog
     * and advances {@code requester_type_seq} to {@code MAX(id)+1=3} (i.e. the next
     * {@code nextval()} returns {@code 4}) as it does for every sequence in the
     * schema on a fresh test DB.
     */
    private static final String[] PROTECTED_SEED_TABLES = { "reference_tables", "requester_type", "label_preset",
            "observation_history_type" };

    /**
     * Legacy entities whose Hibernate generators use standalone sequences and whose
     * fixtures are followed by service-created records in this test suite. Keep
     * this list explicit: resetting every inferred table sequence can rewind
     * unrelated PostgreSQL sequences while other pooled connections still hold
     * cached values.
     */
    private static final String[][] FIXTURE_SEQUENCE_MAPPINGS = { { "person", "person_seq" },
            { "patient", "patient_seq" }, { "patient_identity", "patient_identity_seq" },
            { "nc_event", "nc_event_id_seq" }, { "sample", "sample_seq" }, { "sample_item", "sample_item_seq" },
            { "sample_human", "sample_human_seq" }, { "analysis", "analysis_seq" }, { "result", "result_seq" },
            { "inventory_item", "inventory_item_seq" }, { "observation_history", "observation_history_seq" } };

    /**
     * Default sys_user_id for audit-emitting service calls in tests. Matches the
     * {@code system_user.id=1} ("admin") row that {@code testdata/system-user.xml}
     * and {@code postgre-db-init/OpenELIS-Global.sql} both seed. Tests that invoke
     * an audit-emitting service must either set this on the entity
     * (entity.setSysUserId(TEST_SYS_USER_ID)) or pass it explicitly, otherwise
     * AuditTrailServiceImpl.saveNewHistory/saveHistory throws "System User ID is
     * null". Use this constant rather than a magic "1" so the intent ("the
     * bootstrap admin user the fixture loaders seed") stays visible.
     */
    protected static final String TEST_SYS_USER_ID = "1";

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private org.openelisglobal.observationhistory.service.ObservationHistoryService observationHistoryService;

    protected MockMvc mockMvc;

    /**
     * Reuses a shared {@link ObjectMapper} to avoid expensive repeated jackson
     * init.
     */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        OBJECT_MAPPER = jsonConverter.getObjectMapper();
        OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    @Before
    public void setDefaultTestAuthentication() throws Exception {
        // A cached test context is reused without another ApplicationContextAware
        // callback. Restore the legacy lookup after tests using another context.
        webApplicationContext.getBean(org.openelisglobal.spring.util.SpringContext.class)
                .setApplicationContext(webApplicationContext);
        // Ensure the "admin" SystemUser row exists so UserContextHolder can
        // resolve the principal set below (or by @WithMockUser(username="admin")
        // on individual tests). Without this, fillSysUserIdIfMissing throws
        // for any test whose own fixture doesn't include system_user.
        ensureBaselineSystemUserRows();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_RESULTS"))));
    }

    @After
    public void clearTestAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Initializes MockMvc before each test to prevent null instances when
     * subclasses omit super.setUp().
     */
    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    /**
     * Replace the SecurityContext principal with the given login name. Use this in
     * a subclass {@code @Before} (after {@code super}'s @Before set the admin
     * principal) when the test loads a fixture that replaces {@code system_user}
     * with its own users — e.g. {@code testUser}, {@code alice}. The login name
     * passed must match a {@code login_name} present in the test's loaded fixture
     * so {@link org.openelisglobal.common.util.UserContextHolder} can resolve it;
     * ROLE_ADMIN / ROLE_RESULTS authorities are granted so
     * 
     * @PreAuthorize-protected paths still pass.
     */
    protected void authenticateAs(String loginName) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginName, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_RESULTS"))));
    }

    /**
     * Builds a {@link MockHttpServletRequest} pre-configured for FHIR facade
     * endpoints. Sets the servlet path to {@code /fhir}, content type to
     * {@code application/fhir+json}, and the Accept header accordingly.
     *
     * @param method   the HTTP method (GET, POST, PUT, DELETE)
     * @param pathInfo the FHIR resource path (e.g. {@code /Patient/uuid})
     * @return a configured MockHttpServletRequest
     */
    protected MockHttpServletRequest buildFhirRequest(String method, String pathInfo) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setContextPath("");
        request.setServletPath("/fhir");
        request.setPathInfo(pathInfo);
        request.setRequestURI("/fhir" + pathInfo);
        request.setContentType("application/fhir+json");
        request.addHeader("Accept", "application/fhir+json");

        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(TEST_SYS_USER_ID));

        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    protected String mapToJson(Object obj) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    public <T> T mapFromJson(String json, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    /**
     * Executes a database test with the specified dataset and sequence reset
     * information.
     *
     * @param datasetFileName The filename of the dataset file in the classpath.
     * @throws Exception If an error occurs while executing the test.
     */
    protected void executeDataSetWithStateManagement(String datasetFileName) throws Exception {
        if (datasetFileName == null) {
            throw new NullPointerException("Please provide test dataset file to execute!");
        }

        Connection jdbcConn = DataSourceUtils.getConnection(dataSource);
        boolean participatesInTestTransaction = DataSourceUtils.isConnectionTransactional(jdbcConn, dataSource);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(datasetFileName)) {
            if (!participatesInTestTransaction) {
                jdbcConn.setAutoCommit(false);
            } else if (jdbcConn.getAutoCommit()) {
                throw new IllegalStateException("Fixture connection must share the application transaction; "
                        + "configure the EntityManagerFactory with the test DataSource");
            }
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
            }
            IDatabaseConnection dbUnitConn = buildDbUnitConnection(jdbcConn);
            IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
                    new FlatXmlDataSetBuilder().setColumnSensing(true).build(inputStream));

            // Fixture replacement and application writes must use the same test
            // transaction. A separate committed connection defeats @Transactional
            // rollback and leaves unrelated suites with truncated seed tables.
            truncateTablesInConnection(jdbcConn, dataset.getTableNames());
            DatabaseOperation.REFRESH.execute(dbUnitConn, dataset);
            synchronizeFixtureSequences(jdbcConn, dataset.getTableNames());
            if (participatesInTestTransaction) {
                loadedTransactionalFixture = true;
            } else {
                jdbcConn.commit();
                // Legacy nontransactional fixtures still own their committed setup.
                ensureAuditSystemUser();
            }
            refreshFixtureCaches();
        } catch (Exception e) {
            if (!participatesInTestTransaction) {
                jdbcConn.rollback();
            }
            throw e;
        } finally {
            DataSourceUtils.releaseConnection(jdbcConn, dataSource);
        }
    }

    private boolean loadedTransactionalFixture;

    @AfterTransaction
    public void refreshCachesAfterFixtureRollback() {
        if (loadedTransactionalFixture) {
            refreshFixtureCaches();
            loadedTransactionalFixture = false;
        }
    }

    private void refreshFixtureCaches() {
        if (statusService != null) {
            statusService.refreshCache();
        }
        if (observationHistoryService != null) {
            observationHistoryService.refreshTypeIdCache();
        }
    }

    /**
     * Ensure a {@code system_user} row with {@code login_name='admin'} exists so
     * the principal set by {@link #setDefaultTestAuthentication()} resolves via
     * {@link org.openelisglobal.common.util.UserContextHolder}. Idempotent: checks
     * by {@code login_name} (not by PK) to avoid creating a second admin when
     * another fixture already populated the row with a different id.
     *
     * <p>
     * Tests whose fixtures truncate {@code system_user} and load their own users
     * (e.g. {@code testUser}, {@code alice}) wipe this row; those tests should set
     * up a daemon SecurityContext in their own {@code @Before} to avoid the admin
     * DB lookup entirely.
     */
    private void ensureBaselineSystemUserRows() {
        jdbcTemplate.update("INSERT INTO system_user (id, external_id, login_name, last_name, first_name, initials, "
                + "is_active, is_employee, lastupdated) "
                + "SELECT nextval('system_user_seq'), 'TEST_ADMIN', 'admin', 'Doe', 'John', 'JD', 'Y', 'Y', now() "
                + "WHERE NOT EXISTS (SELECT 1 FROM system_user WHERE login_name = 'admin')");
    }

    /**
     * Wraps the supplied JDBC connection in a configured DBUnit
     * {@link IDatabaseConnection}. Accepts the caller's connection so that TRUNCATE
     * and REFRESH share the same transaction.
     *
     * @param jdbcConn an already-open JDBC connection owned by the caller
     * @return a fully configured {@link IDatabaseConnection}
     * @throws DatabaseUnitException if DBUnit fails to wrap the connection
     */
    private IDatabaseConnection buildDbUnitConnection(Connection jdbcConn) throws DatabaseUnitException {
        IDatabaseConnection connection = new DatabaseConnection(jdbcConn);
        DatabaseConfig config = connection.getConfig();
        config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
        return connection;
    }

    /**
     * Truncates the given tables using the supplied connection. Shared by
     * {@link #executeDataSetWithStateManagement} (inside the transactional fixture
     * load) and {@link #cleanRowsInCurrentConnection} (ad-hoc cleanup).
     *
     * @param conn       an open JDBC connection
     * @param tableNames the tables to truncate
     * @throws SQLException if any truncation fails
     */
    private void truncateTablesInConnection(Connection conn, String[] tableNames) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // A fixture ownership error must fail with a database diagnostic rather
            // than leave the test runner waiting indefinitely for its own locks.
            stmt.setQueryTimeout(30);
            for (String tableName : tableNames) {
                stmt.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
                logger.debug("Truncating table: {}", tableName);
            }
        }
    }

    /**
     * Advances standalone Hibernate sequences after DbUnit imports explicit IDs.
     * Without this, generated inserts depend on test order and can reuse a fixture
     * primary key.
     */
    private void synchronizeFixtureSequences(Connection conn, String[] tableNames) throws SQLException {
        Set<String> loadedTables = Arrays.stream(tableNames).map(name -> name.toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        for (String[] mapping : FIXTURE_SEQUENCE_MAPPINGS) {
            if (loadedTables.contains(mapping[0])) {
                synchronizeSequence(conn, "clinlims." + mapping[1], "clinlims." + mapping[0]);
            }
        }
    }

    private void synchronizeSequence(Connection conn, String sequence, String table) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(30);
            // PostgreSQL sequence allocation does not roll back. Never reuse an
            // allocated ID when the surrounding transaction restores larger rows.
            stmt.execute("SELECT setval('" + sequence + "', GREATEST(" + "CAST(COALESCE((SELECT MAX(id) FROM " + table
                    + "), 0) + 1 AS BIGINT), " + "(SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM "
                    + sequence + ")), false)");
        }
    }

    /**
     * Truncates specified test tables while skipping protected Liquibase seed
     * tables in {@link #PROTECTED_SEED_TABLES}. Joins the active test transaction
     * exactly as fixture loading does. Without a test transaction, this helper owns
     * and commits one atomic cleanup transaction.
     *
     * @param tableNames the tables to truncate
     * @throws SQLException if any truncation fails
     */
    protected void cleanRowsInCurrentConnection(String[] tableNames) throws SQLException {
        Set<String> protectedTables = Set.of(PROTECTED_SEED_TABLES);
        String[] safeTableNames = Arrays.stream(tableNames).filter(t -> !protectedTables.contains(t))
                .toArray(String[]::new);
        Connection conn = DataSourceUtils.getConnection(dataSource);
        boolean participatesInTestTransaction = DataSourceUtils.isConnectionTransactional(conn, dataSource);
        try {
            if (!participatesInTestTransaction) {
                conn.setAutoCommit(false);
            }
            truncateTablesInConnection(conn, safeTableNames);
            if (participatesInTestTransaction) {
                loadedTransactionalFixture = true;
            } else {
                conn.commit();
            }
            refreshFixtureCaches();
        } catch (SQLException e) {
            if (!participatesInTestTransaction) {
                conn.rollback();
            }
            throw e;
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Legacy fixture helper for an explicitly requested reference-table row. Uses
     * the test transaction when present; does not commit over its rollback. Tests
     * of migration seeds should assert their presence instead of repairing them
     * with this helper.
     */
    protected String ensureReferenceTable(String name) {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM clinlims.reference_tables WHERE LOWER(name) = LOWER(?)", String.class, name);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        return jdbcTemplate.queryForObject("INSERT INTO clinlims.reference_tables (id, name, keep_history) "
                + "VALUES (nextval('clinlims.reference_tables_seq'), ?, 'Y') RETURNING id", String.class, name);
    }

    /**
     * Ensure explicitly requested legacy fixture rows in the caller's transaction.
     */
    protected void ensureReferenceTables(String... names) {
        for (String name : names) {
            ensureReferenceTable(name);
        }
    }

    /**
     * Advance a fixture table's sequence beyond imported and previously allocated
     * IDs using the same connection as the fixture. Sequence allocation itself is
     * not rolled back by PostgreSQL.
     */
    protected void resyncSequence(String sequence, String table) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            synchronizeSequence(conn, sequence, table);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resync sequence " + sequence + " from " + table, e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Legacy fixture helper for audit actor 1. Joins the active test transaction;
     * tests replacing users should authenticate as their own fixture actor.
     */
    protected void ensureAuditSystemUser() {
        jdbcTemplate.update("INSERT INTO clinlims.system_user (id, external_id, login_name, last_name, first_name, "
                + "initials, is_active, is_employee, lastupdated) "
                + "SELECT 1, '1', 'admin', 'ELIS', 'Open', 'OE', 'Y', 'Y', now() "
                + "WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_user WHERE id = 1)");
    }

    /**
     * Legacy fixture helper for explicitly requested configuration. Joins the
     * active test transaction and preserves an existing value. Tests of migration
     * seeds should verify them rather than filling missing values here.
     */
    protected void ensureSiteInformation(String name, String value) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.site_information (id, name, value, value_type, lastupdated) "
                        + "SELECT nextval('clinlims.site_information_seq'), ?, ?, 'text', now() "
                        + "WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information WHERE name = ?)",
                name, value, name);
    }
}
