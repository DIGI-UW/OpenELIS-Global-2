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

public class MicrobiologyR1PatientOriginLiquibaseRollbackTest {

    @Test
    public void patientOriginReferenceRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertEquals(6, rowCount(connection, "micro_patient_origin"));
                assertEquals(6, distinctGeneratedIdCount(connection));
                assertEquals(1, tableCount(connection, "micro_patient_origin_default"));

                Liquibase originChangelog = new Liquibase(
                        "liquibase/microbiology-r1-patient-origin-reference-rollback.xml", resources, database);
                originChangelog.rollback(1, "test");
                assertEquals(0, tableCount(connection, "micro_patient_origin"));
                assertEquals(0, tableCount(connection, "micro_patient_origin_default"));

                originChangelog.update(new Contexts("test"));
                assertEquals(6, rowCount(connection, "micro_patient_origin"));
                assertEquals(6, distinctGeneratedIdCount(connection));
            }
        }
    }

    private int rowCount(Connection connection, String tableName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT count(*) FROM clinlims." + tableName);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private int tableCount(Connection connection, String tableName) throws Exception {
        String sql = "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema = 'clinlims' AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private int distinctGeneratedIdCount(Connection connection) throws Exception {
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT count(DISTINCT id) FROM clinlims.micro_patient_origin "
                        + "WHERE id IS NOT NULL AND length(id) = 36");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
