package org.openelisglobal.microbiology;

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

public class MicrobiologyR1CultureTimingLiquibaseRollbackTest {

    @Test
    public void cultureTimingChangesetRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertTimingColumns(connection, true);

                Liquibase timingChangelog = new Liquibase("liquibase/microbiology-r1-culture-timing-rollback.xml",
                        resources, database);
                timingChangelog.rollback(1, "test");
                assertTimingColumns(connection, false);

                timingChangelog.update(new Contexts("test"));
                assertTimingColumns(connection, true);
            }
        }
    }

    private void assertTimingColumns(Connection connection, boolean expected) throws Exception {
        assertTrue(columnExists(connection, "micro_culture_setup", "incubation_hours") == expected);
        assertTrue(columnExists(connection, "micro_culture_setup", "subculture_at_hours") == expected);
        assertTrue(columnExists(connection, "micro_culture_setup", "max_incubation_days") == expected);
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(null, "clinlims", tableName, columnName)) {
            return columns.next();
        }
    }
}
