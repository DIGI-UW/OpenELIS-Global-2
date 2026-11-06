package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
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

public class MicroPanelReportCleanupLiquibaseRollbackTest {

    @Test
    public void panelReportCleanupRollsBackWithItsDefaultsAndChecksAndReapplies() throws Exception {
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
                new Liquibase("liquibase/base-changelog.xml", resources, database).update(new Contexts("test"));
                assertFalse(column(connection, "micro_ast_panel_antibiotic", "tier"));
                assertFalse(column(connection, "micro_ast_panel_antibiotic", "report_behavior"));
                assertFalse(column(connection, "micro_ast_run_antibiotic", "tier"));
                execute(connection,
                        "INSERT INTO clinlims.micro_antibiotic (id,display_name,whonet_code) VALUES ('drug-155','Drug 155','D155')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_panel (id,name,logical_key) VALUES ('panel-155','Panel 155','panel-155')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_panel_antibiotic (id,panel_id,antibiotic_id) VALUES ('row-155','panel-155','drug-155')");
                if (!connection.getAutoCommit())
                    connection.commit();

                Liquibase cleanup = new Liquibase("liquibase/3.5.x.x/155-amr-v3-panel-report-cleanup.xml", resources,
                        database);
                cleanup.rollback(1, "test");
                assertTrue(column(connection, "micro_ast_panel_antibiotic", "tier"));
                assertTrue(column(connection, "micro_ast_panel_antibiotic", "report_behavior"));
                assertTrue(column(connection, "micro_ast_run_antibiotic", "tier"));
                assertEquals("1|ALWAYS", scalar(connection,
                        "SELECT tier || '|' || report_behavior FROM clinlims.micro_ast_panel_antibiotic WHERE id='row-155'"));
                assertEquals("2", scalar(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname IN ('micro_ast_panel_antibiotic_tier_chk','micro_ast_panel_antibiotic_report_chk')"));
                assertThrows(java.sql.SQLException.class, () -> execute(connection,
                        "UPDATE clinlims.micro_ast_panel_antibiotic SET tier = 4 WHERE id='row-155'"));
                if (!connection.getAutoCommit())
                    connection.rollback();

                cleanup.update(new Contexts("test"));
                assertFalse(column(connection, "micro_ast_panel_antibiotic", "tier"));
                assertFalse(column(connection, "micro_ast_panel_antibiotic", "report_behavior"));
                assertFalse(column(connection, "micro_ast_run_antibiotic", "tier"));
            }
        }
    }

    private static boolean column(Connection connection, String table, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, "clinlims", table, column)) {
            return columns.next();
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
