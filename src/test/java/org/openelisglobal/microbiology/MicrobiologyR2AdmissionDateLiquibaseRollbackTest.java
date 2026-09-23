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

public class MicrobiologyR2AdmissionDateLiquibaseRollbackTest {

    @Test
    public void admissionDateColumnRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertEquals(1, admissionDateColumnCount(connection));
                assertEquals("date", admissionDateType(connection));
                assertEquals("YES", admissionDateNullable(connection));

                Liquibase admissionDateChangelog = new Liquibase(
                        "liquibase/microbiology-r2-admission-date-rollback.xml", resources, database);
                admissionDateChangelog.rollback(1, "test");
                assertEquals(0, admissionDateColumnCount(connection));

                admissionDateChangelog.update(new Contexts("test"));
                assertEquals(1, admissionDateColumnCount(connection));
                assertEquals("date", admissionDateType(connection));
            }
        }
    }

    private int admissionDateColumnCount(Connection connection) throws Exception {
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'clinlims' AND table_name = 'micro_case_order_detail' "
                        + "AND column_name = 'admission_date'");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private String admissionDateType(Connection connection) throws Exception {
        return admissionDateColumnProperty(connection, "data_type");
    }

    private String admissionDateNullable(Connection connection) throws Exception {
        return admissionDateColumnProperty(connection, "is_nullable");
    }

    private String admissionDateColumnProperty(Connection connection, String property) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT " + property + " FROM information_schema.columns WHERE table_schema = 'clinlims' "
                        + "AND table_name = 'micro_case_order_detail' AND column_name = 'admission_date'");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getString(1);
        }
    }
}
