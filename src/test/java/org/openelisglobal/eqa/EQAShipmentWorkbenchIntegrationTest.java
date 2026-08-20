package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAInvalidTransitionException;
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
 * OGC-613 [EQA V2.5 / T-25] — the provider prep and shipment workbenches
 * against a real DB: the FR-V2.5-12 inventory gate is enforced on the server,
 * and dispatch writes ordinary shipment rows (AC-V2.5-12) while moving the
 * cycle itself.
 */
public class EQAShipmentWorkbenchIntegrationTest extends EQASpineTestBase {

    private static final long ORG_A = 9960L;
    private static final long ORG_B = 9961L;
    private static final long ORG_C = 9962L;
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long ANALYTE_EID = 9803L;

    @Autowired
    private EQAShipmentService shipmentService;

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
        scheme = insertScheme("Provider scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
        cycle = readBack(insertCycle(scheme, 1));
        // Two participants, two samples per panel: FR-V2.5-12 needs 4 aliquots plus
        // whatever the panel holds back.
        enroll(ORG_A);
        enroll(ORG_B);
        panel = insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setPanelName("Provider panel");
        });
        insertPanelSample("PS-1", ANALYTE_HIV_VL);
        insertPanelSample("PS-2", ANALYTE_EID);
    }

    @Override
    protected void cleanEqaTables() {
        // shipping_box.eqa_cycle_id is RESTRICT, so boxes go before their cycle.
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.shipment WHERE shipping_box_id IN"
                    + " (SELECT id FROM clinlims.shipping_box WHERE box_id LIKE 'EQA-C%')");
            jdbc.update("DELETE FROM clinlims.shipping_box WHERE box_id LIKE 'EQA-C%'");
            jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id IN (9960, 9961, 9962)");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.organization WHERE id IN ('9960', '9961', '9962')");
        }
    }

    // ---- FR-V2.5-12: the prep gate ----

    @Test
    public void prepStatusReportsWhatTheGateRequires() {
        Map<String, Object> prep = shipmentService.getPrepStatus(cycle.getId());

        assertEquals(2, prep.get("participantCount"));
        Map<String, Object> panelRow = panels(prep).get(0);
        assertEquals("2 samples x 2 participants + 0 reserve", 4, panelRow.get("aliquotsNeeded"));
        assertEquals(4, panelRow.get("shortfall"));
        assertEquals(Boolean.FALSE, prep.get("readyToShipAllowed"));
        assertTrue(blockers(prep).toString().contains("homogeneity"));
    }

    @Test
    public void reserveAliquotsRaiseWhatIsNeeded() {
        shipmentService.savePrep(panel.getId(), 10, 3, true, "Passed", USER);

        Map<String, Object> panelRow = panels(shipmentService.getPrepStatus(cycle.getId())).get(0);
        assertEquals("4 for participants + 3 reserved", 7, panelRow.get("aliquotsNeeded"));
        assertEquals(0, panelRow.get("shortfall"));
    }

    @Test
    public void tooFewAliquotsBlockReadyToShipEvenWithQcPassed() {
        shipmentService.savePrep(panel.getId(), 3, 0, true, "Passed", USER);
        toPrepInProgress();

        Map<String, Object> prep = shipmentService.getPrepStatus(cycle.getId());
        assertEquals(Boolean.FALSE, prep.get("readyToShipAllowed"));
        assertTrue(blockers(prep).toString().contains("needs 4 aliquots"));

        try {
            readyToShip();
            fail("FR-V2.5-12: ready_to_ship must be refused while the panel is short of aliquots");
        } catch (EQAInvalidTransitionException expected) {
            // The refusal quotes the gate's own blocker, so the operator reads the same
            // sentence the workbench showed.
            assertTrue(expected.getMessage(), expected.getMessage().contains("needs 4 aliquots, has 3"));
        }
        assertEquals("the cycle must not have moved", EQACycleStatus.PREP_IN_PROGRESS,
                readBack(cycle.getId()).getStatus());
    }

    @Test
    public void enoughAliquotsAndPassedQcClearTheGate() {
        shipmentService.savePrep(panel.getId(), 4, 0, true, "Homogeneity CV 3%", USER);
        toPrepInProgress();

        assertEquals(Boolean.TRUE, shipmentService.getPrepStatus(cycle.getId()).get("readyToShipAllowed"));
        readyToShip();
        assertEquals(EQACycleStatus.READY_TO_SHIP, readBack(cycle.getId()).getStatus());
    }

    @Test
    public void aClearedCycleNoLongerOffersTheReadyToShipMove() {
        clearTheGate();

        // Gate arithmetic still satisfied, but PREP_IN_PROGRESS -> READY_TO_SHIP is no
        // longer a legal edge, so the workbench must not offer the button.
        Map<String, Object> prep = shipmentService.getPrepStatus(cycle.getId());
        assertTrue("nothing is outstanding", blockers(prep).isEmpty());
        assertEquals(Boolean.FALSE, prep.get("readyToShipAllowed"));
    }

    @Test
    public void theGateRefusesACycleWithNoPanelAtAll() {
        EQAProgram bare = insertScheme("Panel-less " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
        EQACycle bareCycle = readBack(insertCycle(bare, 1));

        Map<String, Object> prep = shipmentService.getPrepStatus(bareCycle.getId());

        assertTrue(blockers(prep).toString().contains("No panel has been prepared"));
        assertEquals(Boolean.FALSE, prep.get("readyToShipAllowed"));
    }

    @Test
    public void prepRefusesCountsThatBreakTheProducedInvariant() {
        try {
            shipmentService.savePrep(panel.getId(), 2, 5, true, null, USER);
            fail("produced must not fall below reserved + shipped");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("reserved"));
        }
        assertEquals("nothing may be persisted from a refused save", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT aliquots_produced FROM clinlims.eqa_panel WHERE id = ?", Integer.class, panel.getId()));
    }

    // ---- FR-V2.5-13: the shipment workbench ----

    @Test
    public void shipmentRowsCoverEveryActiveParticipantEvenBeforeAnyBoxExists() {
        List<Map<String, Object>> rows = shipmentService.getShipmentRows(cycle.getId());

        assertEquals(2, rows.size());
        assertNull("no box until details are saved", rows.get(0).get("boxId"));
        assertNotNull(rows.get(0).get("organizationName"));
    }

    @Test
    public void savingDetailsTwiceKeepsOneBoxPerParticipant() {
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-1", Date.valueOf("2026-09-01"), USER);
        Map<String, Object> second = shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "FedEx", "TRK-2",
                Date.valueOf("2026-09-02"), USER);

        assertEquals("one box per participant per cycle", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.shipping_box WHERE box_id = ?", Integer.class,
                        "EQA-C" + cycle.getId() + "-" + ORG_A));
        assertEquals("READY_TO_SEND", second.get("boxState"));
        assertEquals("FedEx", second.get("courier"));
        assertEquals("TRK-2", second.get("trackingNumber"));
        assertEquals("the box knows its cycle", Long.valueOf(cycle.getId()),
                jdbc.queryForObject("SELECT eqa_cycle_id FROM clinlims.shipping_box WHERE box_id = ?", Long.class,
                        "EQA-C" + cycle.getId() + "-" + ORG_A));
    }

    @Test
    public void aNonParticipantOrganizationCannotBeShippedTo() {
        try {
            shipmentService.saveShipmentDetails(cycle.getId(), 9999L, "DHL", "TRK-9", null, USER);
            fail("only participants of the cycle may receive a panel");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a participant of this cycle"));
        }
    }

    /**
     * T-24: once a cycle carries its own roster, that roster — not the scheme's
     * enrollment list — is what the cycle costs and who it ships to. This suite's
     * other cases exercise the opposite side of the same rule: they seed no roster
     * at all, so they run on the pre-qa/032 fallback.
     */
    @Test
    public void aCycleRosterOverridesTheSchemeEnrollmentEverywhere() {
        enroll(ORG_C);
        addToRoster(ORG_A);

        assertEquals("3 enrolled, 1 on this cycle's roster", 1,
                shipmentService.getPrepStatus(cycle.getId()).get("participantCount"));
        List<Map<String, Object>> rows = shipmentService.getShipmentRows(cycle.getId());
        assertEquals(1, rows.size());
        assertEquals(ORG_A, rows.get(0).get("organizationId"));

        try {
            shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, "DHL", "TRK-B", null, USER);
            fail("an enrolled lab that is not on this cycle's roster must not be shipped to");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not a participant of this cycle"));
        }
    }

    @Test
    public void firstDispatchShipsTheCycleAndConsumesAliquots() {
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", Date.valueOf("2026-09-01"), USER);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, "DHL", "TRK-B", Date.valueOf("2026-09-01"), USER);

        List<Map<String, Object>> shipped = shipmentService.markShipped(cycle.getId(), List.of(ORG_A, ORG_B), USER);

        assertEquals(2, shipped.size());
        assertEquals("SENT", shipped.get(0).get("boxState"));
        assertEquals("IN_TRANSIT", shipped.get(0).get("shipmentStatus"));
        assertNotNull("dispatch stamps the shipped date", shipped.get(0).get("shippedDate"));
        assertEquals("two shipment rows in the shared shipment table", Integer.valueOf(2),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.shipment s JOIN clinlims.shipping_box b"
                                + " ON b.id = s.shipping_box_id WHERE b.eqa_cycle_id = ?",
                        Integer.class, cycle.getId()));
        assertEquals("2 samples x 2 participants dispatched", Integer.valueOf(4), jdbc.queryForObject(
                "SELECT aliquots_shipped FROM clinlims.eqa_panel WHERE id = ?", Integer.class, panel.getId()));
        assertEquals("the dispatching user is recorded on the shipment", Integer.valueOf(USER),
                jdbc.queryForObject(
                        "SELECT s.sys_user_id FROM clinlims.shipment s JOIN clinlims.shipping_box b"
                                + " ON b.id = s.shipping_box_id WHERE b.eqa_cycle_id = ? LIMIT 1",
                        Integer.class, cycle.getId()));

        assertEquals(EQACycleStatus.SHIPPED, readBack(cycle.getId()).getStatus());
        List<EQACycleStateTransition> audit = cycleService.getTransitions(cycle.getId());
        EQACycleStateTransition last = audit.get(audit.size() - 1);
        assertEquals("SHIPPED", last.getNewState());
        assertEquals(EQATriggerEvent.FIRST_SHIPMENT_SENT, last.getTriggerEvent());
        assertEquals(EQATriggerType.AUTO, last.getTriggerType());
    }

    @Test
    public void aParticipantNamedTwiceInOneBatchDispatchesOnce() {
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);

        List<Map<String, Object>> shipped = shipmentService.markShipped(cycle.getId(), List.of(ORG_A, ORG_A), USER);

        assertEquals("one dispatch per participant, however often named", 1, shipped.size());
        assertEquals("2 samples for 1 participant", Integer.valueOf(2), jdbc.queryForObject(
                "SELECT aliquots_shipped FROM clinlims.eqa_panel WHERE id = ?", Integer.class, panel.getId()));
    }

    @Test
    public void aSecondDispatchDoesNotTransitionTheCycleAgain() {
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, "DHL", "TRK-B", null, USER);

        shipmentService.markShipped(cycle.getId(), List.of(ORG_A), USER);
        int auditAfterFirst = cycleService.getTransitions(cycle.getId()).size();
        shipmentService.markShipped(cycle.getId(), List.of(ORG_B), USER);

        assertEquals("the cycle ships once, however many participants follow", auditAfterFirst,
                cycleService.getTransitions(cycle.getId()).size());
        assertEquals(EQACycleStatus.SHIPPED, readBack(cycle.getId()).getStatus());
    }

    @Test
    public void dispatchIsRefusedWhenTheInventoryCannotCoverIt() {
        // 4 produced covers exactly two participants at 2 samples each; a third
        // dispatch would break produced >= reserved + shipped.
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, "DHL", "TRK-B", null, USER);
        shipmentService.markShipped(cycle.getId(), List.of(ORG_A, ORG_B), USER);
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES (9962, 'Participant lab 9962', 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING");
        enroll(9962L);
        shipmentService.saveShipmentDetails(cycle.getId(), 9962L, "DHL", "TRK-C", null, USER);

        try {
            shipmentService.markShipped(cycle.getId(), List.of(9962L), USER);
            fail("dispatching more material than was produced must be refused");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("does not hold enough aliquots"));
        }
        assertEquals("no aliquots may be consumed by a refused dispatch", Integer.valueOf(4), jdbc.queryForObject(
                "SELECT aliquots_shipped FROM clinlims.eqa_panel WHERE id = ?", Integer.class, panel.getId()));
    }

    @Test
    public void detailsOfADispatchedBoxCanNoLongerBeChanged() {
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);
        shipmentService.markShipped(cycle.getId(), List.of(ORG_A), USER);

        try {
            shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "FedEx", "TRK-Z", null, USER);
            fail("a box in transit is history, not a draft");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("can no longer be changed"));
        }
        assertEquals("DHL",
                jdbc.queryForObject(
                        "SELECT courier FROM clinlims.shipment s"
                                + " JOIN clinlims.shipping_box b ON b.id = s.shipping_box_id WHERE b.box_id = ?",
                        String.class, "EQA-C" + cycle.getId() + "-" + ORG_A));
    }

    @Test
    public void dispatchIsRefusedBeforeTheCycleIsClearedToShip() {
        toPrepInProgress();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);

        try {
            shipmentService.markShipped(cycle.getId(), List.of(ORG_A), USER);
            fail("a cycle still in prep cannot dispatch");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("not been cleared to ship"));
        }
        assertEquals("READY_TO_SEND", jdbc.queryForObject("SELECT state FROM clinlims.shipping_box WHERE box_id = ?",
                String.class, "EQA-C" + cycle.getId() + "-" + ORG_A));
    }

    @Test
    public void dispatchWithoutACourierIsRefusedForEveryParticipantInTheBatch() {
        clearTheGate();
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_A, "DHL", "TRK-A", null, USER);
        shipmentService.saveShipmentDetails(cycle.getId(), ORG_B, null, null, null, USER);

        try {
            shipmentService.markShipped(cycle.getId(), List.of(ORG_A, ORG_B), USER);
            fail("a batch with an incomplete participant must not half-ship");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("courier"));
        }
        assertEquals("the complete participant must not have shipped either", Integer.valueOf(0),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.shipping_box WHERE eqa_cycle_id = ?" + " AND state = 'SENT'",
                        Integer.class, cycle.getId()));
        assertEquals(EQACycleStatus.READY_TO_SHIP, readBack(cycle.getId()).getStatus());
    }

    // ---- FR-V2.5-01: the provider scheme list ----

    @Test
    public void providerSchemeListCarriesOnlySchemesThisLabProvides() {
        EQAProgram participantOnly = insertScheme("Externally provided " + System.nanoTime(),
                EQASchemeType.INTERNATIONAL_PT, "NHLS");
        insertCycle(participantOnly, 1);

        List<Map<String, Object>> schemes = shipmentService.getProviderSchemes();

        assertEquals("a scheme with no enrolled participant is one this lab only takes part in", 1, schemes.size());
        assertEquals(scheme.getId(), schemes.get(0).get("id"));
        assertEquals(2, schemes.get(0).get("enrolledParticipantCount"));

        List<Map<String, Object>> cycles = cycles(schemes.get(0));
        assertEquals(1, cycles.size());
        assertEquals(cycle.getId(), cycles.get(0).get("id"));
        assertEquals("no roster yet, so the scheme's enrollment stands in", 2, cycles.get(0).get("participantCount"));
        assertEquals(1, cycles.get(0).get("panelCount"));
    }

    @Test
    public void providerSchemeListCountsTheCycleRosterWhenThereIsOne() {
        addToRoster(ORG_A);

        List<Map<String, Object>> cycles = cycles(shipmentService.getProviderSchemes().get(0));

        assertEquals("the list must not disagree with the prep gate", 1, cycles.get(0).get("participantCount"));
    }

    @Test
    public void providerSchemeListOrdersCyclesNewestFirst() {
        insertCycle(scheme, 7);
        insertCycle(scheme, 3);

        List<Map<String, Object>> cycles = cycles(shipmentService.getProviderSchemes().get(0));

        assertEquals(3, cycles.size());
        assertEquals(7, cycles.get(0).get("cycleNumber"));
        assertEquals(3, cycles.get(1).get("cycleNumber"));
        assertEquals(1, cycles.get(2).get("cycleNumber"));
    }

    // ---- fixture helpers ----

    private void seedOrganizations() {
        for (long id : new long[] { ORG_A, ORG_B, ORG_C }) {
            jdbc.update(
                    "INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                            + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING",
                    id, "Participant lab " + id);
        }
    }

    private void enroll(long organizationId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_program_enrollment (id, eqa_program_id, organization_id,"
                        + " enrollment_date, status, sys_user_id, lastupdated)"
                        + " VALUES (nextval('clinlims.eqa_enrollment_seq'), ?, ?, now(), 'Active', ?, now())",
                scheme.getId(), organizationId, USER);
    }

    private void addToRoster(long organizationId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_cycle_participant (id, cycle_id, organization_id, sys_user_id)"
                        + " VALUES (nextval('clinlims.eqa_cycle_participant_seq'), ?, ?, ?)",
                cycle.getId(), organizationId, USER);
    }

    private void insertPanelSample(String sampleCode, long analyteId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_panel_sample (id, panel_id, sample_code, analyte_id, sys_user_id)"
                        + " VALUES (nextval('clinlims.eqa_panel_sample_seq'), ?, ?, ?, ?)",
                panel.getId(), sampleCode, analyteId, USER);
    }

    private void toPrepInProgress() {
        cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
    }

    private void readyToShip() {
        cycleService.transition(cycle.getId(), EQACycleStatus.READY_TO_SHIP, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.HOMOGENEITY_QC_PASSED, null, null, USER);
    }

    private void clearTheGate() {
        shipmentService.savePrep(panel.getId(), 4, 0, true, "Homogeneity CV 3%", USER);
        toPrepInProgress();
        readyToShip();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> panels(Map<String, Object> prep) {
        return (List<Map<String, Object>>) prep.get("panels");
    }

    @SuppressWarnings("unchecked")
    private List<String> blockers(Map<String, Object> prep) {
        return (List<String>) prep.get("blockers");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cycles(Map<String, Object> scheme) {
        return (List<Map<String, Object>>) scheme.get("cycles");
    }
}
