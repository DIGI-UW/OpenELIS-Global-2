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

    @Test
    public void panelReportBehaviorsBecomeInactiveReportingRulesThatRollBack() throws Exception {
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
                Liquibase cleanup = new Liquibase("liquibase/3.5.x.x/155-amr-v3-panel-report-cleanup.xml", resources,
                        database);

                execute(connection,
                        "DELETE FROM clinlims.databasechangelog WHERE id='155-amr-v3-panel-report-carryover'");
                commit(connection);
                cleanup.update(new Contexts("test"));
                assertEquals("MARK_RAN", scalar(connection,
                        "SELECT exectype FROM clinlims.databasechangelog WHERE id='155-amr-v3-panel-report-carryover'"));

                cleanup.rollback(2, "test");
                execute(connection,
                        "INSERT INTO clinlims.micro_antibiotic (id,display_name,whonet_code) VALUES "
                                + "('drug-cascade','Cascade drug','DC'),('drug-suppress','Suppress drug','DS'),"
                                + "('drug-always','Always drug','DA'),('drug-old','Old drug','DO'),"
                                + "('drug-scoped','Scoped drug','DSC')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_panel (id,name,logical_key,version_number,is_current) "
                                + "VALUES ('panel-a-old','Panel A','family-a',1,'N'),('panel-a','Panel A','family-a',2,'Y'),"
                                + "('panel-b','Panel B','family-b',1,'Y'),('panel-c','Panel C','family-c',1,'Y')");
                execute(connection,
                        "INSERT INTO clinlims.micro_organism (id,display_name,default_ast_panel_id) VALUES ('organism-c','Organism C','panel-c')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_panel_antibiotic (id,panel_id,antibiotic_id,tier,report_behavior) VALUES "
                                + "('a-cascade','panel-a','drug-cascade',2,'CASCADE'),"
                                + "('a-suppress','panel-a','drug-suppress',1,'SUPPRESS_UNLESS_RESISTANT'),"
                                + "('a-always','panel-a','drug-always',1,'ALWAYS'),"
                                + "('a-old','panel-a-old','drug-old',1,'CASCADE'),"
                                + "('b-suppress','panel-b','drug-suppress',1,'SUPPRESS_UNLESS_RESISTANT'),"
                                + "('c-scoped','panel-c','drug-scoped',1,'SUPPRESS_UNLESS_RESISTANT')");
                commit(connection);

                cleanup.update(new Contexts("test"));
                assertFalse(column(connection, "micro_ast_panel_antibiotic", "report_behavior"));
                assertEquals("drug-cascade|CASCADE|;drug-scoped|SUPPRESS_UNLESS_RESISTANT|organism-c;"
                        + "drug-suppress|SUPPRESS_UNLESS_RESISTANT|", scalar(connection, CARRIED_RULES));
                assertEquals("0", scalar(connection,
                        "SELECT count(*) FROM clinlims.micro_reporting_rule WHERE active OR note IS NOT NULL"));
                assertEquals(
                        "Carried over on upgrade from AST panel \"Panel A\" (tier 2). Panel tiers are not cascade"
                                + " conditions: set the condition, then review before activating.",
                        origin(connection, "drug-cascade"));
                assertEquals(
                        "Carried over on upgrade from AST panels \"Panel A\", \"Panel B\". Review before activating.",
                        origin(connection, "drug-suppress"));
                assertEquals("false|System Daemon", scalar(connection,
                        "SELECT h.after_value::json->>'active' || '|' || u.first_name || ' ' || u.last_name"
                                + " FROM clinlims.micro_reporting_rule_history h JOIN clinlims.micro_reporting_rule r"
                                + " ON r.id = h.rule_id JOIN clinlims.system_user u ON u.id::text = h.changed_by"
                                + " WHERE r.antibiotic_id = 'drug-scoped' AND u.login_name = 'daemon'"));

                cleanup.rollback(2, "test");
                assertEquals(null, scalar(connection, CARRIED_RULES));
                assertEquals("0", scalar(connection, "SELECT count(*) FROM clinlims.micro_reporting_rule_history"));
            }
        }
    }

    private static final String CARRIED_RULES = "SELECT string_agg(antibiotic_id || '|' || report_behavior || '|'"
            + " || coalesce(organism_id, ''), ';' ORDER BY antibiotic_id) FROM clinlims.micro_reporting_rule"
            + " WHERE antibiotic_id LIKE 'drug-%'";

    private static String origin(Connection connection, String antibioticId) throws Exception {
        return scalar(connection, "SELECT h.after_value::json->>'origin' FROM clinlims.micro_reporting_rule_history h"
                + " JOIN clinlims.micro_reporting_rule r ON r.id = h.rule_id WHERE h.revision = 1 AND r.antibiotic_id = '"
                + antibioticId + "'");
    }

    private static void commit(Connection connection) throws Exception {
        if (!connection.getAutoCommit())
            connection.commit();
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
