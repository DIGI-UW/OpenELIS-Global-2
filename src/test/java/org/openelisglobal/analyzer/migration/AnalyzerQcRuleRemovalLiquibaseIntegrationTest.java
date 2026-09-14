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

public class AnalyzerQcRuleRemovalLiquibaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String TEST_SCHEMA = "ogc1054_qc_removal";

    @Autowired
    private DataSource dataSource;

    @Test
    public void upgradeDropsSupersededAnalyzerQcRuleTableAndRollbackRestoresItsSchema() throws Exception {
        try {
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + TEST_SCHEMA);
                statement.execute("CREATE TABLE " + TEST_SCHEMA + ".analyzer (id NUMERIC(10,0) PRIMARY KEY)");
                statement.execute("CREATE TABLE " + TEST_SCHEMA + ".analyzer_qc_rule (id VARCHAR(36) PRIMARY KEY)");
            }

            try (Connection connection = dataSource.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("public");
                database.setLiquibaseSchemaName(TEST_SCHEMA);
                String priorSchema = System.getProperty("analyzer.schema");
                System.setProperty("analyzer.schema", TEST_SCHEMA);
                try {
                    try (Liquibase liquibase = new Liquibase("liquibase/3.5.x.x/088-remove-analyzer-qc-rule.xml",
                            new ClassLoaderResourceAccessor(), database)) {
                        liquibase.update(new Contexts("test"));
                        assertEquals(0, tableCount(TEST_SCHEMA, "analyzer_qc_rule"));
                        liquibase.rollback(1, "test");
                    }
                } finally {
                    if (priorSchema == null) {
                        System.clearProperty("analyzer.schema");
                    } else {
                        System.setProperty("analyzer.schema", priorSchema);
                    }
                }
            }

            assertEquals(1, tableCount(TEST_SCHEMA, "analyzer_qc_rule"));
        } finally {
            try (Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
            }
        }
    }

    private int tableCount(String schema, String table) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
