package org.openelisglobal.testutils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone DBUnit fixture loader for E2E/manual testing.
 *
 * <p>
 * Loads DBUnit XML datasets into a running Postgres database (Docker or direct
 * connection). This enables Cypress/manual testing to use the same fixture
 * source-of-truth as JUnit tests.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * java -cp ... org.openelisglobal.testutils.DbUnitFixtureLoader \
 *   --jdbc-url jdbc:postgresql://localhost:5432/clinlims \
 *   --user clinlims \
 *   --password clinlims \
 *   testdata/storage-e2e.xml testdata/user-role.xml
 * </pre>
 *
 * <p>
 * Or with Docker defaults (assumes openelisglobal-database container):
 *
 * <pre>
 * java -cp ... org.openelisglobal.testutils.DbUnitFixtureLoader \
 *   --docker \
 *   testdata/storage-e2e.xml
 * </pre>
 */
public class DbUnitFixtureLoader {

    private static final Logger logger = LoggerFactory.getLogger(DbUnitFixtureLoader.class);

    // Docker defaults - port can be overridden via DB_PORT environment variable
    // All docker-compose files (dev/test/build/main) consistently use 15432:5432
    // mapping
    // This matches the port mapping across all environments (develop, feature
    // branches, etc.)
    // For different environments, set DB_PORT or use --jdbc-url explicitly
    private static final String DEFAULT_DOCKER_HOST = "localhost";
    private static final String DEFAULT_DOCKER_DB = "clinlims";
    private static final String DEFAULT_DOCKER_USER = "clinlims";
    private static final String DEFAULT_DOCKER_PASSWORD = "clinlims";
    // Default port matches docker-compose port mapping (15432:5432) used across all
    // environments
    private static final int DEFAULT_DOCKER_PORT = 15432;

    private static int getDockerPort() {
        // Check environment variable first (allows override for different environments)
        String portEnv = System.getenv("DB_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try {
                int port = Integer.parseInt(portEnv);
                logger.info("Using DB_PORT from environment: {}", port);
                return port;
            } catch (NumberFormatException e) {
                logger.warn("Invalid DB_PORT environment variable: {}, using default {}", portEnv, DEFAULT_DOCKER_PORT);
            }
        }
        // Default matches docker-compose port mapping used in dev/test/build/main
        // environments
        return DEFAULT_DOCKER_PORT;
    }

    private static String getDefaultDockerJdbcUrl() {
        int port = getDockerPort();
        return String.format("jdbc:postgresql://%s:%d/%s", DEFAULT_DOCKER_HOST, port, DEFAULT_DOCKER_DB);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String jdbcUrl = null;
        String user = null;
        String password = null;
        boolean useDocker = false;
        int datasetStartIdx = 0;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
            case "--jdbc-url":
                if (i + 1 < args.length) {
                    jdbcUrl = args[++i];
                } else {
                    System.err.println("ERROR: --jdbc-url requires a value");
                    System.exit(1);
                }
                break;
            case "--user":
                if (i + 1 < args.length) {
                    user = args[++i];
                } else {
                    System.err.println("ERROR: --user requires a value");
                    System.exit(1);
                }
                break;
            case "--password":
                if (i + 1 < args.length) {
                    password = args[++i];
                } else {
                    System.err.println("ERROR: --password requires a value");
                    System.exit(1);
                }
                break;
            case "--docker":
                useDocker = true;
                datasetStartIdx = i + 1;
                break;
            case "--help":
            case "-h":
                printUsage();
                System.exit(0);
                break;
            default:
                if (args[i].startsWith("--")) {
                    System.err.println("ERROR: Unknown option: " + args[i]);
                    System.exit(1);
                }
                // First non-option argument is start of dataset list
                if (datasetStartIdx == 0) {
                    datasetStartIdx = i;
                }
                break;
            }
        }

        // Apply Docker defaults if requested
        if (useDocker) {
            if (jdbcUrl == null) {
                jdbcUrl = getDefaultDockerJdbcUrl();
            }
            if (user == null) {
                user = DEFAULT_DOCKER_USER;
            }
            if (password == null) {
                password = DEFAULT_DOCKER_PASSWORD;
            }
        }

        // Validate required parameters
        if (jdbcUrl == null || user == null || password == null) {
            System.err.println("ERROR: Must provide --jdbc-url, --user, --password OR use --docker");
            printUsage();
            System.exit(1);
        }

        // Get dataset files
        if (datasetStartIdx >= args.length) {
            System.err.println("ERROR: No dataset files specified");
            printUsage();
            System.exit(1);
        }

        List<String> datasetFiles = Arrays.asList(args).subList(datasetStartIdx, args.length);

        // Load fixtures
        try {
            loadFixtures(jdbcUrl, user, password, datasetFiles);
            logger.info("✅ Successfully loaded {} dataset(s)", datasetFiles.size());
            System.exit(0);
        } catch (Exception e) {
            logger.error("❌ Failed to load fixtures", e);
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println(
                "  java -cp ... org.openelisglobal.testutils.DbUnitFixtureLoader [OPTIONS] <dataset1.xml> [dataset2.xml ...]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --docker              Use Docker defaults (localhost:15432/clinlims, user=clinlims)");
        System.out.println("                        Port can be overridden via DB_PORT environment variable");
        System.out.println("  --jdbc-url URL        JDBC connection URL");
        System.out.println("  --user USER           Database user");
        System.out.println("  --password PASSWORD   Database password");
        System.out.println("  --help, -h            Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Docker (defaults):");
        System.out.println("  java -cp ... DbUnitFixtureLoader --docker testdata/storage-e2e.xml");
        System.out.println();
        System.out.println("  # Direct connection:");
        System.out.println("  java -cp ... DbUnitFixtureLoader \\");
        System.out.println("    --jdbc-url jdbc:postgresql://localhost:5432/clinlims \\");
        System.out.println("    --user clinlims --password clinlims \\");
        System.out.println("    testdata/storage-e2e.xml testdata/user-role.xml");
    }

    private static void loadFixtures(String jdbcUrl, String user, String password, List<String> datasetFiles)
            throws Exception {
        Connection connection = null;
        try {
            // Connect to database
            logger.info("Connecting to database: {}", jdbcUrl);
            connection = DriverManager.getConnection(jdbcUrl, user, password);

            // Create DBUnit connection
            IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
            DatabaseConfig config = dbUnitConnection.getConfig();
            config.setProperty(DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, true);
            config.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
            config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());

            // Load each dataset
            for (String datasetFile : datasetFiles) {
                logger.info("Loading dataset: {}", datasetFile);
                loadDataset(dbUnitConnection, datasetFile);
            }

            // Bump sequences to avoid PK collisions (same logic as BaseStorageTest)
            logger.info("Bumping storage sequences to avoid PK collisions");
            bumpStorageSequences(connection);

        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static void loadDataset(IDatabaseConnection dbUnitConnection, String datasetFileName) throws Exception {
        InputStream inputStream = null;
        try {
            // Load from classpath (same as BaseWebContextSensitiveTest)
            inputStream = DbUnitFixtureLoader.class.getClassLoader().getResourceAsStream(datasetFileName);
            if (inputStream == null) {
                throw new IllegalArgumentException("Dataset file '" + datasetFileName + "' not found in classpath");
            }

            IDataSet dataset = new FlatXmlDataSet(inputStream);
            String[] tableNames = dataset.getTableNames();

            // IMPORTANT: Do NOT truncate tables here.
            //
            // This loader is used for Cypress/manual testing against a long-lived dev DB.
            // Truncating tables based on dataset contents is dangerous because datasets may
            // include shared reference tables (e.g., localization) and would wipe unrelated
            // application data, potentially preventing the webapp from starting.
            //
            // Instead, we rely on:
            // - DatabaseOperation.REFRESH to upsert rows by primary key
            // - reset-test-database.sh (optional) to clean test-owned rows when needed
            logger.info("Refreshing dataset into database (no truncation)");
            DatabaseOperation.REFRESH.execute(dbUnitConnection, dataset);

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static void cleanRowsInConnection(IDatabaseConnection dbUnitConnection, String[] tableNames)
            throws SQLException {
        // Get connection from dbUnitConnection but don't close it (dbUnitConnection
        // manages lifecycle)
        Connection conn = dbUnitConnection.getConnection();
        try (Statement stmt = conn.createStatement()) {
            for (String tableName : tableNames) {
                String truncateQuery = "TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE";
                logger.debug("Truncating table: {}", tableName);
                stmt.execute(truncateQuery);
            }
        }
        // Note: Don't close conn here - dbUnitConnection manages it
    }

    private static void bumpStorageSequences(Connection connection) throws SQLException {
        // Same sequence bumps as BaseStorageTest.setUp()
        String[] sequences = { "storage_room_seq", "storage_device_seq", "storage_shelf_seq", "storage_rack_seq",
                "storage_box_seq" };
        int[] values = { 1000, 1000, 1000, 1000, 10000 };

        try (Statement stmt = connection.createStatement()) {
            for (int i = 0; i < sequences.length; i++) {
                String sql = String.format("SELECT setval('%s', %d, false)", sequences[i], values[i]);
                logger.debug("Bumping sequence: {} to {}", sequences[i], values[i]);
                stmt.execute(sql);
            }
        }
    }
}
