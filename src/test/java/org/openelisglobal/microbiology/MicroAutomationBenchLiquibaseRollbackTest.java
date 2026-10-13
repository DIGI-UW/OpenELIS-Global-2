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

public class MicroAutomationBenchLiquibaseRollbackTest {
    @Test
    public void upgradesRollsBackAndRefusesToDiscardSavedSheets() throws Exception {
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
                var context = new Contexts("test");
                var pending = app.listUnrunChangeSets(context, new LabelExpression());
                int preceding = 0;
                while (preceding < pending.size()
                        && !pending.get(preceding).getId().equals("amr-v2-m7-automation-bench-labels"))
                    preceding++;
                assertTrue("M7 registered", preceding < pending.size());
                app.update(preceding, context, new LabelExpression());
                var migration = new Liquibase("liquibase/3.5.x.x/124-amr-v2-automation-bench-labels.xml", resources,
                        database);
                migration.update(context);
                assertEquals("3",
                        scalar(connection, "SELECT count(*) FROM clinlims.label_preset WHERE prints_per_container"));
                migration.rollback(4, "test");
                assertEquals("0", scalar(connection,
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema='clinlims' AND table_name='micro_incoming_item'"));
                migration.update(context);
                try (var statement = connection.createStatement()) {
                    statement.execute(
                            "INSERT INTO clinlims.workplan_print_record(id,layout,lab_unit_id,printed_by,printed_at,snapshot) VALUES('owned-sheet','MICROBIOLOGY_BENCH','1','1',now(),'immutable-sheet')");
                }
                connection.commit();
                try {
                    migration.rollback(4, "test");
                    fail("Saved sheet must prevent destructive rollback");
                } catch (liquibase.exception.LiquibaseException expected) {
                    assertTrue(expected.getMessage().contains("history"));
                }
                connection.rollback();
                assertEquals("immutable-sheet", scalar(connection,
                        "SELECT snapshot FROM clinlims.workplan_print_record WHERE id='owned-sheet'"));
            }
        }
    }

    private String scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
}
