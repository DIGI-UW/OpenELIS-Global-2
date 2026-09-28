package org.openelisglobal.microbiology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

public class MicrobiologyM9LiquibaseRollbackTest {

    @Test
    public void m9ChangesetsRollbackAndReapplyOnStandaloneDatabase() throws Exception {
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

                Liquibase fullChangelog = new Liquibase("liquibase/base-changelog.xml", resources, database);
                fullChangelog.update(new Contexts("test"));
                assertM9SchemaPresent(connection);

                Liquibase m9Changelog = new Liquibase("liquibase/microbiology-m9-rollback.xml", resources, database);
                m9Changelog.rollback(3, "test");
                assertM9SchemaAbsent(connection);

                m9Changelog.update(new Contexts("test"));
                assertM9SchemaPresent(connection);
            }
        }
    }

    private void assertM9SchemaPresent(Connection connection) throws Exception {
        assertTrue(columnExists(connection, "micro_organism", "default_ast_panel_id"));
        assertTrue(columnExists(connection, "micro_antibiotic", "route"));
        assertTrue(columnExists(connection, "micro_culture_setup", "last_updated_by"));
        assertTrue(columnExists(connection, "micro_ast_panel", "version_number"));
        assertTrue(columnExists(connection, "micro_ast_panel_antibiotic", "report_behavior"));
        assertTrue(columnExists(connection, "micro_breakpoint_standard", "lifecycle_status"));
        assertTrue(columnExists(connection, "micro_breakpoint_rule", "source_row_hash"));
        assertTrue(tableExists(connection, "micro_breakpoint_activation_event"));
    }

    private void assertM9SchemaAbsent(Connection connection) throws Exception {
        assertFalse(columnExists(connection, "micro_organism", "default_ast_panel_id"));
        assertFalse(columnExists(connection, "micro_antibiotic", "route"));
        assertFalse(columnExists(connection, "micro_culture_setup", "last_updated_by"));
        assertFalse(columnExists(connection, "micro_ast_panel", "version_number"));
        assertFalse(columnExists(connection, "micro_ast_panel_antibiotic", "report_behavior"));
        assertFalse(columnExists(connection, "micro_breakpoint_standard", "lifecycle_status"));
        assertFalse(columnExists(connection, "micro_breakpoint_rule", "source_row_hash"));
        assertFalse(tableExists(connection, "micro_breakpoint_activation_event"));
    }

    private boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(null, "clinlims", tableName, new String[] { "TABLE" })) {
            return tables.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, "clinlims", tableName, columnName)) {
            return columns.next();
        }
    }
}
