package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.util.FileCopyUtils;

/**
 * OGC-608 V1 absorption — qa/022 gives every COMPLETED legacy distribution a
 * closed historical cycle so finished V1 rounds appear on the cycle-centric V2
 * pages. Active distributions are left alone; see
 * docs/eqa/v1-distribution-absorption.md.
 *
 * The changelog runs against an empty Testcontainers DB at context startup, so
 * the shipped changeset sees no legacy rows. This test seeds V1-shaped
 * distributions (cycle_id NULL) and executes the SAME SQL files the changeset
 * runs (backfill via sqlFile, rollback via sqlFile — single source of truth),
 * then verifies:
 *
 * <ul>
 * <li>a COMPLETED distribution becomes a CLOSED cycle with the distribution's
 * name and dates and one V1_BACKFILL audit row; DRAFT, PREPARED and SHIPPED
 * distributions stay unlinked and produce no cycle;
 * <li>cycle numbering continues after the scheme's existing cycles and starts
 * at 1 on a scheme with none;
 * <li>a distribution already linked to a cycle is untouched;
 * <li>a second run changes nothing (idempotence);
 * <li>the V2 provider scheme read lists the migrated cycle as closed history;
 * <li>the rollback removes exactly what the backfill created, and refuses when
 * a migrated cycle has been used since;
 * <li>the superseded legacy menu rows are off, My Programs stays on and moves
 * directly under EQA once its one-child group is retired.
 * </ul>
 */
public class EQAV1AbsorptionMigrationTest extends EQASpineTestBase {

    private static final String BACKFILL_SQL = "liquibase/qa/022-v1-distribution-cycle-backfill.sql";
    private static final String ROLLBACK_SQL = "liquibase/qa/022-v1-distribution-cycle-rollback.sql";

    // High ids to avoid colliding with fixture data; cleaned up by
    // cleanEqaTables below.
    private static final long COMPLETED_DIST = 9961L;
    private static final long SECOND_COMPLETED_DIST = 9962L;
    private static final long DRAFT_DIST = 9963L;
    private static final long PREPARED_DIST = 9964L;
    private static final long SHIPPED_DIST = 9965L;
    private static final long LINKED_DIST = 9966L;
    private static final long FRESH_SCHEME_DIST = 9967L;
    private static final long PARTICIPANT_ORG = 9967L;

    @Autowired
    private EQAShipmentService shipmentService;

    @Override
    protected void cleanEqaTables() {
        // Distributions and enrollments reference cycles/schemes, so they go
        // before the base deletes.
        jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE id BETWEEN 9961 AND 9967");
        jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id = ?", PARTICIPANT_ORG);
        super.cleanEqaTables();
    }

    @Test
    public void backfill_closesCompletedDistributionsAndLeavesActiveOnesUnlinked() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        // Existing V2 cycle: synthetic numbering must continue after it.
        Long existingCycleId = insertCycle(scheme, 3);

        insertLegacyDistribution(COMPLETED_DIST, scheme, "Serology run 1", "2025-01-10", "COMPLETED");
        insertLegacyDistribution(SECOND_COMPLETED_DIST, scheme, "Serology run 2", "2025-02-10", "COMPLETED");
        insertLegacyDistribution(DRAFT_DIST, scheme, "Serology run 3", "2025-03-10", "DRAFT");
        insertLegacyDistribution(PREPARED_DIST, scheme, "Serology run 4", "2025-04-10", "PREPARED");
        insertLegacyDistribution(SHIPPED_DIST, scheme, "Serology run 5", "2025-05-10", "SHIPPED");

        EQAProgram freshScheme = insertScheme("V1 absorption fresh scheme", EQASchemeType.REGIONAL_PT, "AFRO");
        insertLegacyDistribution(FRESH_SCHEME_DIST, freshScheme, "Fresh run", "2025-06-10", "COMPLETED");

        jdbc.update("INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                + " distribution_date, deadline, status, created_by, cycle_id, sys_user_id, last_updated)"
                + " VALUES (?, gen_random_uuid(), ?, 'Already linked', now(), now(), 'COMPLETED', ?, ?, ?, now())",
                LINKED_DIST, scheme.getId(), ADMIN_USER_ID, existingCycleId, USER);

        runSql(BACKFILL_SQL);

        // Date order drives numbering: the two completed runs become cycles 4
        // and 5 after the existing cycle 3.
        assertSyntheticCycle(COMPLETED_DIST, scheme.getId(), 4, "Serology run 1", "2025-01-10");
        assertSyntheticCycle(SECOND_COMPLETED_DIST, scheme.getId(), 5, "Serology run 2", "2025-02-10");
        // A scheme with no cycles starts at 1.
        assertSyntheticCycle(FRESH_SCHEME_DIST, freshScheme.getId(), 1, "Fresh run", "2025-06-10");

        // Active distributions are not migrated: no link, no cycle.
        for (long distId : new long[] { DRAFT_DIST, PREPARED_DIST, SHIPPED_DIST }) {
            assertNull("distribution " + distId + " must stay unlinked", cycleIdOf(distId));
        }
        assertEquals(3, countCycles(scheme.getId())); // existing 3 + two synthetic
        assertEquals(1, countCycles(freshScheme.getId()));

        // The pre-linked distribution keeps its cycle, and that cycle gains no
        // backfill audit row.
        assertEquals(existingCycleId, cycleIdOf(LINKED_DIST));
        assertEquals(0, countBackfillAuditRows(existingCycleId));
    }

    @Test
    public void backfill_isIdempotent() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption idempotence", EQASchemeType.INTER_LAB_SPLIT, "CPHL");
        insertLegacyDistribution(COMPLETED_DIST, scheme, "Only run", "2025-06-10", "COMPLETED");

        runSql(BACKFILL_SQL);
        Long cycleId = cycleIdOf(COMPLETED_DIST);
        assertEquals(1, countCycles(scheme.getId()));
        assertEquals(1, countBackfillAuditRows(cycleId));

        runSql(BACKFILL_SQL);

        assertEquals(cycleId, cycleIdOf(COMPLETED_DIST));
        assertEquals(1, countCycles(scheme.getId()));
        assertEquals(1, countBackfillAuditRows(cycleId));
        assertEquals(1, countAllBackfillAuditRows());
    }

    @Test
    public void providerSchemeRead_listsTheMigratedCycleAsClosedHistory() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption provider read", EQASchemeType.REGIONAL_PT, "This lab");
        enrollParticipant(scheme);
        insertLegacyDistribution(COMPLETED_DIST, scheme, "Historic run", "2025-06-10", "COMPLETED");

        runSql(BACKFILL_SQL);
        Long cycleId = cycleIdOf(COMPLETED_DIST);

        Map<String, Object> board = providerScheme(scheme.getId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cycles = (List<Map<String, Object>>) board.get("cycles");
        assertEquals(1, cycles.size());
        Map<String, Object> dto = cycles.get(0);
        assertEquals(cycleId.longValue(), ((Number) dto.get("id")).longValue());
        assertEquals(1, ((Number) dto.get("cycleNumber")).intValue());
        assertEquals("Historic run", dto.get("cycleName"));
        assertEquals("CLOSED", dto.get("status"));
        // Closed history must not count as an active cycle on the board.
        assertEquals(0, ((Number) board.get("activeCycleCount")).intValue());
    }

    @Test
    public void rollback_removesExactlyWhatTheBackfillCreated() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption rollback", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        Long existingCycleId = insertCycle(scheme, 1);
        insertLegacyDistribution(COMPLETED_DIST, scheme, "Rolled back run", "2025-06-10", "COMPLETED");
        jdbc.update("INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                + " distribution_date, deadline, status, created_by, cycle_id, sys_user_id, last_updated)"
                + " VALUES (?, gen_random_uuid(), ?, 'Already linked', now(), now(), 'COMPLETED', ?, ?, ?, now())",
                LINKED_DIST, scheme.getId(), ADMIN_USER_ID, existingCycleId, USER);

        runSql(BACKFILL_SQL);
        Long syntheticId = cycleIdOf(COMPLETED_DIST);
        assertEquals(2, countCycles(scheme.getId()));

        runSql(ROLLBACK_SQL);

        assertNull(cycleIdOf(COMPLETED_DIST));
        assertEquals(0,
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle WHERE id = ?", Long.class, syntheticId)
                        .longValue());
        assertEquals(0,
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ?",
                        Long.class, syntheticId).longValue());
        // The hand-made cycle and its link were never the backfill's, so they stay.
        assertEquals(existingCycleId, cycleIdOf(LINKED_DIST));
        assertEquals(1, countCycles(scheme.getId()));
    }

    @Test
    public void rollback_refusesWhenAMigratedCycleHasBeenUsedSince() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption rollback guard", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        insertLegacyDistribution(COMPLETED_DIST, scheme, "Reopened run", "2025-06-10", "COMPLETED");
        runSql(BACKFILL_SQL);
        Long syntheticId = cycleIdOf(COMPLETED_DIST);

        // An operator override recorded after the migration.
        jdbc.update("INSERT INTO clinlims.eqa_cycle_state_transition (id, cycle_id, prior_state, new_state,"
                + " state_machine, trigger_type, trigger_event, triggered_by, reason, occurred_at, sys_user_id)"
                + " VALUES (nextval('clinlims.eqa_cycle_state_transition_seq'), ?, 'CLOSED', 'SCORED', 'PROVIDER',"
                + " 'MANUAL', 'MANUAL_OVERRIDE', ?, 'Re-scoring a legacy round', now(), ?)", syntheticId, ADMIN_USER_ID,
                USER);

        try {
            runSql(ROLLBACK_SQL);
            fail("rollback must refuse a cycle with post-migration activity");
        } catch (DataAccessException expected) {
            assertTrue(rootMessage(expected), rootMessage(expected).contains("rollback refused"));
            assertTrue(rootMessage(expected), rootMessage(expected).contains(String.valueOf(syntheticId)));
        }

        // Nothing was touched.
        assertEquals(syntheticId, cycleIdOf(COMPLETED_DIST));
        assertEquals(1, countCycles(scheme.getId()));
        assertEquals(2,
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ?",
                        Long.class, syntheticId).longValue());
    }

    @Test
    public void supersededLegacyMenuRowsAreOff_myProgramsStaysOn() {
        for (String elementId : new String[] { "menu_eqa_orders", "menu_eqa_distribution",
                "menu_eqa_mgmt_distributions" }) {
            assertFalse(elementId + " should be deactivated", menuIsActive(elementId));
        }
        assertTrue("menu_eqa_my_programs should stay active", menuIsActive("menu_eqa_my_programs"));
        // The one-child "EQA Tests" group is gone: My Programs sits directly under EQA.
        assertFalse("menu_eqa_tests should be deactivated", menuIsActive("menu_eqa_tests"));
        Map<String, Object> myPrograms = jdbc.queryForMap("SELECT p.element_id AS parent, m.presentation_order"
                + " FROM clinlims.menu m JOIN clinlims.menu p ON p.id = m.parent_id"
                + " WHERE m.element_id = 'menu_eqa_my_programs'");
        assertEquals("menu_eqa", myPrograms.get("parent"));
        assertEquals(25, ((Number) myPrograms.get("presentation_order")).intValue());
    }

    private void runSql(String classpathFile) throws IOException {
        String sql = FileCopyUtils.copyToString(
                new InputStreamReader(new ClassPathResource(classpathFile).getInputStream(), StandardCharsets.UTF_8));
        jdbc.execute(sql);
    }

    private void insertLegacyDistribution(long id, EQAProgram scheme, String name, String distributionDate,
            String status) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                        + " distribution_date, deadline, status, created_by, sys_user_id, last_updated)"
                        + " VALUES (?, gen_random_uuid(), ?, ?, ?, ?::timestamp + interval '30 days', ?, ?, ?, now())",
                id, scheme.getId(), name, Date.valueOf(distributionDate), Date.valueOf(distributionDate), status,
                ADMIN_USER_ID, USER);
    }

    private void enrollParticipant(EQAProgram scheme) {
        jdbc.update(
                "INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                        + " VALUES (?, 'V1 absorption participant', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING",
                PARTICIPANT_ORG);
        jdbc.update(
                "INSERT INTO clinlims.eqa_program_enrollment (id, eqa_program_id, organization_id,"
                        + " enrollment_date, status, sys_user_id, lastupdated)"
                        + " VALUES (nextval('clinlims.eqa_enrollment_seq'), ?, ?, now(), 'Active', ?, now())",
                scheme.getId(), PARTICIPANT_ORG, USER);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> providerScheme(Long schemeId) {
        List<Map<String, Object>> schemes = (List<Map<String, Object>>) shipmentService.getProviderSchemes()
                .get("schemes");
        return schemes.stream().filter(s -> ((Number) s.get("id")).longValue() == schemeId).findFirst()
                .orElseThrow(() -> new AssertionError("scheme " + schemeId + " missing from the provider board"));
    }

    private void assertSyntheticCycle(long distId, Long schemeId, int expectedNumber, String expectedName,
            String distributionDate) {
        Map<String, Object> cycle = jdbc.queryForMap("SELECT c.id, c.scheme_id, c.cycle_number, c.cycle_name,"
                + " c.planned_start_date, c.planned_end_date, c.actual_start_date, c.actual_end_date, c.status,"
                + " c.created_by FROM clinlims.eqa_cycle c JOIN clinlims.eqa_distribution d ON d.cycle_id = c.id"
                + " WHERE d.id = ?", distId);
        assertEquals(schemeId.longValue(), ((Number) cycle.get("scheme_id")).longValue());
        assertEquals(expectedNumber, ((Number) cycle.get("cycle_number")).intValue());
        assertEquals(expectedName, cycle.get("cycle_name"));
        assertEquals("CLOSED", cycle.get("status"));
        Date start = Date.valueOf(distributionDate);
        Date end = Date.valueOf(start.toLocalDate().plusDays(30));
        assertEquals(start, cycle.get("planned_start_date"));
        assertEquals(end, cycle.get("planned_end_date"));
        assertEquals(start, cycle.get("actual_start_date"));
        assertEquals(end, cycle.get("actual_end_date"));
        assertEquals(ADMIN_USER_ID, ((Number) cycle.get("created_by")).longValue());

        long cycleId = ((Number) cycle.get("id")).longValue();
        List<Map<String, Object>> audit = jdbc.queryForList(
                "SELECT prior_state, new_state, state_machine, trigger_type, reason"
                        + " FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ? AND trigger_event = 'V1_BACKFILL'",
                cycleId);
        assertEquals(1, audit.size());
        assertNull(audit.get(0).get("prior_state"));
        assertEquals("CLOSED", audit.get(0).get("new_state"));
        assertEquals("PROVIDER", audit.get(0).get("state_machine"));
        assertEquals("AUTO", audit.get(0).get("trigger_type"));
        assertEquals("Cycle created from legacy distribution " + distId, audit.get(0).get("reason"));
    }

    private Long cycleIdOf(long distId) {
        return jdbc.queryForObject("SELECT cycle_id FROM clinlims.eqa_distribution WHERE id = ?", Long.class, distId);
    }

    private long countCycles(Long schemeId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle WHERE scheme_id = ?", Long.class, schemeId);
    }

    private long countBackfillAuditRows(Long cycleId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle_state_transition"
                + " WHERE cycle_id = ? AND trigger_event = 'V1_BACKFILL'", Long.class, cycleId);
    }

    private long countAllBackfillAuditRows() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle_state_transition WHERE trigger_event = 'V1_BACKFILL'",
                Long.class);
    }

    private boolean menuIsActive(String elementId) {
        return jdbc.queryForObject("SELECT is_active FROM clinlims.menu WHERE element_id = ?", Boolean.class,
                elementId);
    }
}
