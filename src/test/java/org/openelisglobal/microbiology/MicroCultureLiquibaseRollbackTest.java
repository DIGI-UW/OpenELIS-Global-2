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

/** Real upgrade and rollback against retained inoculation history. */
public class MicroCultureLiquibaseRollbackTest {
    @Test
    public void upgradesStoredInoculationsAndReversesOnlyM4Structure() throws Exception {
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
                        && !pending.get(preceding).getId().equals("amr-v2-m4-culture-workspace"))
                    preceding++;
                assertTrue("M4 must be registered", preceding < pending.size());
                app.update(preceding, contexts, new LabelExpression());
                execute(connection,
                        "INSERT INTO clinlims.dictionary_category (id,name,description,local_abbrev,lastupdated) VALUES (887810,'Culture atmosphere','Existing lab atmosphere','LABATM',now())");
                execute(connection,
                        "INSERT INTO clinlims.dictionary (id,dict_entry,dictionary_category_id,is_active,lastupdated) VALUES (887811,'Aerobic',887810,'Y',now()), (887812,'Lab-specific atmosphere',887810,'Y',now())");
                execute(connection,
                        "INSERT INTO clinlims.sample (id,accession_number,entered_date,received_date) VALUES (887801,'M4-UPGRADE',DATE '2026-01-01',TIMESTAMP '2026-01-02 09:00:00')");
                execute(connection,
                        "INSERT INTO clinlims.sample_item (id,sort_order,samp_id,status_id) SELECT 887802,1,887801,MIN(id) FROM clinlims.status_of_sample");
                execute(connection,
                        "INSERT INTO clinlims.micro_case (id,sample_item_id,workflow_type) VALUES ('stored-case',887802,'BACTERIOLOGY')");
                execute(connection,
                        "INSERT INTO clinlims.micro_case_activity (id,case_id,activity_type) VALUES ('stored-activity','stored-case','CULTURE_INOCULATED')");
                execute(connection,
                        "INSERT INTO clinlims.micro_case_inoculation (id,case_id,activity_id,container_identifier,media,performed_by) VALUES ('stored-row','stored-case','stored-activity','HISTORIC-PLATE','Historic agar','1')");
                if (!connection.getAutoCommit())
                    connection.commit();
                String before = scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_case_inoculation i WHERE id='stored-row'");
                String vocabularyBefore = scalar(connection,
                        "SELECT jsonb_agg(jsonb_build_array(id,dict_entry,dictionary_category_id,is_active,lastupdated) ORDER BY id)::text FROM clinlims.dictionary d WHERE id IN (887811,887812)");
                app.update(new Liquibase("liquibase/3.5.x.x/121-amr-v2-culture-workspace.xml", resources, database)
                        .getDatabaseChangeLog().getChangeSets().size(), contexts, new LabelExpression());
                assertTrue(column(connection, "micro_case_inoculation", "duration"));
                assertTrue(column(connection, "test_section", "gram_stain_test_id"));
                String columns = "ARRAY['source_sample_item_id','subculture_purpose','medium_item_id','lot_id','not_tracked','temperature','duration','duration_unit','check_interval_hours','loop_volume','positive_at','positive_source','outcome','outcome_by','outcome_at']";
                assertEquals(before, scalar(connection, "SELECT (to_jsonb(i) - " + columns
                        + ")::text FROM clinlims.micro_case_inoculation i WHERE id='stored-row'"));
                var m4 = new Liquibase("liquibase/3.5.x.x/121-amr-v2-culture-workspace.xml", resources, database);
                m4.rollback(m4.getDatabaseChangeLog().getChangeSets().size(), "test");
                assertFalse(column(connection, "micro_case_inoculation", "duration"));
                assertFalse(column(connection, "test_section", "gram_stain_test_id"));
                assertTrue(column(connection, "inventory_item", "track_lots"));
                assertEquals(before, scalar(connection,
                        "SELECT to_jsonb(i)::text FROM clinlims.micro_case_inoculation i WHERE id='stored-row'"));
                assertEquals(vocabularyBefore, scalar(connection,
                        "SELECT jsonb_agg(jsonb_build_array(id,dict_entry,dictionary_category_id,is_active,lastupdated) ORDER BY id)::text FROM clinlims.dictionary d WHERE id IN (887811,887812)"));
                assertEquals("Existing lab atmosphere",
                        scalar(connection, "SELECT description FROM clinlims.dictionary_category WHERE id=887810"));
                m4.update(contexts);
                assertTrue(column(connection, "micro_culture_reading", "reading_id"));
                assertTrue(column(connection, "micro_case_inoculation", "duration"));
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
