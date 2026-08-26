package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-613 [EQA V2.5 / T-26] — receipt monitoring (FR-V2.5-14), reprovisioning
 * (FR-V2.5-15) and provider scoring (FR-V2.5-03/04) against a real DB: the
 * overdue rule, the inventory arithmetic a repeat consumes, the automatic move
 * to submissions_open, and the bridge from the V2 cycle to the distribution its
 * participants' results hang off.
 */
public class EQAProviderCycleOversightIntegrationTest extends EQASpineTestBase {

    private static final long ORG_A = 9970L;
    private static final long ORG_B = 9971L;
    /** Twelve peers so one outlier can exceed |Z| = 3 with a sample SD. */
    private static final long FIRST_SCORING_ORG = 9975L;
    private static final int SCORING_ORGS = 12;
    private static final long TEST_ID = 9977L;
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long ANALYTE_EID = 9803L;

    @Autowired
    private EQAShipmentService shipmentService;

    @Autowired
    private EQAProviderScoringService scoringService;

    @Autowired
    private EQACycleService cycleService;

    private EQAProgram scheme;
    private EQACycle cycle;
    private EQAPanel panel;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        seedOrganizations();
        seedTest();
        scheme = insertScheme("Oversight scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
        cycle = readBack(insertCycle(scheme, 1));
        enroll(ORG_A);
        enroll(ORG_B);
        panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Oversight panel");
        });
        insertPanelSample("OS-1", ANALYTE_HIV_VL);
        insertPanelSample("OS-2", ANALYTE_EID);
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution");
            // shipping_box.eqa_cycle_id is RESTRICT, so boxes go before their cycle.
            jdbc.update("DELETE FROM clinlims.shipment WHERE shipping_box_id IN"
                    + " (SELECT id FROM clinlims.shipping_box WHERE box_id LIKE 'EQA-C%')");
            jdbc.update("DELETE FROM clinlims.box_sample_item WHERE shipping_box_id IN"
                    + " (SELECT id FROM clinlims.shipping_box WHERE box_id LIKE 'EQA-C%')");
            jdbc.update("DELETE FROM clinlims.shipping_box WHERE box_id LIKE 'EQA-C%'");
            jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id BETWEEN 9970 AND 9990");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.organization WHERE CAST(id AS numeric) BETWEEN 9970 AND 9990");
        }
    }

    // ---- FR-V2.5-14: the overdue rule ----

    @Test
    public void aShipmentIsOverdueOnlyTwoBusinessDaysAfterTheExpectedDelivery() {
        dispatchBoth(minusBusinessDays(LocalDate.now(), 2));
        Map<String, Object> onTheBoundary = receiptRow(ORG_A);
        assertEquals("two business days of grace are not yet overdue", Boolean.FALSE, onTheBoundary.get("overdue"));
        assertEquals("IN_TRANSIT", onTheBoundary.get("receiptStatus"));

        // One business day earlier — which is more than one calendar day earlier
        // whenever the window spans a weekend, and that is the whole point of the
        // rule: a Friday delivery is not chased on Sunday. Written straight to the
        // row because a dispatched box no longer accepts edited courier details.
        jdbc.update("UPDATE clinlims.shipment SET estimated_delivery_date = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(minusBusinessDays(LocalDate.now(), 3).atStartOfDay()), shipmentId(ORG_A));
        Map<String, Object> pastTheGrace = receiptRow(ORG_A);
        assertEquals("three business days past the estimate is overdue", Boolean.TRUE, pastTheGrace.get("overdue"));
        assertEquals("OVERDUE", pastTheGrace.get("receiptStatus"));
    }

    @Test
    public void aShipmentWithNoExpectedDateIsNeverOverdue() {
        dispatchBoth(null);

        Map<String, Object> row = receiptRow(ORG_A);
        assertEquals(Boolean.FALSE, row.get("overdue"));
        assertEquals("IN_TRANSIT", row.get("receiptStatus"));
    }

    @Test
    public void receiptRowsCoverEveryParticipantEvenBeforeAnythingShips() {
        List<Map<String, Object>> rows = shipmentService.getReceiptRows(cycle.getId());

        assertEquals(2, rows.size());
        assertEquals("NOT_SHIPPED", rows.get(0).get("receiptStatus"));
        assertNull(rows.get(0).get("receivedDate"));
    }

    // ---- FR-V2.5-14 / AC-V2.5-13: delivery opens submissions ----

    @Test
    public void submissionsOpenOnlyWhenEveryParticipantHoldsItsPanel() {
        dispatchBoth(LocalDate.now());

        shipmentService.markDelivered(cycle.getId(), ORG_A, USER);
        assertEquals("one delivery of two leaves the cycle shipped", EQACycleStatus.SHIPPED,
                readBack(cycle.getId()).getStatus());

        Map<String, Object> second = shipmentService.markDelivered(cycle.getId(), ORG_B, USER);

        assertEquals("DELIVERED", second.get("receiptStatus"));
        assertNotNull("the delivery date is stamped", second.get("receivedDate"));
        assertEquals("RECEIVED", second.get("boxState"));
        assertEquals(EQACycleStatus.SUBMISSIONS_OPEN, readBack(cycle.getId()).getStatus());

        List<String> states = cycleService.getTransitions(cycle.getId()).stream()
                .filter(t -> t.getTriggerEvent() == EQATriggerEvent.ALL_SHIPMENTS_DELIVERED)
                .map(EQACycleStateTransition::getNewState).toList();
        assertEquals("each edge keeps its own audit row", List.of("DELIVERED", "SUBMISSIONS_OPEN"), states);
    }

    @Test
    public void recordingADeliveryTwiceChangesNothing() {
        dispatchBoth(null);
        shipmentService.markDelivered(cycle.getId(), ORG_A, USER);
        int auditRows = cycleService.getTransitions(cycle.getId()).size();

        Map<String, Object> repeated = shipmentService.markDelivered(cycle.getId(), ORG_A, USER);

        assertEquals("DELIVERED", repeated.get("receiptStatus"));
        assertEquals(auditRows, cycleService.getTransitions(cycle.getId()).size());
    }

    @Test
    public void aDeliveryCannotBeRecordedBeforeAnythingWasSent() {
        try {
            shipmentService.markDelivered(cycle.getId(), ORG_A, USER);
            fail("nothing has been dispatched, so nothing can have arrived");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("No shipment has been dispatched"));
        }
    }

    // ---- FR-V2.5-15: reprovisioning ----

    @Test
    public void aRepeatComesOutOfTheReserveAndRecordsWhatItReplaces() {
        // 2 samples x 2 participants + 2 reserved = 6 produced clears the gate.
        prepAndDispatch(6, 2, null);
        Integer originalShipment = shipmentId(ORG_A);

        Map<String, Object> repeat = shipmentService.sendRepeat(cycle.getId(), ORG_A, null, USER);

        assertEquals("EQA-C" + cycle.getId() + "-" + ORG_A + "-R1", repeat.get("boxCode"));
        assertEquals("the repeat records the shipment it replaces", originalShipment, repeat.get("repeatOfShipmentId"));
        assertEquals("IN_TRANSIT", repeat.get("shipmentStatus"));
        assertEquals("the reserve paid for the repeat", Integer.valueOf(0), aliquots("aliquots_reserved"));
        assertEquals("2 samples for the original 2 participants plus 2 for the repeat", Integer.valueOf(6),
                aliquots("aliquots_shipped"));
        assertEquals("the monitor now follows the repeat", repeat.get("boxCode"), receiptRow(ORG_A).get("boxCode"));
        // T-40: a repeat box is packed like any other, so it does not go out empty.
        assertEquals("the repeat box holds the panel material it replaces", Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.box_sample_item bsi"
                                + " JOIN clinlims.shipping_box b ON b.id = bsi.shipping_box_id"
                                + " WHERE b.box_id = ? AND bsi.eqa_panel_sample_id IS NOT NULL",
                        Integer.class, repeat.get("boxCode")));
    }

    @Test
    public void aRepeatBeyondTheReserveNeedsAWrittenOverride() {
        // Reserve holds 1 of the 2 aliquots a repeat needs, with headroom for the rest.
        prepAndDispatch(8, 1, null);

        try {
            shipmentService.sendRepeat(cycle.getId(), ORG_A, null, USER);
            fail("dipping into unreserved material takes a justification");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("override note"));
        }
        assertEquals("a refused repeat consumes nothing", Integer.valueOf(1), aliquots("aliquots_reserved"));
        assertEquals(Integer.valueOf(4), aliquots("aliquots_shipped"));

        shipmentService.sendRepeat(cycle.getId(), ORG_A, "Courier lost the box; replacement authorised", USER);

        assertEquals(Integer.valueOf(0), aliquots("aliquots_reserved"));
        assertEquals(Integer.valueOf(6), aliquots("aliquots_shipped"));
    }

    @Test
    public void aRepeatTheInventoryCannotCoverIsRefusedEvenWithAnOverride() {
        // Exactly enough for the two dispatches and nothing more.
        prepAndDispatch(4, 0, null);

        try {
            shipmentService.sendRepeat(cycle.getId(), ORG_A, "Authorised by the scheme manager", USER);
            fail("a repeat cannot be sent from material that does not exist");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no aliquots left"));
        }
        assertEquals(Integer.valueOf(4), aliquots("aliquots_shipped"));
    }

    @Test
    public void thereIsNothingToRepeatBeforeAnythingWasDispatched() {
        try {
            shipmentService.sendRepeat(cycle.getId(), ORG_A, null, USER);
            fail("a repeat replaces a shipment, so one has to exist");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Nothing has been dispatched"));
        }
    }

    // ---- FR-V2.5-03/04: scoring and score return ----

    @Test
    public void scoringWritesZScoresAdvancesTheCycleAndRegistersTheOutlier() {
        toSubmissionsOpen();
        seedPeerResults();

        Map<String, Object> summary = scoringService.scoreCycle(cycle.getId(), USER);

        assertEquals(SCORING_ORGS, summary.get("scoredCount"));
        assertEquals("only the outlier is followed up", 1, summary.get("followupCount"));
        assertEquals("SCORED", summary.get("cycleStatus"));
        assertEquals(EQACycleStatus.SCORED, readBack(cycle.getId()).getStatus());

        Long outlier = FIRST_SCORING_ORG + SCORING_ORGS - 1;
        assertEquals("UNACCEPTABLE", verdictOf(outlier));
        assertEquals("ACCEPTABLE", verdictOf(FIRST_SCORING_ORG));
        assertTrue("the outlier's Z is past the unacceptable threshold",
                zScoreOf(outlier).abs().compareTo(new BigDecimal("3")) > 0);

        // Read from the table rather than through the register: this test owns the
        // write, and T-27's register owns how it is read back.
        assertEquals("one register row, for the outlier", Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_participant_followup WHERE participant_org_id = ?",
                        Integer.class, outlier));
        assertEquals("one failing cycle is not yet persistent", Boolean.FALSE,
                jdbc.queryForObject("SELECT persistent_failure_flag FROM clinlims.eqa_participant_followup"
                        + " WHERE participant_org_id = ?", Boolean.class, outlier));
    }

    @Test
    public void aCycleWithTooFewResultsIsRefusedRatherThanHalfScored() {
        toSubmissionsOpen();
        Long distributionId = insertDistribution();
        insertResult(distributionId, FIRST_SCORING_ORG, new BigDecimal("100"));

        try {
            scoringService.scoreCycle(cycle.getId(), USER);
            fail("peer statistics over one result describe nothing");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("at least 5"));
        }
        assertEquals("a refused scoring run leaves the cycle where it was", EQACycleStatus.SUBMISSIONS_OPEN,
                readBack(cycle.getId()).getStatus());
    }

    @Test
    public void aCycleNotOpenForSubmissionsCannotBeScored() {
        try {
            scoringService.scoreCycle(cycle.getId(), USER);
            fail("a planned cycle has nothing to score");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not open for scoring"));
        }
    }

    @Test
    public void theScoreCsvCarriesOneParticipantAndNoOther() {
        toSubmissionsOpen();
        seedPeerResults();
        scoringService.scoreCycle(cycle.getId(), USER);

        String csv = scoringService.buildScoreCsv(cycle.getId(), FIRST_SCORING_ORG);

        assertEquals("header plus this participant's single result", 2, csv.trim().split("\n").length);
        assertTrue(csv, csv.contains("ACCEPTABLE"));
        assertFalse("another participant's verdict must not leak", csv.contains("UNACCEPTABLE"));
        // A negative Z prints as a number, not as a quoted formula-safe string.
        assertFalse("a decimal must not be escaped as text", csv.contains("'-"));
        assertTrue("the peer Z is a plain decimal", csv.contains(",-0.2"));
    }

    @Test
    public void scoreRowsAreEmptyUntilResultsArrive() {
        assertTrue(scoringService.getScoreRows(cycle.getId()).isEmpty());
    }

    // ---- fixture helpers ----

    private void seedOrganizations() {
        for (long id = ORG_A; id < FIRST_SCORING_ORG + SCORING_ORGS; id++) {
            jdbc.update(
                    "INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                            + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING",
                    id, "Participant lab " + id);
        }
    }

    private void seedTest() {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, 'EQA Oversight CD4', 'EQA Oversight CD4', 'Y', ?, now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_ID, UUID.randomUUID().toString(), TEST_ID);
    }

    private void enroll(long organizationId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_program_enrollment (id, eqa_program_id, organization_id,"
                        + " enrollment_date, status, sys_user_id, lastupdated)"
                        + " VALUES (nextval('clinlims.eqa_enrollment_seq'), ?, ?, now(), 'Active', ?, now())",
                scheme.getId(), organizationId, USER);
    }

    private void insertPanelSample(String sampleCode, long analyteId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_panel_sample (id, panel_id, sample_code, analyte_id, sys_user_id)"
                        + " VALUES (nextval('clinlims.eqa_panel_sample_seq'), ?, ?, ?, ?)",
                panel.getId(), sampleCode, analyteId, USER);
    }

    /** Prep, clear the gate, record couriers and dispatch both participants. */
    private void prepAndDispatch(int produced, int reserved, LocalDate expectedDelivery) {
        shipmentService.savePrep(panel.getId(), produced, reserved, true, "Homogeneity CV 3%", USER);
        cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
        cycleService.transition(cycle.getId(), EQACycleStatus.READY_TO_SHIP, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.HOMOGENEITY_QC_PASSED, null, null, USER);
        java.sql.Date estimate = expectedDelivery == null ? null : java.sql.Date.valueOf(expectedDelivery);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", estimate, USER);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, "DHL", "TRK-B", estimate, USER);
        shipmentService.markShipped(cycle.getId(), List.of(ORG_A, ORG_B), USER);
    }

    private void dispatchBoth(LocalDate expectedDelivery) {
        prepAndDispatch(4, 0, expectedDelivery);
    }

    /** Straight to submissions_open, the state scoring starts from. */
    private void toSubmissionsOpen() {
        dispatchBoth(null);
        shipmentService.markDelivered(cycle.getId(), ORG_A, USER);
        shipmentService.markDelivered(cycle.getId(), ORG_B, USER);
    }

    /**
     * Eleven peers around 100 and one at 400: mean 125, sample SD 86.6, so the
     * outlier's Z is 3.17 — unacceptable — and every peer sits well inside 2.
     */
    private void seedPeerResults() {
        Long distributionId = insertDistribution();
        for (int i = 0; i < SCORING_ORGS; i++) {
            insertResult(distributionId, FIRST_SCORING_ORG + i, new BigDecimal(i == SCORING_ORGS - 1 ? "400" : "100"));
        }
    }

    private Long insertDistribution() {
        jdbc.update(
                "INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                        + " distribution_date, deadline, status, created_by, cycle_id, sys_user_id)"
                        + " VALUES (nextval('clinlims.eqa_distribution_seq'), ?, ?, 'Oversight round', now(), now(),"
                        + " 'SHIPPED', ?, ?, ?)",
                UUID.randomUUID(), scheme.getId(), ADMIN_USER_ID, cycle.getId(), USER);
        return jdbc.queryForObject("SELECT id FROM clinlims.eqa_distribution WHERE cycle_id = ?", Long.class,
                cycle.getId());
    }

    private void insertResult(Long distributionId, Long organizationId, BigDecimal value) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_result (id, fhir_uuid, eqa_distribution_id, participant_organization_id,"
                        + " test_id, result_value, submission_method, submission_date, is_late_submission,"
                        + " sys_user_id) VALUES (nextval('clinlims.eqa_result_seq'), ?, ?, ?, ?, ?,"
                        + " 'MANUAL', now(), false, ?)",
                UUID.randomUUID(), distributionId, organizationId, TEST_ID, value, USER);
    }

    private Map<String, Object> receiptRow(long organizationId) {
        return shipmentService.getReceiptRows(cycle.getId()).stream()
                .filter(row -> Long.valueOf(organizationId).equals(row.get("organizationId"))).findFirst()
                .orElseThrow(AssertionError::new);
    }

    private Integer shipmentId(long organizationId) {
        return (Integer) receiptRow(organizationId).get("shipmentId");
    }

    private Integer aliquots(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM clinlims.eqa_panel WHERE id = ?", Integer.class,
                panel.getId());
    }

    private String verdictOf(Long organizationId) {
        return jdbc.queryForObject(
                "SELECT performance_status FROM clinlims.eqa_result WHERE participant_organization_id = ?",
                String.class, organizationId);
    }

    private BigDecimal zScoreOf(Long organizationId) {
        return jdbc.queryForObject("SELECT z_score FROM clinlims.eqa_result WHERE participant_organization_id = ?",
                BigDecimal.class, organizationId);
    }

    /** The same weekend skip the monitor applies, walked backwards. */
    private static LocalDate minusBusinessDays(LocalDate from, int days) {
        LocalDate date = from;
        for (int walked = 0; walked < days;) {
            date = date.minusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                walked++;
            }
        }
        return date;
    }
}
