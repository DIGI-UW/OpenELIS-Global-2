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

public class MicrobiologyM10LiquibaseRollbackTest {

    @Test
    public void m10ChangesetRollsBackAndReappliesOnStandaloneDatabase() throws Exception {
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
                assertTrue(tableExists(connection, "micro_whonet_export_run"));
                assertFalse(columnExists(connection, "micro_whonet_export_run", "lastupdated"));
                assertFalse(indexExists(connection, "micro_whonet_export_run", "idx_micro_whonet_export_generated_at"));

                Liquibase m10Changelog = new Liquibase("liquibase/microbiology-m10-rollback.xml", resources, database);
                m10Changelog.rollback(2, "test");
                assertFalse(tableExists(connection, "micro_whonet_export_run"));

                m10Changelog.update(new Contexts("test"));
                assertTrue(tableExists(connection, "micro_whonet_export_run"));
                assertFalse(columnExists(connection, "micro_whonet_export_run", "lastupdated"));
                assertFalse(indexExists(connection, "micro_whonet_export_run", "idx_micro_whonet_export_generated_at"));
            }
        }
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

    private boolean indexExists(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet indexes = metadata.getIndexInfo(null, "clinlims", tableName, false, false)) {
            while (indexes.next()) {
                if (indexName.equals(indexes.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
