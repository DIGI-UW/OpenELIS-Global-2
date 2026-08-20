package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQACycleService.PanelSampleRequest;
import org.openelisglobal.eqa.service.EQACycleService.ProviderCycleRequest;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQADistributionMethod;
import org.openelisglobal.eqa.valueholder.EQAPanelSourceType;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStorageTemp;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-613 [EQA V2.5 / T-24] — the five-step cycle wizard's single write
 * (FR-V2.5-02) against a real DB: one call leaves a cycle, its panel, its panel
 * samples and its participant roster in place and the cycle in prep, or it
 * leaves nothing at all.
 */
public class EQAProviderCycleWizardIntegrationTest extends EQASpineTestBase {

    private static final long ORG_A = 9970L;
    private static final long ORG_B = 9971L;
    private static final long ORG_UNENROLLED = 9972L;
    // The spine fixture seeds tests and analytes but no test_analyte link, so the
    // wizard's test -> analyte resolution has nothing to resolve until this suite
    // links them. TEST_NO_ANALYTE is deliberately left unlinked.
    private static final String TEST_HIV_VL = "9702";
    private static final String TEST_EID = "9703";
    private static final String TEST_NO_ANALYTE = "9704";
    private static final long ANALYTE_HIV_VL = 9802L;
    private static final long ANALYTE_EID = 9803L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQAShipmentService shipmentService;

    private EQAProgram scheme;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        seedOrganizations();
        linkTestAnalytes();
        scheme = insertScheme("Wizard scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
        enroll(ORG_A);
        enroll(ORG_B);
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id IN (9970, 9971, 9972)");
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE id IN (99801, 99802)");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.organization WHERE id IN ('9970', '9971', '9972')");
        }
    }

    @Test
    public void theWizardCreatesTheWholeCycleAndLeavesItInPrep() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A, ORG_B)), USER);

        assertNotNull(created.getId());
        assertEquals(EQACycleStatus.PREP_IN_PROGRESS, readBack(created.getId()).getStatus());
        assertEquals("2026 Round 4", created.getCycleName());
        assertEquals(Integer.valueOf(4), created.getCycleNumber());

        assertEquals("one panel bound to the cycle", Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel WHERE cycle_id = ?", Integer.class, created.getId()));
        assertEquals("two panel samples", Integer.valueOf(2),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_panel_sample s JOIN clinlims.eqa_panel p"
                        + " ON p.id = s.panel_id WHERE p.cycle_id = ?", Integer.class, created.getId()));
        assertEquals("two roster rows", Integer.valueOf(2),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_cycle_participant WHERE cycle_id = ?",
                        Integer.class, created.getId()));

        // Step 4's cold-chain choice lands on the panel, which is where the shipping
        // box reads its temperature requirement from.
        assertEquals("DRY_ICE", jdbc.queryForObject("SELECT storage_temp FROM clinlims.eqa_panel WHERE cycle_id = ?",
                String.class, created.getId()));
        assertEquals("holding material back is a prep-time decision", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT aliquots_reserved FROM clinlims.eqa_panel WHERE cycle_id = ?", Integer.class, created.getId()));

        List<EQACycleStateTransition> audit = cycleService.getTransitions(created.getId());
        EQACycleStateTransition last = audit.get(audit.size() - 1);
        assertEquals("PLANNED", last.getPriorState());
        assertEquals("PREP_IN_PROGRESS", last.getNewState());
        assertEquals("a person clicked the wizard, so the move is attributed to them", Long.valueOf(ADMIN_USER_ID),
                last.getTriggeredBy());
    }

    @Test
    public void theNewCycleIsImmediatelySizedByItsOwnRoster() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A)), USER);

        Map<String, Object> prep = shipmentService.getPrepStatus(created.getId());

        assertEquals("one of the two enrolled labs is on this cycle", 1, prep.get("participantCount"));
        Map<String, Object> panel = panels(prep).get(0);
        assertEquals("2 samples x 1 participant, nothing reserved yet", 2, panel.get("aliquotsNeeded"));
        assertEquals(1, shipmentService.getShipmentRows(created.getId()).size());
    }

    @Test
    public void cycleNumberDefaultsToTheNextOneTheSchemeHasNotUsed() {
        insertCycle(scheme, 11);

        EQACycle created = cycleService.createProviderCycle(withCycleNumber(null), USER);

        assertEquals(Integer.valueOf(12), created.getCycleNumber());
    }

    @Test
    public void aLabThatIsNotEnrolledCannotBeAddedToTheRoster() {
        try {
            cycleService.createProviderCycle(request(List.of(ORG_A, ORG_UNENROLLED)), USER);
            fail("the roster is a subset of the scheme's enrollment, not a second way to enroll");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("not an active participant"));
        }
        assertNothingWasCreated();
    }

    @Test
    public void aRepeatedSampleCodeIsRefusedByNameNotByConstraint() {
        List<PanelSampleRequest> samples = List.of(new PanelSampleRequest("PS-1", TEST_HIV_VL, null, null, null, null),
                new PanelSampleRequest("PS-1", TEST_EID, null, null, null, null));

        try {
            cycleService.createProviderCycle(with(samples, List.of(ORG_A)), USER);
            fail("uq_eqa_panel_sample_panel_code must be caught before it fires");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("PS-1"));
        }
        assertNothingWasCreated();
    }

    @Test
    public void anAcceptanceRangeThatRunsBackwardsIsRefused() {
        List<PanelSampleRequest> samples = List.of(new PanelSampleRequest("PS-1", TEST_HIV_VL, "1000", "cp/mL",
                new BigDecimal("900"), new BigDecimal("100")));

        try {
            cycleService.createProviderCycle(with(samples, List.of(ORG_A)), USER);
            fail("eqa_panel_sample_range_chk must be caught before it fires");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("backwards"));
        }
        assertNothingWasCreated();
    }

    /**
     * The wizard picks the orderable test; the analyte a target is stored against
     * is resolved server-side (T-21's rule, shared via
     * EQAPanelService.analyteIdForTest).
     */
    @Test
    public void theAnalyteBehindEachTestIsResolvedOntoTheSample() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A)), USER);

        assertEquals(Long.valueOf(ANALYTE_HIV_VL),
                jdbc.queryForObject("SELECT s.analyte_id FROM clinlims.eqa_panel_sample s"
                        + " JOIN clinlims.eqa_panel p ON p.id = s.panel_id"
                        + " WHERE p.cycle_id = ? AND s.sample_code = 'PS-1'", Long.class, created.getId()));
    }

    /**
     * Blind codes are the in-house blinding mechanism — at distribution each one
     * becomes a local order's accession number. Provider material is shipped
     * physically and identified by its sample code, so carrying an "IH-" code would
     * assert a blinding that never happened.
     */
    @Test
    public void providerPanelSamplesCarryNoBlindCode() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A)), USER);

        assertEquals("no provider sample may be given a blind code", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_panel_sample s" + " JOIN clinlims.eqa_panel p ON p.id = s.panel_id"
                        + " WHERE p.cycle_id = ? AND s.blind_code IS NOT NULL",
                Integer.class, created.getId()));
    }

    @Test
    public void aTestWithNoAnalyteBehindItIsRefused() {
        List<PanelSampleRequest> samples = List
                .of(new PanelSampleRequest("PS-1", TEST_NO_ANALYTE, null, null, null, null));

        try {
            cycleService.createProviderCycle(with(samples, List.of(ORG_A)), USER);
            fail("a panel target has nowhere to live without an analyte");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("no analyte"));
        }
        assertNothingWasCreated();
    }

    @Test
    public void aVendorSourcedPanelWithoutAVendorIsRefused() {
        ProviderCycleRequest request = new ProviderCycleRequest(scheme.getId(), 4, "2026 Round 4", null, null,
                "Vendor panel", EQAPanelSourceType.VENDOR_SOURCED, "LOT-1", null, null, null, twoSamples(),
                List.of(ORG_A), null, null, EQADistributionMethod.FHIR);

        try {
            cycleService.createProviderCycle(request, USER);
            fail("FR-V2.1-17: vendor-sourced material must carry the vendor's provenance");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("vendor"));
        }
        assertNothingWasCreated();
    }

    @Test
    public void aCycleWithNoParticipantIsRefused() {
        try {
            cycleService.createProviderCycle(request(List.of()), USER);
            fail("a cycle with no participant has nothing to ship to");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("participant"));
        }
        assertNothingWasCreated();
    }

    @Test
    public void aBlankTargetValueIsStoredAsNullRatherThanPlaintext() {
        List<PanelSampleRequest> samples = List
                .of(new PanelSampleRequest("PS-1", TEST_HIV_VL, "   ", "cp/mL", null, null));

        EQACycle created = cycleService.createProviderCycle(with(samples, List.of(ORG_A)), USER);

        // The encryption converter passes blanks through unencrypted and then throws
        // when they are read back, so a blank must never reach the column.
        assertNull(jdbc.queryForObject(
                "SELECT s.target_value FROM clinlims.eqa_panel_sample s"
                        + " JOIN clinlims.eqa_panel p ON p.id = s.panel_id WHERE p.cycle_id = ?",
                String.class, created.getId()));
    }

    // ---- fixture helpers ----

    @Test
    public void theWizardsDistributionMethodIsRecordedOnTheCycle() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A, ORG_B)), USER);

        assertEquals(EQADistributionMethod.MIXED, readBack(created.getId()).getDistributionMethod());
        assertEquals("MIXED", jdbc.queryForObject("SELECT distribution_method FROM clinlims.eqa_cycle WHERE id = ?",
                String.class, created.getId()));
        // Identity, not cardinality: a roster of the right size but the wrong labs
        // would ship panels to laboratories that never enrolled.
        assertEquals(List.of(ORG_A, ORG_B),
                jdbc.queryForList("SELECT organization_id FROM clinlims.eqa_cycle_participant"
                        + " WHERE cycle_id = ? ORDER BY organization_id", Long.class, created.getId()));
        assertEquals("the cold chain the panel step collects reaches the panel", java.sql.Date.valueOf("2027-01-31"),
                jdbc.queryForObject("SELECT expiration_date FROM clinlims.eqa_panel WHERE cycle_id = ?",
                        java.sql.Date.class, created.getId()));
    }

    @Test
    public void aCycleCreatedOutsideTheWizardHasNoDistributionMethod() {
        EQACycle created = cycleService.create(scheme.getId(), 9, "Legacy round", null, null, USER);

        assertNull("nothing may invent a method the provider never chose",
                readBack(created.getId()).getDistributionMethod());
    }

    @Test
    public void theDatabaseRefusesADistributionMethodOutsideTheEnum() {
        EQACycle created = cycleService.createProviderCycle(request(List.of(ORG_A)), USER);

        try {
            // Short enough to reach the CHECK: a longer value trips VARCHAR(10) first
            // and would pass this test for the wrong reason.
            jdbc.update("UPDATE clinlims.eqa_cycle SET distribution_method = 'PIGEON' WHERE id = ?", created.getId());
            fail("eqa_cycle_distribution_method_chk must reject values the enum does not carry");
        } catch (Exception expected) {
            assertConstraintViolation(expected, "eqa_cycle_distribution_method_chk");
        }
    }

    private ProviderCycleRequest request(List<Long> participants) {
        return with(twoSamples(), participants);
    }

    private ProviderCycleRequest withCycleNumber(Integer cycleNumber) {
        return new ProviderCycleRequest(scheme.getId(), cycleNumber, "2026 Round", null, null, "HIV VL panel",
                EQAPanelSourceType.IN_HOUSE_ALIQUOTED, "LOT-1", null, null, null, twoSamples(), List.of(ORG_A),
                EQAStorageTemp.DRY_ICE, null, EQADistributionMethod.FHIR);
    }

    private ProviderCycleRequest with(List<PanelSampleRequest> samples, List<Long> participants) {
        return new ProviderCycleRequest(scheme.getId(), 4, "2026 Round 4", Date.valueOf("2026-09-01"),
                Date.valueOf("2026-10-01"), "HIV VL panel", EQAPanelSourceType.IN_HOUSE_ALIQUOTED, "LOT-1", null, null,
                null, samples, participants, EQAStorageTemp.DRY_ICE, Date.valueOf("2027-01-31"),
                EQADistributionMethod.MIXED);
    }

    private List<PanelSampleRequest> twoSamples() {
        List<PanelSampleRequest> samples = new ArrayList<>();
        samples.add(new PanelSampleRequest("PS-1", TEST_HIV_VL, "1000", "cp/mL", new BigDecimal("500"),
                new BigDecimal("2000")));
        samples.add(new PanelSampleRequest("PS-2", TEST_EID, null, null, null, null));
        return samples;
    }

    /**
     * The wizard's all-or-nothing promise: a refusal must not leave a cycle behind
     * for the scheme list to show as a cycle with no panel.
     */
    private void assertNothingWasCreated() {
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_cycle WHERE scheme_id = ?", Integer.class, scheme.getId()));
    }

    private void linkTestAnalytes() {
        jdbc.update("DELETE FROM clinlims.test_analyte WHERE id IN (99801, 99802)");
        jdbc.update(
                "INSERT INTO clinlims.test_analyte (id, test_id, analyte_id, lastupdated)"
                        + " VALUES (99801, ?, ?, now()), (99802, ?, ?, now())",
                Long.valueOf(TEST_HIV_VL), ANALYTE_HIV_VL, Long.valueOf(TEST_EID), ANALYTE_EID);
    }

    private void seedOrganizations() {
        for (long id : new long[] { ORG_A, ORG_B, ORG_UNENROLLED }) {
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> panels(Map<String, Object> prep) {
        return (List<Map<String, Object>>) prep.get("panels");
    }
}
