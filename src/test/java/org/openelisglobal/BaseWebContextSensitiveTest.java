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
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
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
     */
    private static final String[] PROTECTED_SEED_TABLES = { "reference_tables" };

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

    @Autowired(required = false)
    private ReferenceTablesService referenceTablesService;

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
    public void setDefaultTestAuthentication() {
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

        InputStream inputStream = null;

        // Use a single JDBC connection for both TRUNCATE and REFRESH so that
        // if REFRESH fails the truncation can be rolled back and the next test
        // does not start with an empty database.
        try (Connection jdbcConn = dataSource.getConnection()) {
            jdbcConn.setAutoCommit(false);
            IDatabaseConnection dbUnitConn = buildDbUnitConnection(jdbcConn);
            try {
                inputStream = getClass().getClassLoader().getResourceAsStream(datasetFileName);

                if (inputStream == null) {
                    throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
                }

                // Strip PROTECTED_SEED_TABLES from the loaded dataset BEFORE truncating
                // or refreshing. This makes the static seed (reference_tables, etc.)
                // immune to fixture-load wipes — see PROTECTED_SEED_TABLES javadoc.
                // Any <reference_tables> rows declared by a fixture are silently
                // ignored; the SQL-seeded row stays in place.
                // Column sensing scans ALL rows to build the column list, so a mistyped
                // attribute on any row (e.g. pws_d vs pws_id) is caught immediately as a
                // hard PSQLException instead of being silently dropped.
                IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
                        new FlatXmlDataSetBuilder().setColumnSensing(true).build(inputStream));

                truncateTablesInConnection(jdbcConn, dataset.getTableNames());
                DatabaseOperation.REFRESH.execute(dbUnitConn, dataset);
                jdbcConn.commit();

                // Refresh StatusService cache to pick up any status_of_sample changes
                // from the loaded test data
                if (statusService != null) {
                    statusService.refreshCache();
                }
            } catch (Exception e) {
                jdbcConn.rollback();
                throw e;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        }
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
            for (String tableName : tableNames) {
                stmt.execute("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
                logger.debug("Truncating table: {}", tableName);
            }
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
        try (Connection conn = dataSource.getConnection()) {
            truncateTablesInConnection(conn, safeTableNames);
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
        if (referenceTablesService != null) {
            ReferenceTables existing = referenceTablesService.getReferenceTableByName(name);
            if (existing != null) {
                return existing.getId();
            }
        }
        try (Connection conn = dataSource.getConnection();
                java.sql.PreparedStatement insert = conn
                        .prepareStatement("INSERT INTO clinlims.reference_tables (id, name, keep_history) "
                                + "VALUES (nextval('clinlims.reference_tables_seq'), ?, 'Y')")) {
            insert.setString(1, name);
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed reference_tables row for " + name, e);
        }
        if (referenceTablesService != null) {
            ReferenceTables seeded = referenceTablesService.getReferenceTableByName(name);
            if (seeded != null) {
                return seeded.getId();
            }
        }
        // Fall back to raw lookup if the service bean isn't wired (rare in unit
        // tests that lookup post-seed).
        try (Connection conn = dataSource.getConnection();
                java.sql.PreparedStatement select = conn.prepareStatement(
                        "SELECT id FROM clinlims.reference_tables WHERE LOWER(name) = LOWER(?) LIMIT 1")) {
            select.setString(1, name);
            try (java.sql.ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up seeded reference_tables row for " + name, e);
        }
        throw new IllegalStateException("Reference table row for '" + name + "' is still missing after seed attempt");
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
     * Idempotently ensure the audit user {@code system_user.id=1} ("admin") exists,
     * inserting it via raw JDBC (no audit emission) if absent. Audit-emitting
     * service calls stamp history with {@code sys_user_id=1}
     * ({@link #TEST_SYS_USER_ID}); a sibling test that truncates
     * {@code system_user} to its own fixture rows wipes this seed, so an
     * audit-dependent test must ensure it rather than assume the global seed
     * survives a prior test's fixture load.
     */
    protected void ensureAuditSystemUser() {
        try (Connection conn = dataSource.getConnection()) {
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
}
