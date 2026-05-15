package org.openelisglobal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ExcludeTableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.login.valueholder.UserSessionData;
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

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private org.openelisglobal.observationhistory.service.ObservationHistoryService observationHistoryService;

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
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = jsonConverter.getObjectMapper();
        return objectMapper.writeValueAsString(obj);
    }

    public <T> T mapFromJson(String json, Class<T> clazz) throws IOException {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = jsonConverter.getObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return objectMapper.readValue(json, clazz);
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

        IDatabaseConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = new DatabaseConnection(dataSource.getConnection());
            DatabaseConfig config = connection.getConfig();
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
            config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());

            inputStream = getClass().getClassLoader().getResourceAsStream(datasetFileName);

            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
            }

            // Strip PROTECTED_SEED_TABLES from the loaded dataset BEFORE truncating
            // or refreshing. This makes the static seed (reference_tables, etc.)
            // immune to fixture-load wipes — see PROTECTED_SEED_TABLES javadoc.
            // Any <reference_tables> rows declared by a fixture are silently
            // ignored; the SQL-seeded row stays in place.
            IDataSet dataset = new FilteredDataSet(new ExcludeTableFilter(PROTECTED_SEED_TABLES),
                    new FlatXmlDataSet(inputStream));
            String[] tableNames = dataset.getTableNames();
            cleanRowsInCurrentConnection(tableNames);

            DatabaseOperation.REFRESH.execute(connection, dataset);

            // Refresh StatusService cache to pick up any status_of_sample changes
            // from the loaded test data
            if (statusService != null) {
                statusService.refreshCache();
            }
            // Same for ObservationHistoryService — it caches ObservationType → id
            // on first call and never invalidates unless asked. Without this,
            // earlier-running test classes' fixtures pin a stale mapping.
            if (observationHistoryService != null) {
                observationHistoryService.refreshCache();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Helper method to clear out all rows in specified tables within the given
     * dataset in the current connection.
     *
     * @param tableNames The names of the tables to truncate.
     * @throws SQLException If an error occurs during truncation.
     */
    protected void cleanRowsInCurrentConnection(String[] tableNames) throws SQLException, DatabaseUnitException {
        IDatabaseConnection connection = new DatabaseConnection(dataSource.getConnection());
        try (Connection conn = connection.getConnection(); Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                String truncateQuery = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE";
                logger.info("Truncating table: {}", tableName);
                stmt.execute(truncateQuery);
            }
        }
    }
}
