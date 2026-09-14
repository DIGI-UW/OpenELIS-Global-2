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

public class MicrobiologyR1AstProvenanceLiquibaseRollbackTest {

    @Test
    public void astProvenanceChangesetRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertTrue(columnExists(connection, "micro_ast_run", "panel_version"));
                assertTrue(columnExists(connection, "micro_ast_run", "breakpoint_version"));

                Liquibase provenanceChangelog = new Liquibase("liquibase/microbiology-r1-ast-provenance-rollback.xml",
                        resources, database);
                provenanceChangelog.rollback(1, "test");
                assertFalse(columnExists(connection, "micro_ast_run", "panel_version"));
                assertFalse(columnExists(connection, "micro_ast_run", "breakpoint_version"));

                provenanceChangelog.update(new Contexts("test"));
                assertTrue(columnExists(connection, "micro_ast_run", "panel_version"));
                assertTrue(columnExists(connection, "micro_ast_run", "breakpoint_version"));
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, "clinlims", tableName, columnName)) {
            return columns.next();
        }
    }
}
