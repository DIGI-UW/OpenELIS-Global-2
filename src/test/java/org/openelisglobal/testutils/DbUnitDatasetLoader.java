package org.openelisglobal.testutils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;

/**
 * Standalone DBUnit Dataset Loader for Manual and E2E Testing.
 * 
 * <p>
 * Loads DBUnit Flat XML datasets into the database without requiring the full
 * Spring test context. Can be run via Maven exec plugin.
 * </p>
 * 
 * <h3>Usage:</h3>
 * 
 * <pre>
 * # Load a specific dataset
 * mvn exec:java -Dexec.mainClass="org.openelisglobal.testutils.DbUnitDatasetLoader" \
 *     -Dexec.classpathScope=test \
 *     -Dexec.args="testdata/analyzer-mapping-test-data.xml"
 * 
 * # With custom database connection
 * mvn exec:java -Dexec.mainClass="org.openelisglobal.testutils.DbUnitDatasetLoader" \
 *     -Dexec.classpathScope=test \
 *     -Dexec.args="testdata/analyzer-mapping-test-data.xml" \
 *     -Ddb.url="jdbc:postgresql://localhost:15432/clinlims" \
 *     -Ddb.user="clinlims" \
 *     -Ddb.password="clinlims"
 * </pre>
 * 
 * <h3>Environment Variables (alternative to system properties):</h3>
 * <ul>
 * <li>DB_URL - JDBC URL (default:
 * jdbc:postgresql://localhost:15432/clinlims)</li>
 * <li>DB_USER - Database user (default: clinlims)</li>
 * <li>DB_PASSWORD - Database password (default: clinlims)</li>
 * </ul>
 * 
 * @see org.openelisglobal.BaseWebContextSensitiveTest#executeDataSetWithStateManagement
 */
public class DbUnitDatasetLoader {

    // Default connection parameters (Docker dev environment)
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:15432/clinlims";
    private static final String DEFAULT_USER = "clinlims";
    private static final String DEFAULT_PASSWORD = "clinlims";
    private static final String DEFAULT_SCHEMA = "clinlims";

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String datasetPath = args[0];
        String operation = args.length > 1 ? args[1].toUpperCase() : "CLEAN_INSERT";

        // Get connection parameters from system properties or environment variables
        String url = getConfig("db.url", "DB_URL", DEFAULT_URL);
        String user = getConfig("db.user", "DB_USER", DEFAULT_USER);
        String password = getConfig("db.password", "DB_PASSWORD", DEFAULT_PASSWORD);
        String schema = getConfig("db.schema", "DB_SCHEMA", DEFAULT_SCHEMA);

        System.out.println("======================================");
        System.out.println("  DBUnit Dataset Loader");
        System.out.println("======================================");
        System.out.println("Dataset: " + datasetPath);
        System.out.println("Operation: " + operation);
        System.out.println("Database: " + url);
        System.out.println("Schema: " + schema);
        System.out.println();

        try {
            loadDataset(datasetPath, url, user, password, schema, operation);
            System.out.println("✅ Dataset loaded successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to load dataset: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load a DBUnit dataset into the database.
     *
     * @param datasetPath   Path to the dataset file (relative to classpath or
     *                      absolute)
     * @param url           JDBC URL
     * @param user          Database user
     * @param password      Database password
     * @param schema        Database schema
     * @param operationType DBUnit operation type (CLEAN_INSERT, INSERT, REFRESH,
     *                      etc.)
     */
    public static void loadDataset(String datasetPath, String url, String user, String password, String schema,
            String operationType) throws Exception {

        // Load PostgreSQL driver
        Class.forName("org.postgresql.Driver");

        // Establish connection
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        try (Connection jdbcConnection = DriverManager.getConnection(url, props)) {
            // Set schema
            jdbcConnection.createStatement().execute("SET search_path TO " + schema);

            // Create DBUnit connection
            IDatabaseConnection dbConnection = new DatabaseConnection(jdbcConnection, schema);

            // Configure for PostgreSQL
            DatabaseConfig config = dbConnection.getConfig();
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
            config.setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, false);
            // Preserve lowercase column names (PostgreSQL) so REFRESH matches XML
            // attributes (e.g. fhir_uuid).
            config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);

            // Load dataset
            IDataSet dataSet = loadDataSetFromPath(datasetPath);

            // Get operation
            DatabaseOperation dbOperation = getOperation(operationType);

            // Execute
            System.out.println("Loading " + dataSet.getTableNames().length + " tables...");
            for (String tableName : dataSet.getTableNames()) {
                int rowCount = dataSet.getTable(tableName).getRowCount();
                System.out.println("  - " + tableName + ": " + rowCount + " rows");
            }

            dbOperation.execute(dbConnection, dataSet);
        }
    }

    /**
     * Load dataset from path (tries classpath first, then filesystem).
     */
    private static IDataSet loadDataSetFromPath(String path) throws Exception {
        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        builder.setColumnSensing(true); // Handle missing columns gracefully
        builder.setCaseSensitiveTableNames(true); // Preserve XML column case (e.g. fhir_uuid) for REFRESH

        // Try classpath first
        InputStream is = DbUnitDatasetLoader.class.getClassLoader().getResourceAsStream(path);
        if (is != null) {
            System.out.println("Loading from classpath: " + path);
            return builder.build(is);
        }

        // Try filesystem
        System.out.println("Loading from filesystem: " + path);
        return builder.build(new FileInputStream(path));
    }

    /**
     * Get DBUnit operation by name.
     */
    private static DatabaseOperation getOperation(String operationType) {
        switch (operationType) {
        case "CLEAN_INSERT":
            return DatabaseOperation.CLEAN_INSERT;
        case "INSERT":
            return DatabaseOperation.INSERT;
        case "REFRESH":
            return DatabaseOperation.REFRESH;
        case "UPDATE":
            return DatabaseOperation.UPDATE;
        case "DELETE":
            return DatabaseOperation.DELETE;
        case "DELETE_ALL":
            return DatabaseOperation.DELETE_ALL;
        case "TRUNCATE":
            return DatabaseOperation.TRUNCATE_TABLE;
        default:
            System.out.println("Unknown operation '" + operationType + "', using CLEAN_INSERT");
            return DatabaseOperation.CLEAN_INSERT;
        }
    }

    /**
     * Get configuration value from system property or environment variable.
     */
    private static String getConfig(String sysProp, String envVar, String defaultValue) {
        String value = System.getProperty(sysProp);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    private static void printUsage() {
        System.out.println("Usage: DbUnitDatasetLoader <dataset-path> [operation]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  dataset-path  Path to DBUnit XML dataset (classpath or filesystem)");
        System.out.println("  operation     DBUnit operation (default: CLEAN_INSERT)");
        System.out.println("                Options: CLEAN_INSERT, INSERT, REFRESH, UPDATE, DELETE");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Load analyzer test data");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"org.openelisglobal.testutils.DbUnitDatasetLoader\" \\");
        System.out.println("      -Dexec.classpathScope=test \\");
        System.out.println("      -Dexec.args=\"testdata/analyzer-mapping-test-data.xml\"");
        System.out.println();
        System.out.println("  # Use INSERT (don't delete existing data)");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"org.openelisglobal.testutils.DbUnitDatasetLoader\" \\");
        System.out.println("      -Dexec.classpathScope=test \\");
        System.out.println("      -Dexec.args=\"testdata/analyzer-mapping-test-data.xml INSERT\"");
        System.out.println();
        System.out.println("Environment/System Properties:");
        System.out.println("  -Ddb.url=<jdbc-url>       Database URL (default: localhost:15432)");
        System.out.println("  -Ddb.user=<user>          Database user (default: clinlims)");
        System.out.println("  -Ddb.password=<password>  Database password (default: clinlims)");
        System.out.println("  -Ddb.schema=<schema>      Database schema (default: clinlims)");
    }
}
