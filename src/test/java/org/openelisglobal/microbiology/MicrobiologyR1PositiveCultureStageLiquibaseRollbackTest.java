package org.openelisglobal.microbiology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

public class MicrobiologyR1PositiveCultureStageLiquibaseRollbackTest {

    @Test
    public void positiveCultureConstraintRollsBackAndReapplies() throws Exception {
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
                assertTrue(stageConstraint(connection).contains("POSITIVE_SIGNAL"));
                assertTrue(stageConstraint(connection).contains("LOST_SPECIMEN"));

                Liquibase stageChangelog = new Liquibase(
                        "liquibase/microbiology-r1-positive-culture-stage-rollback.xml", resources, database);
                stageChangelog.rollback(1, "test");
                assertFalse(stageConstraint(connection).contains("POSITIVE_SIGNAL"));
                assertFalse(stageConstraint(connection).contains("LOST_SPECIMEN"));

                stageChangelog.update(new Contexts("test"));
                assertTrue(stageConstraint(connection).contains("POSITIVE_SIGNAL"));
                assertTrue(stageConstraint(connection).contains("LOST_SPECIMEN"));
            }
        }
    }

    private String stageConstraint(Connection connection) throws Exception {
        String sql = "select pg_get_constraintdef(oid) from pg_constraint where conname = 'micro_case_stage_chk'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
