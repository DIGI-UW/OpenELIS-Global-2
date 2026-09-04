package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
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

public class AnalyzerSupersededSchemaRemovalLiquibaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_SCHEMA = "ogc1054_superseded_analyzer_removal";
    private static final List<String> SUPERSEDED_TABLES = List.of("analyzer_run", "analyzer_file_upload",
            "analyzer_experiment", "analyzer_pending_code", "analyzer_plugin_config", "qualitative_result_mapping",
            "unit_mapping", "analyzer_field_mapping", "analyzer_field", "validation_rule_configuration",
            "custom_field_type", "analyzer_test_map", "analyzer_type");

    @Autowired
    private DataSource dataSource;

    @Test
    public void cutoverRetainsConfiguredAndExcludedAnalyzersWithoutSupersededSchema() throws Exception {
        try {
            createReleasedSchema(true);

            runRemovalMigration();

            for (String table : SUPERSEDED_TABLES) {
                assertEquals(table, 0, tableCount(table));
            }
            for (String column : List.of("scrip_id", "machine_id", "analyzer_type", "description", "location",
                    "has_setup_page", "analyzer_type_id", "identifier_pattern")) {
                assertEquals(column, 0, columnCount("analyzer", column));
            }
            assertEquals(2, analyzerCount());
            assertEquals("bridge-101", analyzerValue("bridge_connection_id", 101));
            assertEquals("101", analyzerValue("site_binding_revision_id", 101));
            assertEquals("INACTIVE", analyzerValue("status", 102));
        } finally {
            dropTestSchema();
        }
    }

    @Test
    public void cutoverStopsWhenAnOperationalAnalyzerHasNoBridgeOrCatalogBinding() throws Exception {
        try {
            createReleasedSchema(false);

            try {
                runRemovalMigration();
                fail("Cutover must stop until operational analyzers have Bridge and catalog references");
            } catch (LiquibaseException expected) {
                assertTrue(exceptionMessages(expected).contains("precondition"));
                assertEquals(1, tableCount("analyzer_type"));
                assertEquals(1, columnCount("analyzer", "machine_id"));
            }
        } finally {
            dropTestSchema();
        }
    }

    private void createReleasedSchema(boolean configured) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + TEST_SCHEMA);
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".analyzer ("
                    + "id NUMERIC(10,0) PRIMARY KEY, name VARCHAR(255), is_active BOOLEAN, status VARCHAR(20), "
                    + "bridge_connection_id VARCHAR(255), site_binding_revision_id NUMERIC(10,0), "
                    + "test_unit_ids TEXT, fhir_uuid UUID, last_activated_date TIMESTAMP, "
                    + "latest_activation_record_id NUMERIC(10,0), scrip_id VARCHAR(255), machine_id VARCHAR(255), "
                    + "analyzer_type VARCHAR(30), description VARCHAR(255), location VARCHAR(255), "
                    + "has_setup_page BOOLEAN, analyzer_type_id NUMERIC(10,0), identifier_pattern VARCHAR(500))");
            statement.execute("INSERT INTO " + TEST_SCHEMA
                    + ".analyzer (id, name, is_active, status, bridge_connection_id, site_binding_revision_id, "
                    + "test_unit_ids, machine_id, analyzer_type) VALUES (101, 'Configured analyzer', TRUE, 'ACTIVE', "
                    + (configured ? "'bridge-101', 101" : "NULL, NULL") + ", '7,8', 'OLD-101', 'MOLECULAR')");
            statement.execute("INSERT INTO " + TEST_SCHEMA
                    + ".analyzer (id, name, is_active, status, bridge_connection_id, site_binding_revision_id, "
                    + "test_unit_ids, machine_id, analyzer_type) VALUES "
                    + "(102, 'Intentionally excluded analyzer', FALSE, 'INACTIVE', NULL, NULL, '', 'OLD-102', 'FILE')");
            for (String table : SUPERSEDED_TABLES) {
                statement.execute("CREATE TABLE " + TEST_SCHEMA + "." + table + " (id INTEGER)");
            }
            statement.execute("CREATE SEQUENCE " + TEST_SCHEMA + ".analyzer_type_seq");
            statement.execute("CREATE SEQUENCE " + TEST_SCHEMA + ".analyzer_experiment_seq");
        }
    }

    private void runRemovalMigration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(TEST_SCHEMA);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(TEST_SCHEMA);
            database.setLiquibaseSchemaName(TEST_SCHEMA);
            try (Liquibase liquibase = new Liquibase("liquibase/3.5.x.x/098-remove-superseded-analyzer-schema.xml",
                    new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts("test"));
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

    private int analyzerCount() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + TEST_SCHEMA + ".analyzer")) {
            result.next();
            return result.getInt(1);
        }
    }

    private String analyzerValue(String column, int id) throws Exception {
        String sql = "SELECT " + column + " FROM " + TEST_SCHEMA + ".analyzer WHERE id = " + id;
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
