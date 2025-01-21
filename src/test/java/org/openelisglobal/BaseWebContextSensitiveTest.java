package org.openelisglobal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import javax.sql.DataSource;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
public abstract class BaseWebContextSensitiveTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    private DataSource dataSource;

    protected MockMvc mockMvc;

    private Map<String, IDataSet> originalStateCache;

    private List<String[]> tablesToRestore;

    protected BaseWebContextSensitiveTest() {
        this.originalStateCache = new HashMap<>();
        this.tablesToRestore = new ArrayList<>();
    }

    protected BaseWebContextSensitiveTest(List<String[]> tablesToRestore) {
        this.originalStateCache = new HashMap<>();
        this.tablesToRestore = tablesToRestore != null ? tablesToRestore : new ArrayList<>();
    }

    protected void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
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
     * Executes a dataset with state management - preserves and restores the
     * original state of affected tables after execution.
     */
    protected void executeDataSetWithStateManagement(String datasetFilename) throws Exception {
        if (datasetFilename == null) {
            throw new NullPointerException("Please provide test dataset file to execute!");
        }

        IDatabaseConnection connection = null;
        try {
            connection = new DatabaseConnection(dataSource.getConnection());
            DatabaseConfig config = connection.getConfig();
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);

            IDataSet newDataSet = loadDataSet(datasetFilename);
            String[] tableNames = newDataSet.getTableNames();

            // Backup current state of affected tables
            IDataSet currentState = connection.createDataSet(tableNames);
            originalStateCache.put(Arrays.toString(tableNames), currentState);
            tablesToRestore.add(tableNames);

            executeDataSet(datasetFilename);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * This method will be called after each transaction to restore the database
     * state
     */
    @AfterTransaction
    @SuppressWarnings("unused")
    protected void restoreDatabase() throws Exception {
        try {
            for (String[] tableNames : tablesToRestore) {
                String key = Arrays.toString(tableNames);
                IDataSet originalState = originalStateCache.get(key);
                if (originalState != null) {
                    IDatabaseConnection connection = null;
                    try {
                        connection = new DatabaseConnection(dataSource.getConnection());
                        DatabaseConfig config = connection.getConfig();
                        config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);

                        DatabaseOperation.CLEAN_INSERT.execute(connection, originalState);
                    } finally {
                        if (connection != null) {
                            connection.close();
                        }
                    }
                    originalStateCache.remove(key);
                }
            }
        } finally {
            originalStateCache.clear();
            tablesToRestore.clear();
        }
    }

    /**
     * Loads a dataset from an XML file.
     */
    private IDataSet loadDataSet(String datasetFilename) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(datasetFilename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFilename + "' not found in classpath");
            }
            return new FlatXmlDataSet(inputStream);
        }
    }

    /**
     * Executes a dataset from an XML file.
     */
    protected void executeDataSet(String datasetFilename) throws Exception {
        if (datasetFilename == null) {
            throw new NullPointerException("please provide test dataset file to execute!");
        }

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(datasetFilename);
        try (inputStream) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFilename + "' not found in classpath");
            }
            IDatabaseConnection connection = new DatabaseConnection(dataSource.getConnection());

            DatabaseConfig config = connection.getConfig();
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
            IDataSet dataset = new FlatXmlDataSet(inputStream);

            try {
                DatabaseOperation.REFRESH.execute(connection, dataset);
            } finally {
                connection.close();
            }
        }
    }
}
