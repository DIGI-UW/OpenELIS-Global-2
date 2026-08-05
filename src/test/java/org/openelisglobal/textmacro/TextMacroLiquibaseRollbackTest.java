package org.openelisglobal.textmacro;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class TextMacroLiquibaseRollbackTest {

    @Test
    public void textMacroChangesetRollsBackAndReapplies() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims");
            postgres.withUsername("clinlims");
            postgres.withPassword("clinlims");
            postgres.start();

            try (Connection connection = postgres.createConnection("")) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                ClassLoaderResourceAccessor resources = new ClassLoaderResourceAccessor();
                Liquibase changelog = new Liquibase("liquibase/text-macro-rollback.xml", resources, database);

                changelog.update(new Contexts("test"));
                assertTrue(tableExists(connection, "text_macro"));
                assertTrue(tableExists(connection, "text_macro_context"));
                assertTrue(indexExists(connection, "text_macro", "uq_text_macro_code"));
                assertTrue(sequenceExists(connection, "text_macro_seq"));
                assertTrue(textMacroAuditRegistrationExists(connection));

                changelog.rollback(2, "test");
                assertFalse(tableExists(connection, "text_macro_context"));
                assertFalse(tableExists(connection, "text_macro"));
                assertFalse(sequenceExists(connection, "text_macro_seq"));
                assertFalse(textMacroAuditRegistrationExists(connection));

                changelog.update(new Contexts("test"));
                assertTrue(tableExists(connection, "text_macro"));
                assertTrue(tableExists(connection, "text_macro_context"));
                assertTrue(sequenceExists(connection, "text_macro_seq"));
                assertTrue(textMacroAuditRegistrationExists(connection));
            }
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, "clinlims", tableName, new String[] { "TABLE" })) {
            return tables.next();
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(null, "clinlims", tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equals(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean sequenceExists(Connection connection, String sequenceName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.sequences WHERE sequence_schema = 'clinlims' AND sequence_name = ?")) {
            statement.setString(1, sequenceName);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private boolean textMacroAuditRegistrationExists(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM clinlims.reference_tables WHERE LOWER(name) = 'text_macro' AND keep_history = 'Y'");
                ResultSet rows = statement.executeQuery()) {
            return rows.next();
        }
    }
}
