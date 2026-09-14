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

public class MicrobiologyR1ProgramCodeLengthLiquibaseRollbackTest {

    @Test
    public void rollbackPreservesLongProgramCodesAndCompatibleColumnWidth() throws Exception {
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
                assertEquals(20, programCodeLength(connection));
                insertMicrobiologyProgram(connection);

                Liquibase codeLengthChangelog = new Liquibase(
                        "liquibase/microbiology-r1-program-code-length-rollback.xml", resources, database);
                codeLengthChangelog.rollback(1, "test");
                assertEquals(20, programCodeLength(connection));
                assertEquals(1, microbiologyProgramCount(connection));

                codeLengthChangelog.update(new Contexts("test"));
                assertEquals(20, programCodeLength(connection));
                assertEquals(1, microbiologyProgramCount(connection));
            }
        }
    }

    private int programCodeLength(Connection connection) throws Exception {
        String sql = "SELECT character_maximum_length FROM information_schema.columns "
                + "WHERE table_schema = 'clinlims' AND table_name = 'program' AND column_name = 'code'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private void insertMicrobiologyProgram(Connection connection) throws Exception {
        String sql = "INSERT INTO clinlims.program (id, code, name, lastupdated) "
                + "VALUES (nextval('clinlims.program_seq'), 'MICROBIOLOGY', 'Microbiology', CURRENT_TIMESTAMP)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
        connection.commit();
    }

    private int microbiologyProgramCount(Connection connection) throws Exception {
        String sql = "SELECT COUNT(*) FROM clinlims.program WHERE code = 'MICROBIOLOGY'";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
