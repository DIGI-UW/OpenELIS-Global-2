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
import org.springframework.http.MediaType;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
            { "patient", "patient_seq" }, { "sample", "sample_seq" }, { "sample_item", "sample_item_seq" },
            { "sample_human", "sample_human_seq" }, { "analysis", "analysis_seq" }, { "result", "result_seq" },
            { "inventory_item", "inventory_item_seq" }, { "observation_history", "observation_history_seq" },
            { "image", "image_seq" }, { "organization", "organization_seq" }, { "analyzer", "analyzer_seq" },
            { "analyzer_mapping", "analyzer_mapping_seq" },
            { "referral_status_history", "referral_status_history_seq" }, { "calculation", "calculation_seq" },
            { "result_limits", "result_limits_seq" }, { "site_information", "site_information_seq" },
            { "reflex_rule", "reflex_rule_seq" }, { "reflex_rule_condition", "reflex_rule_condition_seq" },
            { "reflex_rule_action", "reflex_rule_action_seq" }, { "provider", "provider_seq" },
            { "nc_event", "nc_event_id_seq" } };

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

    private boolean loadedTransactionalFixture;

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
                throw new IllegalStateException("Fixture connection must share the application transaction");
            }
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
            }
            IDatabaseConnection dbUnitConn = buildDbUnitConnection(jdbcConn);
            // Preserve Liquibase-owned seed rows and load the fixture on the same
            // connection as application writes in a transactional test.
            IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
                    new FlatXmlDataSetBuilder().setColumnSensing(true).build(inputStream));
            truncateTablesInConnection(jdbcConn, dataset.getTableNames());
            DatabaseOperation.REFRESH.execute(dbUnitConn, dataset);
            synchronizeFixtureSequences(jdbcConn, dataset.getTableNames());
            resyncSequencesForTables(jdbcConn, dataset.getTableNames());
            if (participatesInTestTransaction) {
                ensureAuditSystemUser();
                ensureReferenceSeedRows();
                loadedTransactionalFixture = true;
            } else {
                jdbcConn.commit();
                ensureAuditSystemUser();
                ensureReferenceSeedRows();
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
        // A fixture user without a version is considered transient by Hibernate.
        jdbcTemplate.update("UPDATE system_user SET lastupdated = now() WHERE lastupdated IS NULL");
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
            // Sequence allocation is not rolled back with the fixture transaction.
            stmt.execute("SELECT setval('" + sequence + "', GREATEST(" + "CAST(COALESCE((SELECT MAX(id) FROM " + table
                    + "), 0) + 1 AS BIGINT), " + "(SELECT last_value + CASE WHEN is_called THEN 1 ELSE 0 END FROM "
                    + sequence + ")), false)");
        }
    }

    /**
     * Truncates specified test tables while skipping protected Liquibase seed
     * tables in {@link #PROTECTED_SEED_TABLES}. Delegates to
     * {@link #truncateTablesInConnection(Connection, String[])}.
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
     * Idempotently ensure {@code clinlims.reference_tables} has a row with the
     * given name (case-insensitive) and return its id. Looks up via the service,
     * inserts via raw JDBC if absent. Used in tests whose DbUnit fixture truncates
     * {@code reference_tables} as a side effect, ahead of code paths that
     * audit-emit and require the row to exist.
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
     * Convenience: seed multiple reference_tables names in one call. See
     * {@link #ensureReferenceTable(String)}.
     */
    protected void ensureReferenceTables(String... names) {
        for (String name : names) {
            ensureReferenceTable(name);
        }
    }

    /**
     * Move a Postgres sequence forward to {@code MAX(id)+1} of its table using an
     * existing connection. Never moves it backwards: several of these sequences are
     * declared {@code CACHE 20}, so each pooled connection holds a block of values
     * it has not handed out yet. Rewinding the sequence into a block another
     * connection is still holding makes both connections issue the same id, and the
     * loser fails on the primary key an insert or two later, in whichever test
     * class happens to run next.
     */
    protected void resyncSequence(Connection conn, String sequence, String table) {
        try (Statement st = conn.createStatement()) {
            // id columns are numeric(10); setval needs a bigint.
            st.execute("SELECT setval('" + sequence + "', GREATEST((SELECT last_value FROM " + sequence
                    + "), (SELECT COALESCE(MAX(id), 0) + 1 FROM " + table + "))::bigint, false)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resync sequence " + sequence + " from " + table, e);
        }
    }

    /**
     * Move a Postgres sequence forward to {@code MAX(id)+1} of its table. DBUnit
     * fixture loads insert rows with explicit ids without advancing the sequence,
     * so a later sequence-backed insert can collide with a seeded id depending on
     * test order (e.g. {@code person_pk id=2 already exists}). Call this before
     * sequence-backed inserts into a fixture-seeded table.
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
     * Synchronize conventionally named numeric fixture sequences on the fixture's
     * own connection, including inside a rollback-managed test transaction.
     */
    private void resyncSequencesForTables(Connection conn, String[] tableNames) throws SQLException {
        for (String table : tableNames) {
            String tableName = table.toLowerCase(java.util.Locale.ROOT);
            String sequence = tableName + "_seq";
            try (java.sql.PreparedStatement check = conn.prepareStatement("SELECT 1 FROM pg_class c"
                    + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                    + " JOIN information_schema.columns col ON col.table_name = ? AND col.column_name = 'id'"
                    + "   AND col.table_schema = 'clinlims' AND col.data_type IN ('numeric', 'integer', 'bigint')"
                    + " WHERE c.relkind = 'S' AND c.relname = ? AND n.nspname = 'clinlims'")) {
                check.setString(1, tableName);
                check.setString(2, sequence);
                try (java.sql.ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        continue;
                    }
                }
            }
            // Leave an already-ahead sequence alone: setval invalidates cached
            // allocations even when no fixture ID requires an adjustment.
            try (Statement check = conn.createStatement();
                    java.sql.ResultSet rows = check.executeQuery("SELECT COALESCE(MAX(id), 0) >= "
                            + "(SELECT last_value FROM clinlims." + sequence + ") FROM clinlims." + tableName)) {
                rows.next();
                if (rows.getBoolean(1)) {
                    synchronizeSequence(conn, "clinlims." + sequence, "clinlims." + tableName);
                }
            }
        }
    }

    /**
     * Reference vocabularies the production Liquibase seed guarantees but a fixture
     * load can silently gut: {@code executeDataSetWithStateManagement} truncates
     * every table a dataset names and re-inserts only the dataset's own rows, so a
     * dataset declaring a partial {@code type_of_test_result} leaves later suites
     * without rows their inserts FK to (test_result_type_fk). Restore the seed
     * after every load, like {@link #ensureAuditSystemUser}, by id so a fixture's
     * own extra rows are left alone. {@code requester_type} needs no restore here:
     * it is in {@link #PROTECTED_SEED_TABLES}, so a dataset declaring it is
     * stripped before the truncation rather than after.
     */
    private void ensureReferenceSeedRows() throws SQLException {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO clinlims.type_of_test_result (id, test_result_type, description, lastupdated,"
                    + " hl7_value) VALUES" + " (1, 'R', 'Remark', now(), 'TX'), (2, 'D', 'Dictionary', now(), 'TX'),"
                    + " (3, 'T', 'Titer', now(), 'TX'), (4, 'N', 'Numeric', now(), 'NM'),"
                    + " (5, 'A', 'Alpha,no range check', now(), 'TX'), (6, 'M', 'Multiselect', now(), 'TX'),"
                    + " (7, 'C', 'Cascading Multiselect', now(), 'TX')" + " ON CONFLICT (id) DO NOTHING");
            // The record-status pair every sample/patient status write FKs to.
            // ObservationHistoryService caches the name->id mapping at first use, so
            // after a fixture guts this table the cached ids (15/16) FK-fail on
            // insert — restoring by exact id is the only repair that honours the
            // cache. Fixtures only ever declare ids 1-5, so no conflict.
            st.execute("INSERT INTO clinlims.observation_history_type (id, type_name, description, lastupdated)"
                    + " VALUES (15, 'SampleRecordStatus', 'Sample Record Status', now()),"
                    + " (16, 'PatientRecordStatus', 'Patient Record Status', now())" + " ON CONFLICT (id) DO NOTHING");
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Idempotently ensure the audit user {@code system_user.id=1} ("admin") exists,
     * inserting it via raw JDBC (no audit emission) if absent. Audit-emitting
     * service calls stamp history with {@code sys_user_id=1}
     * ({@link #TEST_SYS_USER_ID}); a sibling test that truncates
     * {@code system_user} to its own fixture rows wipes this seed, so an
     * audit-dependent test must ensure it rather than assume the global seed
     * survives a prior test's fixture load.
     */
    protected void ensureAuditSystemUser() {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            try (java.sql.PreparedStatement check = conn
                    .prepareStatement("SELECT 1 FROM clinlims.system_user WHERE id = 1");
                    java.sql.ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
            try (java.sql.PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO clinlims.system_user (id, external_id, login_name, last_name, first_name, "
                            + "initials, is_active, is_employee, lastupdated) "
                            + "VALUES (1, '1', 'admin', 'ELIS', 'Open', 'OE', 'Y', 'Y', now())")) {
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure audit system_user id=1", e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    /**
     * Idempotently ensure at least one {@code clinlims.site_information} row
     * exists, inserting one (with a domain row for the FK) via raw JDBC if the
     * table is empty. For audit tests that update a seed-provided site_information
     * row but do not own that seed — a sibling fixture's
     * {@code TRUNCATE ... CASCADE} can wipe it.
     */
    protected void ensureSiteInformationPresent() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM clinlims.site_information")) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
            String domainId;
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT id FROM clinlims.site_information_domain LIMIT 1")) {
                if (rs.next()) {
                    domainId = rs.getString(1);
                } else {
                    stmt.execute("INSERT INTO clinlims.site_information_domain (id, name, description) VALUES "
                            + "(nextval('clinlims.site_information_domain_seq'), 'auditRegressionDomain', "
                            + "'ensured by test')");
                    try (java.sql.ResultSet r2 = stmt
                            .executeQuery("SELECT id FROM clinlims.site_information_domain LIMIT 1")) {
                        r2.next();
                        domainId = r2.getString(1);
                    }
                }
            }
            stmt.execute("INSERT INTO clinlims.site_information (id, name, value, value_type, domain_id, lastupdated) "
                    + "VALUES (nextval('clinlims.site_information_seq'), 'auditRegressionMarker', 'seed', 'text', "
                    + domainId + ", now())");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure a site_information row", e);
        }
    }

    /**
     * Idempotently ensure the named {@code clinlims.site_information} row exists
     * with the given value, inserting it (value_type {@code 'text'}, null domain)
     * via raw JDBC if absent. For config-backed tests that read a migration-seeded
     * site_information row but do not own that seed: a sibling fixture's
     * {@code TRUNCATE site_information ... CASCADE} runs committed on a separate
     * connection (outside the test's {@code @Rollback} transaction), so it
     * permanently deletes the row for the rest of the JVM run. {@code domain_id} is
     * nullable and unused by {@code ConfigurationProperties}, so it is left null.
     * Insert-if-absent only — an existing row's value is left untouched.
     */
    protected void ensureSiteInformation(String name, String value) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.site_information (id, name, value, value_type, lastupdated) "
                        + "SELECT nextval('clinlims.site_information_seq'), ?, ?, 'text', now() "
                        + "WHERE NOT EXISTS (SELECT 1 FROM clinlims.site_information WHERE name = ?)",
                name, value, name);
    }

    /**
     * Resync all tracked entity sequences to MAX(id)+1. Convenience method for
     * tests that insert multiple records programmatically across different
     * entities.
     */
    protected void resyncAllSequences() {
        try (Connection conn = dataSource.getConnection()) {
            for (String[] mapping : FIXTURE_SEQUENCE_MAPPINGS) {
                resyncSequence(conn, "clinlims." + mapping[1], "clinlims." + mapping[0]);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to resync all sequence mappings", e);
        }
    }

    /**
     * Helper for MockMvc GET requests pre-configured with JSON headers.
     *
     * @param url the endpoint URL
     * @return ResultActions to perform assertions on
     */
    protected ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    /**
     * Helper for MockMvc POST requests pre-configured with JSON body and headers.
     *
     * @param url     the endpoint URL
     * @param content the object payload to serialize as JSON
     * @return ResultActions to perform assertions on
     */
    protected ResultActions performPost(String url, Object content) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).content(mapToJson(content)));
    }
}
