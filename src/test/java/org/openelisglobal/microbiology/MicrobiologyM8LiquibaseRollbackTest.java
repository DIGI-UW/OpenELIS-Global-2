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

public class MicrobiologyM8LiquibaseRollbackTest {

    @Test
    public void m8ChangesetsRollbackAndReapplyOnStandaloneDatabase() throws Exception {
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
                assertM8SchemaPresent(connection);

                Liquibase m8Changelog = new Liquibase("liquibase/microbiology-m8-rollback.xml", resources, database);
                m8Changelog.rollback(2, "test");
                assertM8SchemaAbsent(connection);

                m8Changelog.update(new Contexts("test"));
                assertM8SchemaPresent(connection);
            }
        }
    }

    private void assertM8SchemaPresent(Connection connection) throws Exception {
        assertTrue(tableExists(connection, "micro_case_amendment"));
        assertTrue(tableExists(connection, "micro_report_version"));
        assertTrue(tableExists(connection, "micro_isolate_identification_event"));
        assertTrue(columnExists(connection, "micro_ast_run", "amendment_id"));
        assertTrue(columnExists(connection, "micro_ast_run", "attempt_type"));
        assertTrue(columnExists(connection, "micro_ast_run", "reportable"));
    }

    private void assertM8SchemaAbsent(Connection connection) throws Exception {
        assertFalse(tableExists(connection, "micro_case_amendment"));
        assertFalse(tableExists(connection, "micro_report_version"));
        assertFalse(tableExists(connection, "micro_isolate_identification_event"));
        assertFalse(columnExists(connection, "micro_ast_run", "amendment_id"));
        assertFalse(columnExists(connection, "micro_ast_run", "attempt_type"));
        assertFalse(columnExists(connection, "micro_ast_run", "reportable"));
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
