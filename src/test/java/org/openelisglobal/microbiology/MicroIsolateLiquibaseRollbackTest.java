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
 * Real upgrade and rollback against retained isolate identification history.
 */
public class MicroIsolateLiquibaseRollbackTest {
    @Test
    public void preservesHistoricIsolatesAndIdentificationWhileReversingM5() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims").withUsername("clinlims").withPassword(UUID.randomUUID().toString());
            postgres.start();
            try (Connection connection = postgres.createConnection("")) {
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName("clinlims");
                var resources = new ClassLoaderResourceAccessor();
                var app = new Liquibase("liquibase/base-changelog.xml", resources, database);
                var contexts = new Contexts("test");
                var pending = app.listUnrunChangeSets(contexts, new LabelExpression());
                int preceding = 0;
                while (preceding < pending.size()
                        && !pending.get(preceding).getId().equals("amr-v2-m5-isolates-referral"))
                    preceding++;
                assertTrue("M5 must be registered", preceding < pending.size());
                app.update(preceding, contexts, new LabelExpression());
                execute(connection,
                        "INSERT INTO clinlims.sample (id,accession_number,entered_date,received_date) VALUES (887801,'M5-UPGRADE',DATE '2026-01-01',TIMESTAMP '2026-01-02 09:00:00')");
                execute(connection,
                        "INSERT INTO clinlims.sample_item (id,sort_order,samp_id,status_id) SELECT 887802,1,887801,MIN(id) FROM clinlims.status_of_sample");
                execute(connection,
                        "INSERT INTO clinlims.micro_case (id,sample_item_id,workflow_type) VALUES ('stored-case',887802,'BACTERIOLOGY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_isolate (id,case_id,isolate_label,significance,identification_status) VALUES ('stored-isolate','stored-case','ISO-1','UNKNOWN','PRELIMINARY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_isolate_identification_event (id,isolate_id,event_type,new_organism_text,reason,changed_at,changed_by) VALUES ('stored-event','stored-isolate','IDENTIFIED','Stored identification','Original observation',TIMESTAMP '2026-01-03 09:00:00','1')");
                if (!connection.getAutoCommit())
                    connection.commit();
                String isolateBefore = scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_isolate i WHERE id='stored-isolate'");
                String eventBefore = scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_isolate_identification_event i WHERE id='stored-event'");
                app.update(contexts);
                assertTrue(column(connection, "micro_isolate", "source_culture_id"));
                assertTrue(column(connection, "micro_isolate_identification_event", "event_sequence"));
                assertTrue(column(connection, "type_of_sample", "microbiology_isolate"));
                assertEquals("1", scalar(connection,
                        "SELECT count(*) FROM pg_indexes WHERE schemaname='clinlims' AND indexname='idx_micro_isolate_source'"));
                assertEquals("5", scalar(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname IN ('fk_micro_isolate_source_culture_id','fk_micro_isolate_source_sample_item_id','fk_micro_isolate_sample_item_id','fk_micro_isolate_received_from_organization_id','fk_micro_isolate_reported_organism_id')"));
                assertEquals("0", scalar(connection,
                        "SELECT event_sequence FROM clinlims.micro_isolate_identification_event WHERE id='stored-event'"));
                execute(connection,
                        "UPDATE clinlims.micro_isolate SET significance='COLONIZER' WHERE id='stored-isolate'");
                if (!connection.getAutoCommit())
                    connection.commit();
                var m5 = new Liquibase("liquibase/3.5.x.x/122-amr-v2-isolates-referral.xml", resources, database);
                assertThrows(liquibase.exception.LiquibaseException.class, () -> m5.rollback(1, "test"));
                database.rollback();
                assertEquals("COLONIZER", scalar(connection,
                        "SELECT significance FROM clinlims.micro_isolate WHERE id='stored-isolate'"));
                assertTrue(column(connection, "micro_isolate", "source_culture_id"));
                execute(connection,
                        "UPDATE clinlims.micro_isolate SET significance='UNKNOWN' WHERE id='stored-isolate'");
                if (!connection.getAutoCommit())
                    connection.commit();
                m5.rollback(1, "test");
                assertFalse(column(connection, "micro_isolate", "source_culture_id"));
                assertFalse(column(connection, "micro_isolate_identification_event", "event_sequence"));
                assertFalse(column(connection, "type_of_sample", "microbiology_isolate"));
                assertEquals("0", scalar(connection,
                        "SELECT count(*) FROM pg_indexes WHERE schemaname='clinlims' AND indexname='idx_micro_isolate_source'"));
                assertEquals("0", scalar(connection,
                        "SELECT count(*) FROM pg_constraint WHERE conname IN ('fk_micro_isolate_source_culture_id','fk_micro_isolate_source_sample_item_id','fk_micro_isolate_sample_item_id','fk_micro_isolate_received_from_organization_id','fk_micro_isolate_reported_organism_id')"));
                assertEquals(isolateBefore, scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_isolate i WHERE id='stored-isolate'"));
                assertEquals(eventBefore, scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_isolate_identification_event i WHERE id='stored-event'"));
                m5.update(contexts);
                assertTrue(column(connection, "micro_isolate", "sample_item_id"));
                assertTrue(column(connection, "micro_isolate_identification_event", "previous_identified_at"));
            }
        }
    }

    private void execute(Connection c, String sql) throws Exception {
        try (var s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private String scalar(Connection c, String sql) throws Exception {
        try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
            assertTrue(r.next());
            return r.getString(1);
        }
    }

    private boolean column(Connection c, String table, String name) throws Exception {
        try (var r = c.getMetaData().getColumns(null, "clinlims", table, name)) {
            return r.next();
        }
    }
}
