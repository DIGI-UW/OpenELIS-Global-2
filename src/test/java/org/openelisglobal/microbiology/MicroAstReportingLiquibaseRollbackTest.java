package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.UUID;
import liquibase.*;
import liquibase.database.*;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Upgrade preserves old selections; rollback refuses to erase clinical history.
 */
public class MicroAstReportingLiquibaseRollbackTest {
    @Test
    public void upgradeBackfillsSelectedReadingsAndRollbackPreservesHistory() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims").withUsername("clinlims").withPassword(UUID.randomUUID().toString());
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                var database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                var resources = new ClassLoaderResourceAccessor();
                var app = new Liquibase("liquibase/base-changelog.xml", resources, database);
                var contexts = new Contexts("test");
                var pending = app.listUnrunChangeSets(contexts, new LabelExpression());
                int preceding = 0;
                while (preceding < pending.size() && !pending.get(preceding).getId().equals("amr-v2-m6-ast-reporting"))
                    preceding++;
                assertTrue("M6 registered", preceding < pending.size());
                app.update(preceding, contexts, new LabelExpression());
                var m6 = new Liquibase("liquibase/3.5.x.x/123-amr-v2-ast-reporting.xml", resources, database);
                // Empty installations can safely reverse and reapply the complete structural
                // change.
                m6.update(contexts);
                assertTrue(column(connection, "micro_ast_reading", "entry_sequence"));
                m6.rollback(1, "test");
                assertFalse(column(connection, "micro_ast_reading", "entry_sequence"));
                execute(connection,
                        "INSERT INTO clinlims.sample (id,accession_number,entered_date,received_date) VALUES (887901,'M6-UPGRADE',DATE '2026-01-01',TIMESTAMP '2026-01-02 09:00:00')");
                execute(connection,
                        "INSERT INTO clinlims.sample_item (id,sort_order,samp_id,status_id) SELECT 887902,1,887901,MIN(id) FROM clinlims.status_of_sample");
                execute(connection,
                        "INSERT INTO clinlims.micro_case (id,sample_item_id,workflow_type) VALUES ('stored-case',887902,'BACTERIOLOGY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_isolate (id,case_id,isolate_label,significance,identification_status) VALUES ('stored-isolate','stored-case','ISO-1','UNKNOWN','PRELIMINARY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_antibiotic (id,display_name,whonet_code) VALUES ('stored-drug','Stored drug','M6D')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_run (id,isolate_id,status,reportable,reviewed_at,reviewed_by,method,technique) VALUES ('stored-run','stored-isolate','REVIEWED',true,TIMESTAMP '2026-01-04 10:00:00','1','MIC','BROTH_MICRODILUTION')");
                execute(connection,
                        "INSERT INTO clinlims.micro_ast_reading (id,ast_run_id,antibiotic_id,method,raw_value,interpretation,created_at,created_by) VALUES ('old-reading','stored-run','stored-drug','MIC',4,'SUSCEPTIBLE',TIMESTAMP '2026-01-03 09:00:00','1'),('new-reading','stored-run','stored-drug','MIC',64,'RESISTANT',TIMESTAMP '2026-01-03 10:00:00','1')");
                if (!connection.getAutoCommit())
                    connection.commit();
                m6.update(contexts);
                assertEquals("new-reading", scalar(connection,
                        "SELECT reading_id FROM clinlims.micro_ast_report_choice WHERE isolate_id='stored-isolate' AND antibiotic_id='stored-drug'"));
                assertEquals("1,2", scalar(connection,
                        "SELECT string_agg(entry_sequence::text,',' ORDER BY created_at) FROM clinlims.micro_ast_reading WHERE ast_run_id='stored-run'"));
                assertEquals("0", scalar(connection,
                        "SELECT count(*) FROM pg_indexes WHERE indexname='uq_micro_ast_run_reportable'"));
                assertThrows(liquibase.exception.LiquibaseException.class, () -> m6.rollback(1, "test"));
                database.rollback();
                assertTrue(column(connection, "micro_ast_reading", "entry_sequence"));
                assertEquals("4.0000,64.0000", scalar(connection,
                        "SELECT string_agg(raw_value::text,',' ORDER BY created_at) FROM clinlims.micro_ast_reading WHERE ast_run_id='stored-run'"));
                assertEquals("new-reading", scalar(connection,
                        "SELECT reading_id FROM clinlims.micro_ast_report_choice WHERE isolate_id='stored-isolate'"));
            }
        }
    }

    private void execute(Connection c, String sql) throws Exception {
        try (var statement = c.createStatement()) {
            statement.execute(sql);
        }
    }

    private String scalar(Connection c, String sql) throws Exception {
        try (var statement = c.createStatement(); var row = statement.executeQuery(sql)) {
            assertTrue(row.next());
            return row.getString(1);
        }
    }

    private boolean column(Connection c, String table, String name) throws Exception {
        try (var columns = c.getMetaData().getColumns(null, "clinlims", table, name)) {
            return columns.next();
        }
    }
}
