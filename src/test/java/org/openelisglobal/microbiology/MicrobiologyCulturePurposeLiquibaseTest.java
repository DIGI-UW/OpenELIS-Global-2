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

public class MicrobiologyCulturePurposeLiquibaseTest {

    @Test
    public void culturePurposeColumnRollsBackAndReapplies() throws Exception {
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
                Liquibase changelog = new Liquibase("liquibase/microbiology-r11-culture-purpose-rollback.xml",
                        new ClassLoaderResourceAccessor(), database);

                changelog.update(new Contexts("test"));
                assertTrue(columnExists(connection, "micro_case_order_detail", "culture_purpose"));

                changelog.rollback(1, "test");
                assertFalse(columnExists(connection, "micro_case_order_detail", "culture_purpose"));

                changelog.update(new Contexts("test"));
                assertTrue(columnExists(connection, "micro_case_order_detail", "culture_purpose"));
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
