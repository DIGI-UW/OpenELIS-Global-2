package org.openelisglobal.microbiology;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.UUID;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class MicroEnvironmentalMigrationLiquibaseRollbackTest {
    @Test
    public void freshInstallAndLegacyUpgradeAreAtomicAndKeepIndependentCaseHistory() throws Exception {
        try (var postgres = new PostgreSQLContainer<>("postgres:14.4")) {
            postgres.withCopyFileToContainer(MountableFile.forClasspathResource("postgre-db-init"),
                    "/docker-entrypoint-initdb.d");
            postgres.withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5");
            postgres.withDatabaseName("clinlims").withUsername("clinlims").withPassword(UUID.randomUUID().toString());
            postgres.start();
            try (Connection c = postgres.createConnection("")) {
                var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
                database.setDefaultSchemaName("clinlims");
                var resources = new ClassLoaderResourceAccessor();
                var app = new Liquibase("liquibase/base-changelog.xml", resources, database);
                var contexts = new Contexts("test");
                var pending = app.listUnrunChangeSets(contexts, new LabelExpression());
                int before = 0;
                while (before < pending.size() && !pending.get(before).getId().equals("amr-v2-environmental-migration"))
                    before++;
                assertTrue("M8 is registered after case routing", before < pending.size());
                app.update(before, contexts, new LabelExpression());
                assertEquals("0", scalar(c, "SELECT count(*) FROM clinlims.micro_patient_origin"));
                assertEquals("0", scalar(c, "SELECT count(*) FROM clinlims.label_preset WHERE prints_per_container"));
                assertEquals("0",
                        scalar(c, "SELECT count(*) FROM clinlims.site_information WHERE name='blockSelfValidation'"));
                var migration = new Liquibase("liquibase/3.5.x.x/125-amr-v2-environmental-migration.xml", resources,
                        database);
                String dictionaryCount = scalar(c, "SELECT count(*) FROM clinlims.dictionary");
                String observationTypeCount = scalar(c, "SELECT count(*) FROM clinlims.observation_history_type");
                migration.update(contexts);
                assertEquals(dictionaryCount, scalar(c, "SELECT count(*) FROM clinlims.dictionary"));
                assertEquals(observationTypeCount, scalar(c, "SELECT count(*) FROM clinlims.observation_history_type"));
                assertFalse(column(c, "micro_case", "workflow_type"));
                assertTrue(column(c, "micro_case", "culture_purpose"));
                migration.rollback(1, "test");
                assertTrue(column(c, "micro_case", "workflow_type"));
                assertFalse(column(c, "micro_case", "culture_purpose"));
                execute(c,
                        "INSERT INTO clinlims.test_section(id,name,description,is_active,domain,name_localization_id) VALUES (887981,'M8 unit','M8 unit','Y','CLINICAL',(SELECT min(id) FROM clinlims.localization))");
                execute(c,
                        "INSERT INTO clinlims.program(id,code,name,domain,is_active,manually_changed) VALUES (887981,'BACTERIOLOGY','M8 bacteriology','CLINICAL','Y',true)");
                execute(c,
                        "INSERT INTO clinlims.sample(id,accession_number,entered_date,received_date) VALUES (887981,'M8-LEGACY',DATE '2026-01-01',TIMESTAMP '2026-01-02 09:00:00')");
                execute(c,
                        "INSERT INTO clinlims.type_of_sample(id,description,local_abbrev,is_active,name_localization_id) VALUES (887981,'M8 specimen','M8','Y',(SELECT min(id) FROM clinlims.localization))");
                execute(c,
                        "INSERT INTO clinlims.sample_item(id,sort_order,samp_id,typeosamp_id,status_id) SELECT 887981,1,887981,887981,min(id) FROM clinlims.status_of_sample");
                execute(c,
                        "INSERT INTO clinlims.test(id,name,description,guid,test_section_id,is_active,name_localization_id) VALUES (887981,'M8 test','M8 test','m8-test',887981,'Y',(SELECT min(id) FROM clinlims.localization))");
                execute(c,
                        "INSERT INTO clinlims.analysis(id,sampitem_id,test_id,analysis_type,status_id) SELECT 887981,887981,887981,'MANUAL',min(id) FROM clinlims.status_of_sample");
                execute(c,
                        "INSERT INTO clinlims.micro_case(id,sample_item_id,workflow_type,created_at) VALUES ('legacy-a',887981,'BACTERIOLOGY',TIMESTAMP '2026-01-02 10:00:00'),('legacy-b',887981,'BACTERIOLOGY',TIMESTAMP '2026-01-02 11:00:00')");
                execute(c,
                        "INSERT INTO clinlims.micro_case_analysis(id,case_id,analysis_id) VALUES ('legacy-analysis','legacy-a',887981)");
                execute(c,
                        "INSERT INTO clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,note,structured_data) VALUES ('original','legacy-a','WORKFLOW_CHANGED',TIMESTAMP '2026-01-02 12:00:00','original reason','{\"from\":\"UNASSIGNED\",\"to\":\"BACTERIOLOGY\"}')");
                execute(c,
                        "INSERT INTO clinlims.test_section(id,name,description,is_active,domain,name_localization_id) VALUES (887982,'Wrong legacy unit','Wrong legacy unit','Y','CLINICAL',(SELECT min(id) FROM clinlims.localization))");
                execute(c, "UPDATE clinlims.program SET test_section_id=887982 WHERE id=887981");
                execute(c, "INSERT INTO clinlims.program_lab_unit(program_id,test_section_id) VALUES (887981,887982)");
                execute(c,
                        "INSERT INTO clinlims.program(id,code,name,domain,is_active,manually_changed) VALUES (887982,'MICRO_ONE','Microbiology','CLINICAL','Y',true),(887983,'MICRO_TWO','Microbiology','CLINICAL','Y',true)");
                execute(c,
                        "INSERT INTO clinlims.program_sample(id,program_id,sample_id,questionnaire_response_uuid) VALUES (887981,887983,887981,'12345678-1234-1234-1234-123456789012')");
                c.commit();
                // One unmapped case halts the whole migration, including its schema changes.
                assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.update(contexts));
                database.rollback();
                assertFalse(column(c, "micro_case", "culture_purpose"));
                assertEquals("0", scalar(c, "SELECT count(*) FROM clinlims.micro_case WHERE lab_unit_id IS NOT NULL"));
                assertEquals("1", scalar(c, "SELECT count(*) FROM clinlims.micro_case_activity"));
                execute(c, "UPDATE clinlims.micro_case SET lab_unit_id=887981 WHERE id='legacy-b'");
                c.commit();
                migration.update(contexts);
                assertEquals("2", scalar(c,
                        "SELECT count(*) FROM clinlims.micro_case WHERE lab_unit_id=887981 AND program_id=887981 AND sample_id=887981"));
                assertEquals("2",
                        scalar(c, "SELECT count(*) FROM clinlims.micro_case_sample WHERE sample_item_id=887981"));
                assertEquals("1", scalar(c, "SELECT count(*) FROM clinlims.program WHERE name='Microbiology'"));
                assertEquals("887982", scalar(c, "SELECT program_id FROM clinlims.program_sample WHERE id=887981"));
                assertEquals("12345678-1234-1234-1234-123456789012", scalar(c,
                        "SELECT questionnaire_response_uuid::text FROM clinlims.program_sample WHERE id=887981"));
                assertEquals("887981", scalar(c, "SELECT test_section_id FROM clinlims.program WHERE id=887981"));
                assertEquals("887981",
                        scalar(c, "SELECT test_section_id FROM clinlims.program_lab_unit WHERE program_id=887981"));
                assertEquals("887981",
                        scalar(c, "SELECT analysis_id FROM clinlims.micro_case_analysis WHERE id='legacy-analysis'"));
                assertEquals("2", scalar(c,
                        "SELECT count(*) FROM clinlims.micro_case_activity WHERE activity_type='MIGRATION_REVIEW' AND structured_data LIKE '%SHARED_GROUPING%'"));
                assertEquals("2026-01-02 12:00:00", scalar(c,
                        "SELECT occurred_at::text FROM clinlims.micro_case_activity WHERE activity_type='LEGACY_HISTORY_PRESERVED' AND structured_data LIKE '%original%'"));
                assertEquals("original reason",
                        scalar(c, "SELECT note FROM clinlims.micro_case_activity WHERE id='original'"));
                assertThrows(liquibase.exception.LiquibaseException.class, () -> migration.rollback(1, "test"));
                database.rollback();
                assertTrue(column(c, "micro_case", "culture_purpose"));
                assertEquals("2", scalar(c, "SELECT count(*) FROM clinlims.micro_case"));
            }
        }
    }

    private static void execute(Connection c, String sql) throws Exception {
        try (var s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static String scalar(Connection c, String sql) throws Exception {
        try (var s = c.createStatement(); var rows = s.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }

    private static boolean column(Connection c, String table, String column) throws Exception {
        try (var rows = c.getMetaData().getColumns(null, "clinlims", table, column)) {
            return rows.next();
        }
    }
}
