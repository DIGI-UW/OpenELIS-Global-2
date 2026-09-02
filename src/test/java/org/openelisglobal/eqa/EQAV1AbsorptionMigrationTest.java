package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/**
 * OGC-608 V1 absorption — qa/022 gives every legacy distribution a synthetic
 * provider cycle so V1 data appears on the cycle-centric V2 pages.
 *
 * The changelog runs against an empty Testcontainers DB at context startup, so
 * the shipped changeset sees no legacy rows. This test seeds V1-shaped
 * distributions (cycle_id NULL) and executes the SAME SQL the changeset runs
 * (022-v1-distribution-cycle-backfill.sql via sqlFile — single source of
 * truth), then verifies:
 *
 * <ul>
 * <li>each distribution status maps to its provider-machine cycle state (DRAFT
 * to PLANNED, PREPARED to READY_TO_SHIP, SHIPPED to SHIPPED, COMPLETED to
 * CLOSED);
 * <li>cycle numbering continues after the scheme's existing cycles and starts
 * at 1 on a scheme with none;
 * <li>each synthetic cycle carries the distribution's name and dates and one
 * V1_BACKFILL audit row;
 * <li>a distribution already linked to a cycle is untouched;
 * <li>a second run changes nothing (idempotence);
 * <li>the superseded legacy menu rows are off and My Programs stays on.
 * </ul>
 */
public class EQAV1AbsorptionMigrationTest extends EQASpineTestBase {

    private static final String BACKFILL_SQL = "liquibase/qa/022-v1-distribution-cycle-backfill.sql";

    // High ids to avoid colliding with fixture data; cleaned up by
    // cleanEqaTables below.
    private static final long DRAFT_DIST = 9961L;
    private static final long PREPARED_DIST = 9962L;
    private static final long SHIPPED_DIST = 9963L;
    private static final long COMPLETED_DIST = 9964L;
    private static final long FRESH_SCHEME_DIST = 9965L;
    private static final long LINKED_DIST = 9966L;

    @Override
    protected void cleanEqaTables() {
        // Distributions reference cycles, so they go before the base deletes.
        jdbc.update("DELETE FROM clinlims.eqa_distribution WHERE id BETWEEN 9961 AND 9966");
        super.cleanEqaTables();
    }

    @Test
    public void backfill_givesEveryLegacyDistributionACycleWithTheMappedState() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        // Existing V2 cycle: synthetic numbering must continue after it.
        Long existingCycleId = insertCycle(scheme, 3);

        insertLegacyDistribution(DRAFT_DIST, scheme, "Serology run 1", "2025-01-10", "DRAFT");
        insertLegacyDistribution(PREPARED_DIST, scheme, "Serology run 2", "2025-02-10", "PREPARED");
        insertLegacyDistribution(SHIPPED_DIST, scheme, "Serology run 3", "2025-03-10", "SHIPPED");
        insertLegacyDistribution(COMPLETED_DIST, scheme, "Serology run 4", "2025-04-10", "COMPLETED");

        EQAProgram freshScheme = insertScheme("V1 absorption fresh scheme", EQASchemeType.REGIONAL_PT, "AFRO");
        insertLegacyDistribution(FRESH_SCHEME_DIST, freshScheme, "Fresh run", "2025-05-10", "COMPLETED");

        jdbc.update("INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                + " distribution_date, deadline, status, created_by, cycle_id, sys_user_id, last_updated)"
                + " VALUES (?, gen_random_uuid(), ?, 'Already linked', now(), now(), 'SHIPPED', ?, ?, ?, now())",
                LINKED_DIST, scheme.getId(), ADMIN_USER_ID, existingCycleId, USER);

        runBackfill();

        // Date order drives numbering: runs 1-4 become cycles 4-7 after the
        // existing cycle 3.
        assertSyntheticCycle(DRAFT_DIST, scheme.getId(), 4, "Serology run 1", "2025-01-10", "PLANNED");
        assertSyntheticCycle(PREPARED_DIST, scheme.getId(), 5, "Serology run 2", "2025-02-10", "READY_TO_SHIP");
        assertSyntheticCycle(SHIPPED_DIST, scheme.getId(), 6, "Serology run 3", "2025-03-10", "SHIPPED");
        assertSyntheticCycle(COMPLETED_DIST, scheme.getId(), 7, "Serology run 4", "2025-04-10", "CLOSED");
        // A scheme with no cycles starts at 1.
        assertSyntheticCycle(FRESH_SCHEME_DIST, freshScheme.getId(), 1, "Fresh run", "2025-05-10", "CLOSED");

        // The pre-linked distribution keeps its cycle, and that cycle gains no
        // backfill audit row.
        long linkedCycleId = jdbc.queryForObject("SELECT cycle_id FROM clinlims.eqa_distribution WHERE id = ?",
                Long.class, LINKED_DIST);
        assertEquals(existingCycleId.longValue(), linkedCycleId);
        assertEquals(0, countBackfillAuditRows(existingCycleId));
    }

    @Test
    public void backfill_isIdempotent() throws IOException {
        EQAProgram scheme = insertScheme("V1 absorption idempotence", EQASchemeType.INTER_LAB_SPLIT, "CPHL");
        insertLegacyDistribution(DRAFT_DIST, scheme, "Only run", "2025-06-10", "DRAFT");

        runBackfill();
        long cyclesAfterFirstRun = countCycles(scheme.getId());
        long auditRowsAfterFirstRun = countAllBackfillAuditRows();

        runBackfill();

        assertEquals(1, cyclesAfterFirstRun);
        assertEquals(cyclesAfterFirstRun, countCycles(scheme.getId()));
        assertEquals(auditRowsAfterFirstRun, countAllBackfillAuditRows());
    }

    @Test
    public void supersededLegacyMenuRowsAreOff_myProgramsStaysOn() {
        for (String elementId : new String[] { "menu_eqa_orders", "menu_eqa_distribution",
                "menu_eqa_mgmt_distributions" }) {
            assertFalse(elementId + " should be deactivated", menuIsActive(elementId));
        }
        assertTrue("menu_eqa_my_programs should stay active", menuIsActive("menu_eqa_my_programs"));
    }

    private void runBackfill() throws IOException {
        String sql = FileCopyUtils.copyToString(
                new InputStreamReader(new ClassPathResource(BACKFILL_SQL).getInputStream(), StandardCharsets.UTF_8));
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

    private void assertSyntheticCycle(long distId, Long schemeId, int expectedNumber, String expectedName,
            String distributionDate, String expectedStatus) {
        Map<String, Object> cycle = jdbc
                .queryForMap("SELECT c.id, c.scheme_id, c.cycle_number, c.cycle_name, c.planned_start_date,"
                        + " c.planned_end_date, c.status, c.created_by"
                        + " FROM clinlims.eqa_cycle c JOIN clinlims.eqa_distribution d ON d.cycle_id = c.id"
                        + " WHERE d.id = ?", distId);
        assertEquals(schemeId.longValue(), ((Number) cycle.get("scheme_id")).longValue());
        assertEquals(expectedNumber, ((Number) cycle.get("cycle_number")).intValue());
        assertEquals(expectedName, cycle.get("cycle_name"));
        assertEquals(expectedStatus, cycle.get("status"));
        assertEquals(Date.valueOf(distributionDate), cycle.get("planned_start_date"));
        assertEquals(Date.valueOf(distributionDate).toLocalDate().plusDays(30),
                ((Date) cycle.get("planned_end_date")).toLocalDate());
        assertEquals(ADMIN_USER_ID, ((Number) cycle.get("created_by")).longValue());

        long cycleId = ((Number) cycle.get("id")).longValue();
        List<Map<String, Object>> audit = jdbc.queryForList(
                "SELECT prior_state, new_state, state_machine, trigger_type, reason"
                        + " FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ? AND trigger_event = 'V1_BACKFILL'",
                cycleId);
        assertEquals(1, audit.size());
        assertNull(audit.get(0).get("prior_state"));
        assertEquals(expectedStatus, audit.get(0).get("new_state"));
        assertEquals("PROVIDER", audit.get(0).get("state_machine"));
        assertEquals("AUTO", audit.get(0).get("trigger_type"));
        assertEquals("Cycle created from legacy distribution " + distId, audit.get(0).get("reason"));
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
