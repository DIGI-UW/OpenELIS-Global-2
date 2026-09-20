package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;

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
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerSupersededRuntimeRemovalLiquibaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_SCHEMA = "ogc1054_runtime_removal";

    @Autowired
    private DataSource dataSource;

    @Test
    public void upgradeRemovesPreviouslyAppliedFullStateAndTransportSchema() throws Exception {
        try {
            createSupersededSchema();

            try (Connection connection = dataSource.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(TEST_SCHEMA);
                database.setLiquibaseSchemaName(TEST_SCHEMA);
                try (Liquibase liquibase = new Liquibase("liquibase/3.5.x.x/093-remove-superseded-analyzer-runtime.xml",
                        new ClassLoaderResourceAccessor(), database)) {
                    liquibase.update(new Contexts("test"));
                }
            }

            assertEquals(0, tableCount("analyzer_activation_candidate"));
            assertEquals(0, sequenceCount("analyzer_activation_candidate_seq"));
            assertEquals(0, columnCount("analyzer", "active_candidate_id"));
            assertEquals(0, columnCount("analyzer", "transport_mode"));
            assertEquals(0, columnCount("analyzer", "connection_role"));
            assertEquals(0, referenceTableCount("analyzer_activation_candidate"));
        } finally {
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
            }
        }
    }

    private void createSupersededSchema() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + TEST_SCHEMA);
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".reference_tables (name VARCHAR(255) NOT NULL)");
            statement.execute("CREATE TABLE " + TEST_SCHEMA + ".analyzer ("
                    + "id NUMERIC(10,0) PRIMARY KEY, transport_mode VARCHAR(16), "
                    + "connection_role VARCHAR(16), active_candidate_id NUMERIC(10,0))");
            statement.execute("CREATE SEQUENCE " + TEST_SCHEMA + ".analyzer_activation_candidate_seq");
            statement.execute(
                    "CREATE TABLE " + TEST_SCHEMA + ".analyzer_activation_candidate (id NUMERIC(10,0) PRIMARY KEY)");
            statement.execute("ALTER TABLE " + TEST_SCHEMA
                    + ".analyzer ADD CONSTRAINT fk_analyzer_active_candidate FOREIGN KEY (active_candidate_id) "
                    + "REFERENCES " + TEST_SCHEMA + ".analyzer_activation_candidate(id)");
            statement.execute(
                    "CREATE INDEX idx_analyzer_active_candidate ON " + TEST_SCHEMA + ".analyzer(active_candidate_id)");
            statement.execute("ALTER TABLE " + TEST_SCHEMA
                    + ".analyzer ADD CONSTRAINT chk_analyzer_transport_mode CHECK (transport_mode IS NULL OR "
                    + "transport_mode IN ('TCP', 'MLLP', 'SERIAL', 'FILE', 'HTTP'))");
            statement.execute("ALTER TABLE " + TEST_SCHEMA
                    + ".analyzer ADD CONSTRAINT chk_analyzer_connection_role CHECK (connection_role IS NULL OR "
                    + "connection_role IN ('RECEIVER', 'INITIATOR'))");
            statement.execute(
                    "INSERT INTO " + TEST_SCHEMA + ".reference_tables(name) VALUES ('analyzer_activation_candidate')");
        }
    }

    private int tableCount(String table) throws Exception {
        return count("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?", table);
    }

    private int sequenceCount(String sequence) throws Exception {
        return count("SELECT COUNT(*) FROM information_schema.sequences WHERE sequence_schema = ? "
                + "AND sequence_name = ?", sequence);
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

    private int referenceTableCount(String name) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + TEST_SCHEMA + ".reference_tables WHERE LOWER(name) = LOWER(?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
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
