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

public class MicrobiologyR1OrderDetailDraftLiquibaseRollbackTest {

    @Test
    public void orderDetailDraftChangesetRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertEquals(1, columnCount(connection, "sample_id"));
                assertEquals(1, columnCount(connection, "culture_method_id"));

                Liquibase draftChangelog = new Liquibase("liquibase/microbiology-r1-order-detail-draft-rollback.xml",
                        resources, database);
                draftChangelog.rollback(1, "test");
                assertEquals(0, columnCount(connection, "sample_id"));
                assertEquals(0, columnCount(connection, "culture_method_id"));

                draftChangelog.update(new Contexts("test"));
                assertEquals(1, columnCount(connection, "sample_id"));
                assertEquals(1, columnCount(connection, "culture_method_id"));
            }
        }
    }

    private int columnCount(Connection connection, String columnName) throws Exception {
        String sql = "SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = 'clinlims' AND table_name = 'micro_case_order_detail' AND column_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
