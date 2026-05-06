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
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
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
@WithDaemonUser
public abstract class BaseWebContextSensitiveTest extends AbstractTransactionalJUnit4SpringContextTests {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private IStatusService statusService;

    protected MockMvc mockMvc;

    @Before
    public void setDefaultTestAuthentication() throws Exception {
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

    protected void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }

    /**
     * Replace the SecurityContext principal with the given login name. Use this
     * in a subclass {@code @Before} (after {@code super}'s @Before set the admin
     * principal) when the test loads a fixture that replaces {@code system_user}
     * with its own users — e.g. {@code testUser}, {@code alice}. The login name
     * passed must match a {@code login_name} present in the test's loaded
     * fixture so {@link org.openelisglobal.common.util.UserContextHolder} can
     * resolve it; ROLE_ADMIN / ROLE_RESULTS authorities are granted so
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

            IDataSet dataset = new FlatXmlDataSet(inputStream);
            String[] tableNames = dataset.getTableNames();
            cleanRowsInCurrentConnection(tableNames);

            DatabaseOperation.REFRESH.execute(connection, dataset);

            // Refresh StatusService cache to pick up any status_of_sample changes
            // from the loaded test data
            if (statusService != null) {
                statusService.refreshCache();
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
     * Ensure a {@code system_user} row with {@code login_name='admin'} exists so
     * the principal set by {@link #setDefaultTestAuthentication()} resolves via
     * {@link org.openelisglobal.common.util.UserContextHolder}. Idempotent:
     * checks by {@code login_name} (not by PK) to avoid creating a second admin
     * when another fixture already populated the row with a different id.
     *
     * <p>
     * Tests whose fixtures truncate {@code system_user} and load their own users
     * (e.g. {@code testUser}, {@code alice}) wipe this row; those tests should
     * set up a daemon SecurityContext in their own {@code @Before} to avoid the
     * admin DB lookup entirely.
     */
    private void ensureBaselineSystemUserRows() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO system_user (id, external_id, login_name, last_name, first_name, initials, "
                    + "is_active, is_employee, lastupdated) "
                    + "SELECT nextval('system_user_seq'), 'TEST_ADMIN', 'admin', 'Doe', 'John', 'JD', 'Y', 'Y', now() "
                    + "WHERE NOT EXISTS (SELECT 1 FROM system_user WHERE login_name = 'admin')");
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
