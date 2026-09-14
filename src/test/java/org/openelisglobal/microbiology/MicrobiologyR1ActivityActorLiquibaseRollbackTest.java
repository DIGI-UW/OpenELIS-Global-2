package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
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

public class MicrobiologyR1ActivityActorLiquibaseRollbackTest {

    @Test
    public void activityActorReferenceRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertEquals("numeric", performedByType(connection));
                assertEquals(1, actorForeignKeyCount(connection));

                Liquibase actorChangelog = new Liquibase("liquibase/microbiology-r1-activity-actor-rollback.xml",
                        resources, database);
                actorChangelog.rollback(1, "test");
                assertEquals("character varying", performedByType(connection));
                assertEquals(0, actorForeignKeyCount(connection));

                actorChangelog.update(new Contexts("test"));
                assertEquals("numeric", performedByType(connection));
                assertEquals(1, actorForeignKeyCount(connection));
            }
        }
    }

    private String performedByType(Connection connection) throws Exception {
        String sql = "SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = 'clinlims' AND table_name = 'micro_case_activity' "
                + "AND column_name = 'performed_by'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }

    private int actorForeignKeyCount(Connection connection) throws Exception {
        String sql = "SELECT count(*) FROM information_schema.table_constraints "
                + "WHERE constraint_schema = 'clinlims' AND table_name = 'micro_case_activity' "
                + "AND constraint_name = 'fk_micro_case_activity_performed_by'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
