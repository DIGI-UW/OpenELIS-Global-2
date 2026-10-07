package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Exercises the candidate cutover without registering it in the application.
 */
public class AmrCutoverMigrationTest {

    private static final String CUTOVER = "liquibase/3.6.x.x/20261006-OGC-1426-amr-v2-cutover.xml";
    private static final String FIXTURE = "liquibase/amr-cutover-existing-data.xml";
    private static final String MAP = "liquibase/amr-cutover-case-map.csv";
    private static final Contexts CONTEXTS = new Contexts("default");
    private PostgreSQLContainer<?> postgres;
    private Connection connection;
    private Database database;
    private URLClassLoader fixtureLoader;

    @Before
    public void freshApplicationDatabase() throws Exception {
        Path fixtureDirectory = Path.of("target", "amr-cutover-fixtures").toAbsolutePath();
        Files.createDirectories(fixtureDirectory);
        fixtureLoader = new URLClassLoader(new URL[] { fixtureDirectory.toUri().toURL() }, getClass().getClassLoader());
        postgres = new PostgreSQLContainer<>("postgres:14.4").withDatabaseName("clinlims").withUsername("clinlims")
                .withPassword("clinlims").withEnv("POSTGRES_INITDB_ARGS", "--auth-host=md5").withCopyFileToContainer(
                        MountableFile.forClasspathResource("postgre-db-init"), "/docker-entrypoint-initdb.d");
        postgres.start();
        connection = postgres.createConnection("");
        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName("clinlims");
        database.setLiquibaseSchemaName("clinlims");
        changelog("liquibase/base-changelog.xml").update(CONTEXTS);
    }

    @After
    public void closeOwnedDatabase() throws Exception {
        try {
            if (connection != null) {
                connection.close();
            }
        } finally {
            if (postgres != null) {
                postgres.close();
            }
            if (fixtureLoader != null) {
                fixtureLoader.close();
            }
        }
    }

    @Test
    public void freshInstallationHasOnlyTargetAuthority() throws Exception {
        cutover("liquibase/amr-cutover-empty-map.csv").update(CONTEXTS);
        assertTargetSchema();
        assertEquals("0", scalar("select count(*) from clinlims.micro_case_specimen"));
        assertEquals("0", scalar("select count(*) from clinlims.micro_case"));
    }

    @Test
    public void upgradePreservesClinicalIdentityHistoryAndSeparateCases() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String applied = appliedHistory();
        Liquibase migration = cutover(MAP);
        migration.update(CONTEXTS);
        assertTargetSchema();
        assertEquals(before, clinicalSnapshot());
        assertEquals(applied, appliedHistory());
        assertEquals("4", scalar("select count(*) from clinlims.micro_case_specimen"));
        assertEquals("2",
                scalar("select count(*) from clinlims.micro_case where sample_id=990001 "
                        + "and sample_type_id=(select typeosamp_id from clinlims.sample_item where id=990001) "
                        + "and test_section_id=990001"));
        assertEquals("4", scalar("select count(*) from clinlims.micro_case where migration_review_required"));
        assertEquals("990002|990003", scalar(
                "select test_section_id||'|'||program_id from clinlims.micro_case " + "where id='case-unassigned'"));
        assertEquals("4",
                scalar("select count(*) from clinlims.micro_case_activity "
                        + "where activity_type='V2_MIGRATED' and performed_by='1' "
                        + "and occurred_at=timestamp '2026-10-06 12:00:00'"));
        assertEquals("Original pre-case clinical history", scalar("select detail->>'clinical_history' "
                + "from clinlims.configuration_import_run run, jsonb_array_elements(run.summary::jsonb->'details') detail "
                + "where run.id='amr-v2-cutover-20261006' and detail->>'id'='draft-original'"));
        migration.update(CONTEXTS);
        assertEquals("4",
                scalar("select count(*) from clinlims.micro_case_activity where activity_type='V2_MIGRATED'"));
        assertEquals(before, clinicalSnapshot());
    }

    @Test
    public void unresolvedCaseMappingFailsBeforeChangingClinicalData() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(cutover("liquibase/amr-cutover-incomplete-map.csv"),
                "AMR cutover requires a Program and lab unit for every existing case");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertTrue(columnExists("micro_case", "workflow_type"));
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void duplicateMappingFailsAtomically() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(cutover("liquibase/amr-cutover-duplicate-map.csv"), "amr_cutover_case_map_pkey");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertFalse(tableExists("amr_cutover_case_map"));
    }

    @Test
    public void invalidProgramOrLabUnitDoesNotCreateClinicalOwnership() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(cutover("liquibase/amr-cutover-invalid-reference-map.csv"),
                "AMR cutover requires a Program and lab unit for every existing case");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void existingAuditIdentityCollisionRollsBackTheWholeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by,note) "
                            + "values(md5('amr-v2-migration:case-bacteria')::uuid::text,'case-bacteria','NOTE_ADDED',"
                            + "timestamp '2026-09-01 09:00:00','1','Existing attributable audit record')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(cutover(MAP), "pk_micro_case_activity");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertTrue(tableExists("micro_culture_setup"));
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void thousandExistingCasesKeepTheirIdsWithoutRegrouping() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into clinlims.sample_item(id,sort_order,samp_id,typeosamp_id,status_id,collection_date) "
                            + "select n,n,990001,(select typeosamp_id from clinlims.sample_item where id=990001),"
                            + "(select status_id from clinlims.sample_item where id=990001),"
                            + "timestamptz '2026-09-01 08:00:00+00' from generate_series(991000,991999) n");
            statement.executeUpdate(
                    "insert into clinlims.micro_case(id,sample_item_id,workflow_type,created_at,created_by) "
                            + "select 'case-scale-'||n,n,'BACTERIOLOGY',timestamp '2026-09-01 09:00:00','1' "
                            + "from generate_series(991000,991999) n");
        }
        connection.commit();
        StringBuilder mappings = new StringBuilder(
                Files.readString(Path.of("src/test/resources", MAP), StandardCharsets.UTF_8));
        for (int id = 991000; id < 992000; id++) {
            mappings.append("case-scale-").append(id).append(",990001,990001\n");
        }
        String filename = "scale-" + UUID.randomUUID() + ".csv";
        Files.writeString(Path.of("target", "amr-cutover-fixtures", filename), mappings, StandardCharsets.UTF_8);
        Map<String, String> before = clinicalSnapshot();
        cutover(filename).update(CONTEXTS);
        assertTargetSchema();
        assertEquals(before, clinicalSnapshot());
        assertEquals("1004", scalar("select count(*) from clinlims.micro_case"));
        assertEquals("1004", scalar("select count(*) from clinlims.micro_case_specimen"));
        assertEquals("1004",
                scalar("select count(*) from clinlims.micro_case_activity where activity_type='V2_MIGRATED'"));
        assertEquals("1000",
                scalar("select count(*) from clinlims.micro_case c join clinlims.micro_case_specimen member "
                        + "on member.case_id=c.id where c.id='case-scale-'||member.sample_item_id"));
    }

    @Test
    public void rollbackAndReapplyRetainAllClinicalAndAuditRecords() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        String cases = scalar("select jsonb_agg(to_jsonb(c) order by id)::text from clinlims.micro_case c");
        String setups = scalar("select jsonb_agg(to_jsonb(c) order by id)::text from clinlims.micro_culture_setup c");
        Liquibase migration = cutover(MAP);
        migration.update(CONTEXTS);
        Map<String, String> migrated = clinicalSnapshot();
        migration.rollback(1, "default");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertEquals(cases, scalar("select jsonb_agg(to_jsonb(c) order by id)::text from clinlims.micro_case c"));
        assertEquals(setups,
                scalar("select jsonb_agg(to_jsonb(c) order by id)::text from clinlims.micro_culture_setup c"));
        assertFalse(tableExists("micro_case_specimen"));
        migration.update(CONTEXTS);
        assertTargetSchema();
        assertEquals(migrated, clinicalSnapshot());
    }

    @Test
    public void rollbackRefusesToEraseWorkRecordedAfterCutover() throws Exception {
        seed();
        Liquibase migration = cutover(MAP);
        migration.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_ast_reading set raw_value=4.0 where id='reading-repeat'");
        }
        connection.commit();
        try {
            migration.rollback(1, "default");
            fail("Rollback must preserve work recorded after cutover");
        } catch (LiquibaseException exception) {
            assertTrue(exception.toString(), exception.toString().contains("clinical work changed after cutover"));
        }
        assertEquals("4.0000", scalar("select raw_value from clinlims.micro_ast_reading where id='reading-repeat'"));
        assertTargetSchema();
    }

    @Test
    public void rollbackPreservesChangesToTheMigrationTimeline() throws Exception {
        seed();
        Liquibase migration = cutover(MAP);
        migration.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_case_activity set note='Reviewed explicit ownership' "
                    + "where id=md5('amr-v2-migration:case-bacteria')::uuid::text");
        }
        connection.commit();
        try {
            migration.rollback(1, "default");
            fail("Rollback must preserve changes to migration activity");
        } catch (LiquibaseException exception) {
            assertTrue(exception.toString(), exception.toString().contains("clinical work changed after cutover"));
        }
        assertEquals("Reviewed explicit ownership", scalar("select note from clinlims.micro_case_activity "
                + "where id=md5('amr-v2-migration:case-bacteria')::uuid::text"));
        assertTargetSchema();
    }

    private void seed() throws Exception {
        changelog(FIXTURE).update(CONTEXTS);
    }

    private Liquibase changelog(String path) {
        return new Liquibase(path, new ClassLoaderResourceAccessor(fixtureLoader), database);
    }

    @Test
    public void candidateCutoverPreservesRecognizedObservationHistory() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_activity"
                    + "(id,case_id,activity_type,occurred_at,performed_by,note,structured_data) values "
                    + "('recognized-positive','case-bacteria','STAGE_CHANGED',timestamp '2026-09-01 12:34:56',1,"
                    + "'Original bottle signal','{\"from\":\"INCUBATING\",\"to\":\"POSITIVE_SIGNAL\"}')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String applied = appliedHistory();
        Liquibase migration = cutover(MAP);
        migration.update(CONTEXTS);
        assertTargetSchema();
        assertEquals(before, clinicalSnapshot());
        assertEquals(applied, appliedHistory());
        migration.rollback(1, "default");
        assertEquals(before, clinicalSnapshot());
        assertTrue(columnExists("micro_case", "sample_item_id"));
    }

    @Test
    public void candidateCutoverRejectsUnresolvableObservationBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_activity"
                    + "(id,case_id,activity_type,occurred_at,performed_by,structured_data) values "
                    + "('invalid-positive','case-tb','STAGE_CHANGED',timestamp '2026-09-01 12:34:56',1,'unresolvable stage history')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String applied = allHistory();
        expectFailure(cutover(MAP), "AMR cutover requires resolvable stage history");
        assertEquals(before, clinicalSnapshot());
        assertEquals(applied, allHistory());
        assertTrue(columnExists("micro_case", "sample_item_id"));
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void candidateCutoverDoesNotTreatUnknownStageHistoryAsNoResults() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_activity"
                    + "(id,case_id,activity_type,occurred_at,performed_by,structured_data) values "
                    + "('unknown-observation','case-tb','STAGE_CHANGED',timestamp '2026-09-01 12:34:56',1,'{\"to\":\"UNKNOWN_OBSERVATION\"}')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String applied = allHistory();
        expectFailure(cutover(MAP), "AMR cutover requires resolvable stage history");
        assertEquals(before, clinicalSnapshot());
        assertEquals(applied, allHistory());
        assertTrue(columnExists("micro_case", "sample_item_id"));
    }

    @Test
    public void candidateCutoverRejectsCrossCaseSubcultureBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by)"
                            + " values ('cross-case-activity','case-tb','SUBCULTURE_RECORDED',timestamp '2026-09-01 12:34:56','1')");
            statement.executeUpdate(
                    "insert into clinlims.micro_case_inoculation(id,case_id,activity_id,source_inoculation_id,"
                            + "container_identifier,media,occurred_at,performed_by) values ('cross-case-culture','case-tb',"
                            + "'cross-case-activity','inoculation-original','AMR-CROSS-PLATE','Blood agar',timestamp '2026-09-01 12:34:56','1')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(cutover(MAP), "subculture parent in the same case");
        assertEquals(before, clinicalSnapshot());
        assertEquals(history, allHistory());
        assertTrue(columnExists("micro_case", "workflow_type"));
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void candidateCutoverRejectsDuplicateAnalysisOwnersBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_analysis(id,case_id,analysis_id)"
                    + " values ('duplicate-owner','case-tb',990010)");
        }
        connection.commit();
        Map<String, String> clinicalBefore = clinicalSnapshot();
        String historyBefore = allHistory();

        expectFailure(cutover(MAP), "one owner for each analysis");

        assertEquals(clinicalBefore, clinicalSnapshot());
        assertEquals(historyBefore, allHistory());
        assertTrue(columnExists("micro_case", "workflow_type"));
        assertFalse(tableExists("micro_case_specimen"));
    }

    @Test
    public void freshMembershipMigrationCreatesCanonicalSourceAndOwnershipConstraints() throws Exception {
        cutover("liquibase/amr-cutover-empty-map.csv").update(CONTEXTS);
        membershipMigration().update(CONTEXTS);
        assertTrue(columnExists("micro_isolate", "source_sample_item_id"));
        assertTrue(columnExists("micro_case_analysis", "case_role"));
        assertTrue(columnExists("test", "collected_in_sets"));
        assertEquals("0", scalar("select count(*) from clinlims.micro_isolate"));
        assertEquals("1", scalar("select count(*) from information_schema.table_constraints"
                + " where constraint_schema='clinlims' and constraint_name='uq_micro_case_analysis_owner'"));
    }

    @Test
    public void membershipMigrationPreservesClinicalDataAndRehearsesCompleteRollback() throws Exception {
        seed();
        Map<String, String> original = clinicalSnapshot();
        String historicalChangesets = allHistory();
        Liquibase cutover = cutover(MAP);
        cutover.update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        assertEquals("990001",
                scalar("select source_sample_item_id from clinlims.micro_isolate where id='isolate-original'"));
        assertEquals("CULTURE", scalar("select case_role from clinlims.micro_case_analysis where id='link-bacteria'"));
        assertEquals(original, clinicalSnapshot());
        assertEquals(historicalChangesets, appliedHistory());
        membership.rollback(1, "default");
        assertFalse(columnExists("micro_isolate", "source_sample_item_id"));
        assertFalse(columnExists("micro_case_analysis", "case_role"));
        assertEquals(original, clinicalSnapshot());
        assertEquals(historicalChangesets, appliedHistory());
        cutover.rollback(1, "default");
        assertEquals(original, clinicalSnapshot());
        assertEquals(historicalChangesets, appliedHistory());
        assertTrue(columnExists("micro_case", "sample_item_id"));
        assertEquals(historicalChangesets, allHistory());
        cutover.update(CONTEXTS);
        membership.update(CONTEXTS);
        assertEquals(original, clinicalSnapshot());
        assertEquals(historicalChangesets, appliedHistory());
        assertEquals("990001",
                scalar("select source_sample_item_id from clinlims.micro_isolate where id='isolate-original'"));
    }

    @Test
    public void observationSourceMustBelongToItsCase() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        membershipMigration().update(CONTEXTS);
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("update clinlims.micro_case_activity set result_source_sample_item_id=990003"
                        + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text");
            }
            connection.commit();
            fail("An observation cannot use a specimen outside its case");
        } catch (java.sql.SQLException exception) {
            assertEquals("23503", exception.getSQLState());
            assertTrue(exception.getMessage(), exception.getMessage().contains("fk_micro_activity_result_source"));
            connection.rollback();
        }
        assertEquals("990001", scalar("select result_source_sample_item_id from clinlims.micro_case_activity"
                + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text"));
        addSecondCultureMember();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_case_activity set result_source_sample_item_id=990004"
                    + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text");
        }
        connection.commit();
        assertEquals("990004", scalar("select result_source_sample_item_id from clinlims.micro_case_activity"
                + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text"));
    }

    @Test
    public void cultureObservationOwnershipPreservesHistoricalActorsAndRollback() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_activity"
                    + "(id,case_id,activity_type,occurred_at,performed_by,note,structured_data) values "
                    + "('original-positive','case-tb','STAGE_CHANGED',timestamp '2026-09-01 12:34:56',1,"
                    + "'Original bottle signal','{\"from\":\"INCUBATING\",\"to\":\"POSITIVE_SIGNAL\"}')");
            statement.executeUpdate(
                    "update clinlims.micro_case set stage='NO_GROWTH_READY'" + " where id='case-no-analysis'");
        }
        connection.commit();
        Map<String, String> original = clinicalSnapshot();
        Liquibase cutover = cutover(MAP);
        cutover.update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        assertTrue(columnExists("micro_case_activity", "result_source_sample_item_id"));
        assertEquals("990001", scalar("select result_source_sample_item_id from clinlims.micro_case_activity"
                + " where id='original-positive'"));
        assertEquals("990003", scalar("select result_source_sample_item_id from clinlims.micro_case_activity"
                + " where id=md5('amr-v2-migration:case-no-analysis')::uuid::text"));
        assertEquals(original, clinicalSnapshot());
        membership.rollback(1, "default");
        assertFalse(columnExists("micro_case_activity", "result_source_sample_item_id"));
        cutover.rollback(1, "default");
        assertEquals(original, clinicalSnapshot());
    }

    @Test
    public void membershipRejectsAmbiguousCultureObservationBeforeChangingSchema() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_specimen"
                    + "(id,case_id,sample_item_id,created_at) values ('observation-member','case-tb',990002,timestamp '2026-09-01 12:34:56')");
            statement.executeUpdate("insert into clinlims.micro_case_activity"
                    + "(id,case_id,activity_type,occurred_at,performed_by,structured_data) values "
                    + "('ambiguous-positive','case-tb','STAGE_CHANGED',timestamp '2026-09-01 12:34:56',1,'{\"to\":\"POSITIVE_SIGNAL\"}')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        expectFailure(membershipMigration(), "unambiguous source specimen for each existing culture observation");
        assertEquals(before, clinicalSnapshot());
        assertFalse(columnExists("micro_case_activity", "result_source_sample_item_id"));
    }

    @Test
    public void membershipRollbackCannotEraseChangedMigratedObservationOwnership() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        addSecondCultureMember();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_case_activity set result_source_sample_item_id=990004"
                    + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text");
        }
        connection.commit();
        try {
            membership.rollback(1, "default");
            fail("Rollback must preserve observation ownership recorded after migration");
        } catch (LiquibaseException expected) {
            assertTrue(expected.getMessage().contains("AMR membership rollback refused"));
        }
        assertEquals("990004", scalar("select result_source_sample_item_id from clinlims.micro_case_activity"
                + " where id=md5('amr-v2-migration:case-bacteria')::uuid::text"));
    }

    @Test
    public void membershipMigrationRejectsDuplicateAnalysisOwnersBeforeSchemaChanges() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_analysis(id,case_id,analysis_id)"
                    + " values ('duplicate-owner','case-tb',990010)");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        expectFailure(membershipMigration(), "one owner for each analysis");
        assertEquals(before, clinicalSnapshot());
        assertFalse(columnExists("micro_isolate", "source_sample_item_id"));
        assertFalse(columnExists("test", "collected_in_sets"));
    }

    @Test
    public void membershipMigrationRejectsAmbiguousIsolateOriginBeforeSchemaChanges() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_specimen(id,case_id,sample_item_id,created_at)"
                    + " values ('ambiguous-source','case-unassigned',990003,timestamp '2026-09-01 12:34:56')");
            statement.executeUpdate(
                    "insert into clinlims.micro_isolate(id,case_id,isolate_label,organism_id,significance,identification_status)"
                            + " values ('ambiguous-isolate','case-unassigned','2','organism-original','CLINICALLY_SIGNIFICANT','CONFIRMED')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        expectFailure(membershipMigration(), "unambiguous source specimen for each existing isolate");
        assertEquals(before, clinicalSnapshot());
        assertFalse(columnExists("micro_isolate", "source_sample_item_id"));
    }

    @Test
    public void membershipRollbackPreservesLaterClinicalWorkAndItsSource() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_ast_reading set raw_value=4.0 where id='reading-repeat'");
        }
        connection.commit();
        try {
            membership.rollback(1, "default");
            fail("Membership rollback must preserve later clinical work and provenance");
        } catch (LiquibaseException exception) {
            StringBuilder messages = new StringBuilder();
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                messages.append(cause.getMessage()).append('\n');
            }
            assertTrue(messages.toString(),
                    messages.toString().contains("clinical work or catalog changed after migration"));
        }
        assertEquals("4.0000", scalar("select raw_value from clinlims.micro_ast_reading where id='reading-repeat'"));
        assertEquals("990001",
                scalar("select source_sample_item_id from clinlims.micro_isolate where id='isolate-original'"));
        assertTrue(columnExists("micro_case_analysis", "case_role"));
    }

    @Test
    public void membershipRollbackPreservesLaterCaseOwnershipAndItsSource() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_case set test_section_id=990002,program_id=990003"
                    + " where id='case-bacteria'");
        }
        connection.commit();
        try {
            membership.rollback(1, "default");
            fail("Membership rollback must preserve later case ownership and provenance");
        } catch (LiquibaseException exception) {
            assertTrue(exception.toString(),
                    exception.toString().contains("clinical work or catalog changed after migration"));
        }
        assertEquals("990002|990003", scalar(
                "select test_section_id||'|'||program_id from clinlims.micro_case" + " where id='case-bacteria'"));
        assertEquals("990001",
                scalar("select source_sample_item_id from clinlims.micro_isolate where id='isolate-original'"));
        assertTrue(columnExists("micro_case_analysis", "case_role"));
    }

    @Test
    public void membershipBackfillsCultureSpecimenWithoutChangingClinicalHistory() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by)"
                            + " values ('activity-subculture','case-bacteria','SUBCULTURE_RECORDED',timestamp '2026-09-01 12:34:56','1')");
            statement.executeUpdate(
                    "insert into clinlims.micro_case_inoculation(id,case_id,activity_id,source_inoculation_id,"
                            + "container_identifier,media,occurred_at,performed_by) values ('subculture-original','case-bacteria',"
                            + "'activity-subculture','inoculation-original','AMR-SUB-PLATE','Blood agar',timestamp '2026-09-01 12:34:56','1')");
        }
        connection.commit();
        Map<String, String> original = clinicalSnapshot();
        cutover(MAP).update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);

        assertTrue("Cultures must carry explicit specimen provenance",
                columnExists("micro_case_inoculation", "source_sample_item_id"));
        assertEquals("990001|990001", scalar("select string_agg(source_sample_item_id::text,'|' order by id)"
                + " from clinlims.micro_case_inoculation"));
        assertEquals(original, clinicalSnapshot());
        membership.rollback(1, "default");
        assertFalse(columnExists("micro_case_inoculation", "source_sample_item_id"));
        assertEquals(original, clinicalSnapshot());
    }

    @Test
    public void membershipRejectsAmbiguousCultureSpecimenBeforeSchemaChanges() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.micro_case_specimen(id,case_id,sample_item_id,created_at)"
                    + " values ('culture-ambiguous-member','case-unassigned',990003,timestamp '2026-09-01 12:34:56')");
            statement.executeUpdate(
                    "insert into clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by)"
                            + " values ('culture-ambiguous-activity','case-unassigned','INOCULATION_RECORDED',timestamp '2026-09-01 12:34:56','1')");
            statement.executeUpdate(
                    "insert into clinlims.micro_case_inoculation(id,case_id,activity_id,container_identifier,"
                            + "media,occurred_at,performed_by) values ('culture-ambiguous','case-unassigned',"
                            + "'culture-ambiguous-activity','AMR-AMB-PLATE','Blood agar',timestamp '2026-09-01 12:34:56','1')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        expectFailure(membershipMigration(), "unambiguous source specimen for each existing culture");
        assertEquals(before, clinicalSnapshot());
        assertFalse(columnExists("micro_case_inoculation", "source_sample_item_id"));
        assertFalse(columnExists("test", "collected_in_sets"));
    }

    @Test
    public void cultureProvenanceConstraintsRejectForeignMembersAndParentsAtCommit() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        membershipMigration().update(CONTEXTS);
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("update clinlims.micro_case_inoculation set source_sample_item_id=990002"
                        + " where id='inoculation-original'");
            }
            connection.commit();
            fail("A culture cannot use a specimen outside its case");
        } catch (java.sql.SQLException exception) {
            assertEquals("23503", exception.getSQLState());
            assertTrue(exception.getMessage(), exception.getMessage().contains("fk_micro_culture_source_member"));
            connection.rollback();
        }
        addSecondCultureMember();
        connection.commit();
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "insert into clinlims.micro_case_activity(id,case_id,activity_type,occurred_at,performed_by)"
                                + " values ('mismatched-parent-activity','case-bacteria','SUBCULTURE_RECORDED',timestamp '2026-09-01 12:34:56','1')");
                statement.executeUpdate(
                        "insert into clinlims.micro_case_inoculation(id,case_id,activity_id,source_inoculation_id,"
                                + "source_sample_item_id,container_identifier,media,occurred_at,performed_by)"
                                + " values ('mismatched-parent','case-bacteria','mismatched-parent-activity','inoculation-original',"
                                + "990004,'AMR-MISMATCH','Blood agar',timestamp '2026-09-01 12:34:56','1')");
            }
            connection.commit();
            fail("A subculture cannot change its parent's specimen");
        } catch (java.sql.SQLException exception) {
            assertEquals("23503", exception.getSQLState());
            assertTrue(exception.getMessage(), exception.getMessage().contains("fk_micro_subculture_source_member"));
            connection.rollback();
        }
        assertEquals("990001", scalar("select source_sample_item_id from clinlims.micro_case_inoculation"
                + " where id='inoculation-original'"));
        assertEquals("0", scalar("select count(*) from clinlims.micro_case_inoculation where id='mismatched-parent'"));
    }

    @Test
    public void membershipRollbackPreservesLaterCultureProvenance() throws Exception {
        seed();
        cutover(MAP).update(CONTEXTS);
        Liquibase membership = membershipMigration();
        membership.update(CONTEXTS);
        addSecondCultureMember();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.micro_case_inoculation set source_sample_item_id=990004"
                    + " where id='inoculation-original'");
        }
        connection.commit();
        try {
            membership.rollback(1, "default");
            fail("Rollback must preserve later culture provenance");
        } catch (LiquibaseException exception) {
            assertTrue(exception.toString(),
                    exception.toString().contains("clinical work or catalog changed after migration"));
        }
        assertEquals("990004", scalar("select source_sample_item_id from clinlims.micro_case_inoculation"
                + " where id='inoculation-original'"));
        assertTrue(columnExists("micro_case_inoculation", "source_sample_item_id"));
    }

    @Test
    public void completeSequenceRejectsMembershipHistoryCollisionBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.configuration_import_run(id,source,status,started_at,summary)"
                    + " values ('amr-v2-membership-20261006','EXISTING_IMPORT','EXPORTED',"
                    + "timestamp '2026-09-01 12:34:56','preserve this prior import')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(completeSequence(), "membership migration history identifier already exists");
        assertTrue("A later membership failure must not leave the old runtime without its schema",
                columnExists("micro_case", "workflow_type"));
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
        assertEquals("preserve this prior import", scalar(
                "select summary from clinlims.configuration_import_run" + " where id='amr-v2-membership-20261006'"));
    }

    @Test
    public void completeSequenceRejectsMembershipColumnCollisionBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("alter table clinlims.test add column collected_in_sets boolean default true");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(completeSequence(), "collected_in_sets");
        assertTrue("Target-column conflicts must be rejected before retiring workflow authority",
                columnExists("micro_case", "workflow_type"));
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
        assertEquals("0", scalar("select count(*) from clinlims.test where collected_in_sets is not true"));
    }

    @Test
    public void completeSequenceRejectsProgramColumnCollisionBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("alter table clinlims.program add column reporting_track_id numeric(10)");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(completeSequence(), "reporting_track_id");
        assertTrue("Program conflicts must fail before retiring the old runtime schema",
                columnExists("micro_case", "workflow_type"));
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
    }

    @Test
    public void completeSequenceRejectsExportTableCollisionBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "create table clinlims.micro_export_reporting_track (id varchar(36) primary key, existing_value text)");
            statement.executeUpdate(
                    "insert into clinlims.micro_export_reporting_track values ('existing', 'preserve existing data')");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(completeSequence(), "micro_export_reporting_track");
        assertTrue("Export conflicts must fail before retiring the old runtime schema",
                columnExists("micro_case", "workflow_type"));
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
        assertEquals("preserve existing data",
                scalar("select existing_value from clinlims.micro_export_reporting_track"));
    }

    @Test
    public void completeSequenceRejectsBottleSetCollisionBeforeCutover() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("alter table clinlims.sample_item add column culture_set_number integer");
        }
        connection.commit();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        expectFailure(completeSequence(), "culture_set_number");
        assertTrue(columnExists("micro_case", "workflow_type"));
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
    }

    @Test
    public void requestedBottleDetailCollisionFailsBeforeRetirement() throws Exception {
        seed();
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("alter table clinlims.sample_type_request add column body_site varchar(40)");
        }
        connection.commit();
        String history = allHistory();
        var before = clinicalSnapshot();
        expectFailure(completeSequence(), "requested bottle-detail target columns");
        assertTrue(tableExists("micro_culture_setup"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
    }

    @Test
    public void rollbackPreservesRequestDetailsEvenWithoutASetNumber() throws Exception {
        seed();
        Liquibase migration = completeSequence();
        migration.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("insert into clinlims.sample_type_request"
                    + " (id,sample_id,type_of_sample_id,sort_order,status,created_date,body_site)"
                    + " select 990019,samp_id,typeosamp_id,0,'REQUESTED',current_timestamp,'Left arm'"
                    + " from clinlims.sample_item where id=990001");
        }
        connection.commit();
        String history = allHistory();
        try {
            migration.rollback(1, "default");
            fail("Rollback must preserve recorded bottle details");
        } catch (LiquibaseException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("recorded set assignments"));
        }
        assertEquals("Left arm", scalar("select body_site from clinlims.sample_type_request where id=990019"));
        assertEquals(history, allHistory());
    }

    @Test
    public void bottleSetRollbackRefusesToDiscardRecordedAssignments() throws Exception {
        seed();
        Liquibase migration = completeSequence();
        migration.update(CONTEXTS);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update clinlims.sample_item set culture_set_number=2 where id=990001");
        }
        connection.commit();
        String history = allHistory();
        try {
            migration.rollback(1, "default");
            fail("Rollback must preserve recorded bottle sets");
        } catch (LiquibaseException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("recorded set assignments"));
        }
        assertEquals("2", scalar("select culture_set_number from clinlims.sample_item where id=990001"));
        assertEquals(history, allHistory());
    }

    @Test
    public void completeSequencePreservesUpgradeAndSupportsRollbackReapply() throws Exception {
        seed();
        Map<String, String> before = clinicalSnapshot();
        String history = allHistory();
        Liquibase migration = completeSequence();
        migration.update(CONTEXTS);
        assertTargetSchema();
        assertTrue(columnExists("test", "collected_in_sets"));
        assertTrue(columnExists("program", "reporting_track_id"));
        assertTrue(tableExists("micro_export_reporting_track"));
        assertTrue(columnExists("sample_item", "culture_set_number"));
        assertEquals("0", scalar("select count(*) from clinlims.sample_item where culture_set_number is not null"));
        assertEquals(before, clinicalSnapshot());
        migration.rollback(4, "default");
        assertTrue(columnExists("micro_case", "workflow_type"));
        assertFalse(columnExists("test", "collected_in_sets"));
        assertEquals(history, allHistory());
        assertEquals(before, clinicalSnapshot());
        migration.update(CONTEXTS);
        assertTargetSchema();
        assertEquals(before, clinicalSnapshot());
    }

    private Liquibase completeSequence() {
        Liquibase migration = changelog("liquibase/amr-v2-sequence.xml");
        migration.setChangeLogParameter("amr.cutover.mappingFile", MAP);
        migration.setChangeLogParameter("amr.cutover.actorId", "1");
        migration.setChangeLogParameter("amr.cutover.at", "2026-10-06 12:00:00");
        return migration;
    }

    private void addSecondCultureMember() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "insert into clinlims.sample_item(id,sort_order,samp_id,typeosamp_id,status_id,collection_date)"
                            + " select 990004,2,samp_id,typeosamp_id,status_id,collection_date from clinlims.sample_item where id=990001");
            statement.executeUpdate("insert into clinlims.micro_case_specimen(id,case_id,sample_item_id,created_at)"
                    + " values ('second-culture-member','case-bacteria',990004,timestamp '2026-09-01 12:34:56')");
        }
    }

    private Liquibase membershipMigration() {
        Liquibase migration = changelog("liquibase/3.6.x.x/20261006-OGC-1427-amr-v2-membership.xml");
        migration.setChangeLogParameter("amr.cutover.actorId", "1");
        migration.setChangeLogParameter("amr.cutover.at", "2026-10-06 12:00:00");
        return migration;
    }

    private Liquibase cutover(String mappings) {
        Liquibase migration = changelog(CUTOVER);
        migration.setChangeLogParameter("amr.cutover.mappingFile", mappings);
        migration.setChangeLogParameter("amr.cutover.actorId", "1");
        migration.setChangeLogParameter("amr.cutover.at", "2026-10-06 12:00:00");
        return migration;
    }

    private void assertTargetSchema() throws Exception {
        assertTrue(tableExists("micro_case_specimen"));
        assertFalse(tableExists("micro_culture_setup"));
        assertFalse(tableExists("amr_cutover_case_map"));
        assertFalse(columnExists("micro_case", "workflow_type"));
        assertFalse(columnExists("micro_case", "sample_item_id"));
        assertFalse(columnExists("micro_case", "culture_method_id"));
        assertFalse(columnExists("test", "culture_workflow_type"));
        assertFalse(columnExists("micro_ast_panel", "workflow_type"));
        assertFalse(columnExists("micro_case_order_detail", "sample_id"));
        assertFalse(columnExists("micro_case_order_detail", "culture_method_id"));
        assertTrue(columnExists("micro_case", "test_section_id"));
        assertTrue(columnExists("micro_case", "program_id"));
        assertEquals("0", scalar("select count(*) from information_schema.columns where table_schema='clinlims' "
                + "and (table_name like 'micro_%' or table_name='test') and column_name like '%workflow_type%'"));
        assertEquals("4",
                scalar("select count(*) from information_schema.table_constraints where constraint_schema='clinlims' "
                        + "and table_name='micro_case' and constraint_type='FOREIGN KEY'"));
        assertEquals("2",
                scalar("select count(*) from information_schema.table_constraints where constraint_schema='clinlims' "
                        + "and table_name='micro_case_specimen' and constraint_type='FOREIGN KEY'"));
    }

    private Map<String, String> clinicalSnapshot() throws Exception {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("cases", scalar("select jsonb_agg(to_jsonb(c)-'workflow_type'-'sample_item_id'-'culture_method_id'"
                + "-'sample_id'-'sample_type_id'-'test_section_id'-'program_id'-'migration_review_required' order by id)::text "
                + "from clinlims.micro_case c"));
        for (String table : new String[] { "micro_isolate", "micro_isolate_identification_event", "micro_ast_run",
                "micro_ast_reading", "micro_ast_override_event", "micro_case_amendment", "micro_report_version",
                "micro_report_version_source", "micro_case_analysis", "micro_case_inoculation",
                "micro_inventory_usage_link" }) {
            String projection = "to_jsonb(r)"
                    + (("micro_isolate".equals(table) || "micro_case_inoculation".equals(table))
                            ? "-'source_sample_item_id'"
                            : "micro_case_analysis".equals(table) ? "-'case_role'-'collected_in_sets'" : "");
            snapshot.put(table, scalar("select coalesce(jsonb_agg(" + projection
                    + " order by id),'[]'::jsonb)::text from clinlims." + table + " r"));
        }
        snapshot.put("results", scalar(
                "select jsonb_agg(to_jsonb(r) order by id)::text from clinlims.result r where id between 990001 and 990099"));
        snapshot.put("analyses", scalar(
                "select jsonb_agg(to_jsonb(a) order by id)::text from clinlims.analysis a where id between 990001 and 990099"));
        snapshot.put("audit", scalar(
                "select coalesce(jsonb_agg(to_jsonb(a) order by id),'[]'::jsonb)::text from clinlims.history a"));
        snapshot.put("activities", scalar(
                "select jsonb_agg(to_jsonb(a)-'result_source_sample_item_id' order by id)::text from clinlims.micro_case_activity a where activity_type!='V2_MIGRATED'"));
        snapshot.put("panels", scalar(
                "select jsonb_agg(to_jsonb(p)-'workflow_type' order by id)::text from clinlims.micro_ast_panel p"));
        snapshot.put("clinicalContext", scalar(
                "select jsonb_agg(to_jsonb(d)-'sample_id'-'culture_method_id'-'discarded_at'-'discarded_by' order by id)::text "
                        + "from clinlims.micro_case_order_detail d where case_id is not null"));
        for (String table : new String[] { "inventory_item", "inventory_lot", "inventory_usage" }) {
            snapshot.put(table, scalar(
                    "select jsonb_agg(to_jsonb(r) order by id)::text from clinlims." + table + " r where id=990001"));
        }
        return snapshot;
    }

    private String appliedHistory() throws Exception {
        return scalar("select jsonb_agg(to_jsonb(h) order by orderexecuted)::text from clinlims.databasechangelog h "
                + "where id not in ('20261006-OGC-1426-amr-v2-cutover','20261006-OGC-1427-amr-v2-membership')");
    }

    private String allHistory() throws Exception {
        return scalar("select jsonb_agg(to_jsonb(h) order by orderexecuted)::text from clinlims.databasechangelog h");
    }

    private void expectFailure(Liquibase migration, String expectedMessage) throws Exception {
        try {
            migration.update(CONTEXTS);
            fail("Unsafe cutover unexpectedly succeeded");
        } catch (LiquibaseException exception) {
            StringBuilder messages = new StringBuilder();
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                messages.append(cause.getMessage()).append('\n');
            }
            assertTrue(messages.toString(), messages.toString().contains(expectedMessage));
        }
    }

    private String scalar(String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql)) {
            assertTrue("Expected one row", rows.next());
            String value = rows.getString(1);
            assertFalse("Expected exactly one row", rows.next());
            return value;
        }
    }

    private boolean columnExists(String table, String column) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(null, "clinlims", table, column)) {
            return columns.next();
        }
    }

    private boolean tableExists(String table) throws Exception {
        try (ResultSet tables = connection.getMetaData().getTables(null, "clinlims", table, new String[] { "TABLE" })) {
            return tables.next();
        }
    }
}
