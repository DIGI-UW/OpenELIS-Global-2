package org.openelisglobal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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

    private static final ObjectMapper JSON_OBJECT_MAPPER;

    static {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        JSON_OBJECT_MAPPER = jsonConverter.getObjectMapper();
        JSON_OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    @Before
    public void setDefaultTestAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_RESULTS"))));
    }

    @After
    public void clearTestAuthentication() {
        SecurityContextHolder.clearContext();
    }

    protected void setUp() throws Exception {
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
        sessionData.setSytemUserId(1);

        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    protected String mapToJson(Object obj) throws JsonProcessingException {
        return JSON_OBJECT_MAPPER.writeValueAsString(obj);
    }

    public <T> T mapFromJson(String json, Class<T> clazz) throws IOException {
        return JSON_OBJECT_MAPPER.readValue(json, clazz);
    }

    protected void executeDataSetWithStateManagement(String... datasetFileNames) throws Exception {
        Objects.requireNonNull(datasetFileNames, "datasetFileNames");
        if (datasetFileNames.length == 0) {
            throw new IllegalArgumentException("At least one dataset file is required");
        }

        List<IDataSet> datasets = new ArrayList<>();
        Set<String> tableNames = new LinkedHashSet<>();
        for (String datasetFileName : datasetFileNames) {
            IDataSet dataset = loadDatasetFromClasspath(datasetFileName);
            datasets.add(dataset);
            for (String tableName : dataset.getTableNames()) {
                tableNames.add(tableName);
            }
        }

        IDatabaseConnection dbUnitConnection = new DatabaseConnection(dataSource.getConnection());
        try {
            Connection jdbcConnection = dbUnitConnection.getConnection();
            jdbcConnection.setAutoCommit(false);
            configureDatabaseConnection(dbUnitConnection);

            truncateTablesOnConnection(jdbcConnection, tableNames.toArray(new String[0]));
            for (IDataSet dataset : datasets) {
                DatabaseOperation.INSERT.execute(dbUnitConnection, dataset);
            }
            ensureBootstrapSystemUser(jdbcConnection);
            jdbcConnection.commit();

            if (statusService != null) {
                statusService.refreshCache();
            }
        } catch (Exception e) {
            rollbackQuietly(dbUnitConnection);
            throw e;
        } finally {
            dbUnitConnection.close();
        }
    }

    protected void cleanRowsInCurrentConnection(String[] tableNames) throws SQLException, DatabaseUnitException {
        if (tableNames == null || tableNames.length == 0) {
            return;
        }
        IDatabaseConnection dbUnitConnection = new DatabaseConnection(dataSource.getConnection());
        try {
            truncateTablesOnConnection(dbUnitConnection.getConnection(), tableNames);
        } finally {
            dbUnitConnection.close();
        }
    }

    private IDataSet loadDatasetFromClasspath(String datasetFileName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(datasetFileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
            }
            return new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
                    new FlatXmlDataSetBuilder().setColumnSensing(true).build(inputStream));
        }
    }

    private void configureDatabaseConnection(IDatabaseConnection connection) {
        DatabaseConfig config = connection.getConfig();
        config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
        config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
    }

    private void truncateTablesOnConnection(Connection connection, String[] tableNames) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (String tableName : tableNames) {
                String truncateQuery = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE";
                logger.info("Truncating table: {}", tableName);
                stmt.execute(truncateQuery);
            }
        }
    }

    private void rollbackQuietly(IDatabaseConnection dbUnitConnection) {
        try {
            Connection connection = dbUnitConnection.getConnection();
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            logger.warn("Failed to roll back fixture load transaction", e);
        }
    }

    protected void ensureBootstrapSystemUser(Connection connection) throws SQLException {
        try (java.sql.PreparedStatement select = connection
                .prepareStatement("SELECT COUNT(*) FROM clinlims.system_user WHERE id = 1");
                java.sql.PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO clinlims.system_user (id, login_name, first_name, last_name, is_active, is_employee, lastupdated) "
                                + "VALUES (1, 'admin', 'John', 'Doe', 'Y', 'Y', NOW())")) {
            try (java.sql.ResultSet rs = select.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insert.executeUpdate();
                }
            }
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
