package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerConnectionRuntimeRemovalLiquibaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_SCHEMA = "ogc1054_connection_runtime_removal";

    @Autowired
    private DataSource dataSource;

    @Test
    public void migratedAnalyzerLosesBridgeOwnedRuntimeAndRetainsOpenElisState() throws Exception {
        try {
            createReleasedSchema(true);

            runRemovalMigration();

            assertEquals(0, columnCount("analyzer", "ip_address"));
            assertEquals(0, columnCount("analyzer", "port"));
            assertEquals(0, columnCount("analyzer", "protocol_version"));
            assertEquals(0, columnCount("analyzer", "communication_mode"));
            assertEquals(0, columnCount("analyzer", "import_directory"));
            assertEquals(0, columnCount("analyzer", "file_pattern"));
            assertEquals(0, columnCount("analyzer", "column_mappings_json"));
            assertEquals(0, columnCount("analyzer", "file_format"));
            assertEquals(0, columnCount("analyzer", "delimiter"));
            assertEquals(0, columnCount("analyzer", "has_header"));
            assertEquals(0, columnCount("analyzer", "skip_rows"));
            assertEquals(0, tableCount("serial_port_configuration"));
            assertEquals(0, tableCount("file_import_configuration"));

            assertEquals(1, columnCount("analyzer", "test_unit_ids"));
            assertEquals(1, columnCount("analyzer", "status"));
            assertEquals(1, columnCount("analyzer", "bridge_connection_id"));
            assertEquals("7,8", analyzerValue("test_unit_ids"));
            assertEquals("SETUP", analyzerValue("status"));
            assertEquals("bridge-connection-101", analyzerValue("bridge_connection_id"));
        } finally {
            dropTestSchema();
        }
    }

    @Test
    public void unmigratedAnalyzerStopsRuntimeRemovalWithoutDataLoss() throws Exception {
        try {
            createReleasedSchema(false);

            try {
                runRemovalMigration();
                fail("Runtime removal must stop until every retained analyzer has a Bridge connection");
            } catch (LiquibaseException expected) {
                assertTrue(exceptionMessages(expected).contains("precondition"));
                assertEquals(1, columnCount("analyzer", "ip_address"));
                assertEquals(1, tableCount("serial_port_configuration"));
                assertEquals("192.0.2.10", analyzerValue("ip_address"));
            }
        } finally {
            dropTestSchema();
        }
    }

    @Test
    public void rollbackRestoresTheReleasedRuntimeSchema() throws Exception {
        try {
            createReleasedSchema(true);

            runAndRollbackRemovalMigration();

            assertEquals(1, columnCount("analyzer", "ip_address"));
            assertEquals(1, columnCount("analyzer", "communication_mode"));
            assertEquals(1, columnCount("analyzer", "import_directory"));
            assertEquals(1, tableCount("serial_port_configuration"));
            assertEquals(1, tableCount("file_import_configuration"));
        } finally {
            dropTestSchema();
        }
    }

    private void createReleasedSchema(boolean migrated) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + TEST_SCHEMA);
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".analyzer ("
                    + "id NUMERIC(10,0) PRIMARY KEY, ip_address VARCHAR(15), port INTEGER, "
                    + "protocol_version VARCHAR(20), communication_mode VARCHAR(25), "
                    + "import_directory VARCHAR(500), file_pattern VARCHAR(100), column_mappings_json TEXT, "
                    + "file_format VARCHAR(30), delimiter VARCHAR(10), has_header BOOLEAN, skip_rows INTEGER, "
                    + "test_unit_ids TEXT, " + "status VARCHAR(20), bridge_connection_id VARCHAR(255))");
            statement.execute("INSERT INTO " + TEST_SCHEMA + ".analyzer "
                    + "(id, ip_address, port, protocol_version, communication_mode, import_directory, "
                    + "file_pattern, column_mappings_json, file_format, delimiter, has_header, skip_rows, "
                    + "test_unit_ids, status, bridge_connection_id) "
                    + "VALUES (101, '192.0.2.10', 5000, 'ASTM_LIS2_A2', 'ANALYZER_INITIATED', "
                    + "'/released/incoming', '*.csv', '{\"testCode\":\"code\"}', 'CSV', ',', TRUE, 1, "
                    + "'7,8', 'SETUP', " + (migrated ? "'bridge-connection-101'" : "NULL") + ")");
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".serial_port_configuration ("
                    + "id VARCHAR(36) PRIMARY KEY, analyzer_id NUMERIC(10,0) NOT NULL, port_name VARCHAR(50))");
            statement.execute("INSERT INTO " + TEST_SCHEMA + ".serial_port_configuration (id, analyzer_id, port_name) "
                    + "VALUES ('serial-101', 101, '/dev/ttyUSB0')");
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".file_import_configuration ("
                    + "id VARCHAR(36) PRIMARY KEY, analyzer_id NUMERIC(10,0) NOT NULL, import_directory VARCHAR(255))");
            statement.execute(
                    "INSERT INTO " + TEST_SCHEMA + ".file_import_configuration (id, analyzer_id, import_directory) "
                            + "VALUES ('file-101', 101, '/released/incoming')");
        }
    }

    private void runRemovalMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(TEST_SCHEMA);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(TEST_SCHEMA);
            database.setLiquibaseSchemaName(TEST_SCHEMA);
            try (Liquibase liquibase = new Liquibase(
                    "liquibase/3.5.x.x/094-remove-openelis-analyzer-connection-runtime.xml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts("test"));
            }
        }
    }

    private void runAndRollbackRemovalMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(TEST_SCHEMA);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(TEST_SCHEMA);
            database.setLiquibaseSchemaName(TEST_SCHEMA);
            try (Liquibase liquibase = new Liquibase(
                    "liquibase/3.5.x.x/094-remove-openelis-analyzer-connection-runtime.xml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts("test"));
                liquibase.rollback(1, "test");
            }
        }
    }

    private String exceptionMessages(Throwable exception) {
        StringBuilder messages = new StringBuilder();
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage().toLowerCase()).append('\n');
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    private void dropTestSchema() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
        }
    }

    private int tableCount(String table) throws Exception {
        return count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?", table);
    }

    private int columnCount(String table, String column) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ? "
                + "AND column_name = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TEST_SCHEMA);
            statement.setString(2, table);
            statement.setString(3, column);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private String analyzerValue(String column) throws Exception {
        String sql = "SELECT " + column + " FROM " + TEST_SCHEMA + ".analyzer WHERE id = 101";
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private int count(String sql, String objectName) throws Exception {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TEST_SCHEMA);
            statement.setString(2, objectName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
