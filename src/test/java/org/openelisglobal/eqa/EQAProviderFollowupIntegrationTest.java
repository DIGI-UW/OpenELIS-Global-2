package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAParticipantFollowupService;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAFollowupStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-613 [EQA V2.5 / T-27] — the provider-side participant follow-up register:
 * which register a row belongs to, the triage moves FR-V2.5-06 allows, the
 * persistent-failure rule (FR-V2.5-07), and the guarantee that another
 * laboratory's failure never becomes a non-conformity in this one (AC-V2.5-10).
 */
public class EQAProviderFollowupIntegrationTest extends EQASpineTestBase {

    private static final long PARTICIPANT_ORG = 9992L;
    private static final long TEST_ID = 9993L;
    private static final long ANALYTE = 9994L;
    private static final long ENROLLMENT = 9930L;
    private static final int PEERS = 12;
    private static final long FIRST_PEER_ORG = 9995L;

    @Autowired
    private EQAParticipantFollowupService followupService;

    @Autowired
    private EQAProviderScoringService scoringService;

    @Autowired
    private EqaScoreNceService eqaScoreNceService;

    private EQAProgram scheme;

    @Before
    public void seedFixture() {
        seedOrganization(PARTICIPANT_ORG, "Participant lab " + PARTICIPANT_ORG);
        for (int i = 0; i < PEERS; i++) {
            seedOrganization(FIRST_PEER_ORG + i, "Peer lab " + (FIRST_PEER_ORG + i));
        }
        seedAnalyte();
        seedTest();
        seedEnrollment(ENROLLMENT, "EQA Provider Followup Programme");
        scheme = insertScheme("Provider followup scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "This lab");
    }

    @Override
    protected void cleanEqaTables() {
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.eqa_result");
            jdbc.update("DELETE FROM clinlims.eqa_distribution");
            jdbc.update("DELETE FROM clinlims.eqa_program_enrollment WHERE organization_id BETWEEN 9990 AND 9999");
        }
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.organization WHERE CAST(id AS numeric) BETWEEN 9990 AND 9999");
            jdbc.update("DELETE FROM clinlims.organization WHERE name = 'This laboratory'");
        }
    }

    // ---- which register a row belongs to ----

    @Test
    public void aRowAboutAnotherLaboratoryStaysOutOfThisLabsQueue() {
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        followupService.enqueueForOrganization(scheme, cycle, PARTICIPANT_ORG, unacceptableRows(), false, USER);

        assertEquals("the participant queue holds only this lab's own items", 0, followupService.getQueueRows().size());
        List<Map<String, Object>> register = followupService.getProviderRegisterRows();
        assertEquals(1, register.size());
        assertEquals(Long.valueOf(PARTICIPANT_ORG), register.get(0).get("participantOrgId"));
        assertEquals("Participant lab " + PARTICIPANT_ORG, register.get(0).get("organizationName"));
        assertEquals("NOTIFIED", register.get(0).get("followupStatus"));
    }

    @Test
    public void thisLabsOwnRowStaysOutOfTheProviderRegister() {
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        followupService.enqueueForThisLab(scheme, cycle, unacceptableRows(), USER);

        assertEquals(1, followupService.getQueueRows().size());
        assertEquals("this lab is not one of its own participants", 0,
                followupService.getProviderRegisterRows().size());
    }

    // ---- FR-V2.5-06: triage ----

    @Test
    public void triageWalksTheRegisterRowThroughItsLifecycle() {
        Long followupId = registerRow(false);

        assertEquals(EQAFollowupStatus.RESPONSE_RECEIVED, followupService
                .transitionStatus(followupId, EQAFollowupStatus.RESPONSE_RECEIVED, null, USER).getFollowupStatus());
        assertEquals(EQAFollowupStatus.UNDER_INVESTIGATION, followupService
                .transitionStatus(followupId, EQAFollowupStatus.UNDER_INVESTIGATION, null, USER).getFollowupStatus());
        assertEquals(EQAFollowupStatus.RESOLVED,
                followupService
                        .transitionStatus(followupId, EQAFollowupStatus.RESOLVED, "Recalibrated and re-tested", USER)
                        .getFollowupStatus());
        assertEquals("Recalibrated and re-tested", followupService.get(followupId).getResolutionNotes());
    }

    @Test
    public void aClosedRowCannotBeTriagedFurther() {
        Long followupId = registerRow(false);
        followupService.transitionStatus(followupId, EQAFollowupStatus.RESOLVED, "Closed", USER);

        try {
            followupService.transitionStatus(followupId, EQAFollowupStatus.UNDER_INVESTIGATION, null, USER);
            fail("a resolved follow-up is terminal");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("from RESOLVED to UNDER_INVESTIGATION"));
        }
    }

    @Test
    public void removalFromTheProgrammeWithdrawsTheEnrollment() {
        Long enrollmentId = enroll(PARTICIPANT_ORG);
        Long followupId = registerRow(false);

        followupService.transitionStatus(followupId, EQAFollowupStatus.REMOVED_FROM_PROGRAM,
                "Three consecutive failures", USER);

        assertEquals(EQAFollowupStatus.REMOVED_FROM_PROGRAM, followupService.get(followupId).getFollowupStatus());
        assertEquals("Withdrawn", jdbc.queryForObject("SELECT status FROM clinlims.eqa_program_enrollment WHERE id = ?",
                String.class, enrollmentId));
        assertEquals("Three consecutive failures",
                jdbc.queryForObject("SELECT withdrawal_reason FROM clinlims.eqa_program_enrollment WHERE id = ?",
                        String.class, enrollmentId));
    }

    // ---- FR-V2.5-08: notification ----

    @Test
    public void notifyingWithoutAContactEmailAsksTheReviewerToSendItByHand() {
        Long followupId = registerRow(false);

        Map<String, Object> outcome = followupService.notifyParticipant(followupId, USER);

        assertEquals(Boolean.FALSE, outcome.get("emailed"));
        assertTrue(String.valueOf(outcome.get("message")).contains("unacceptable"));
        assertTrue("the notification is still recorded against the row",
                followupService.get(followupId).getNotifiedAt() != null);
    }

    // ---- FR-V2.5-07: persistent failure ----

    @Test
    public void unacceptableInTwoOfTheLastThreeCyclesEscalatesOnItsOwn() {
        // Two earlier cycles this participant already failed, then a third scored now.
        failedCycle(1);
        failedCycle(2);
        EQACycle current = scoredCycle(3);

        scoringService.scoreCycle(current.getId(), USER);

        Map<String, Object> row = followupService.getProviderRegisterRows().stream()
                .filter(r -> Long.valueOf(PARTICIPANT_ORG).equals(r.get("participantOrgId"))).findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals(Boolean.TRUE, row.get("persistentFailureFlag"));
        assertEquals("a persistent failure escalates without waiting for a reviewer", "ESCALATED",
                row.get("followupStatus"));
    }

    @Test
    public void aSingleFailureAmongThreeCyclesIsNotPersistent() {
        passedCycle(1);
        passedCycle(2);
        EQACycle current = scoredCycle(3);

        scoringService.scoreCycle(current.getId(), USER);

        Map<String, Object> row = followupService.getProviderRegisterRows().stream()
                .filter(r -> Long.valueOf(PARTICIPANT_ORG).equals(r.get("participantOrgId"))).findFirst()
                .orElseThrow(AssertionError::new);
        assertFalse(Boolean.TRUE.equals(row.get("persistentFailureFlag")));
        assertEquals("NOTIFIED", row.get("followupStatus"));
    }

    // ---- AC-V2.5-10: never a local non-conformity ----

    @Test
    public void aProviderRowCannotBeEscalatedIntoALocalNonConformity() {
        Long followupId = registerRow(false);

        try {
            eqaScoreNceService.escalateFollowup(followupId, USER);
            fail("another laboratory's failure must not open a non-conformity here");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("provider follow-up register"));
        }
        assertEquals("nothing may be written by a refused escalation", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.nc_event WHERE trigger_source_type = ?",
                        Integer.class, EqaScoreNceService.TRIGGER_SOURCE_EQA_FOLLOWUP));
    }

    // ---- fixture helpers ----

    private Long registerRow(boolean persistentFailure) {
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        return followupService
                .enqueueForOrganization(scheme, cycle, PARTICIPANT_ORG, unacceptableRows(), persistentFailure, USER)
                .getId();
    }

    private List<Map<String, Object>> unacceptableRows() {
        return List.of(Map.of("testId", TEST_ID, "testName", "EQA Followup CD4", "reported", "400", "target", "100",
                "performanceStatus", "UNACCEPTABLE"));
    }

    /** A past cycle whose result for this participant was unacceptable. */
    private void failedCycle(int cycleNumber) {
        pastCycle(cycleNumber, "UNACCEPTABLE");
    }

    private void passedCycle(int cycleNumber) {
        pastCycle(cycleNumber, "ACCEPTABLE");
    }

    private void pastCycle(int cycleNumber, String verdict) {
        EQACycle cycle = readBack(insertCycle(scheme, cycleNumber));
        Long distributionId = insertDistribution(cycle);
        insertResult(distributionId, PARTICIPANT_ORG, new BigDecimal("400"));
        jdbc.update("UPDATE clinlims.eqa_result SET performance_status = ? WHERE eqa_distribution_id = ?", verdict,
                distributionId);
    }

    /**
     * A cycle open for submissions carrying twelve peer results: eleven around 100
     * and this participant at 400, which scores |Z| &gt; 3 against the peer group.
     */
    private EQACycle scoredCycle(int cycleNumber) {
        EQACycle cycle = readBack(insertCycle(scheme, cycleNumber));
        // Straight to the state scoring starts from: the walk through prep and
        // dispatch has its own gate, and it is T-26's test that owns it.
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = ? WHERE id = ?", EQACycleStatus.SUBMISSIONS_OPEN.name(),
                cycle.getId());
        Long distributionId = insertDistribution(readBack(cycle.getId()));
        insertResult(distributionId, PARTICIPANT_ORG, new BigDecimal("400"));
        for (int i = 0; i < PEERS - 1; i++) {
            insertResult(distributionId, FIRST_PEER_ORG + i, new BigDecimal("100"));
        }
        return cycle;
    }

    private Long insertDistribution(EQACycle cycle) {
        jdbc.update("INSERT INTO clinlims.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,"
                + " distribution_date, deadline, status, created_by, cycle_id, sys_user_id)"
                + " VALUES (nextval('clinlims.eqa_distribution_seq'), ?, ?, ?, now(), now(), 'SHIPPED'," + " ?, ?, ?)",
                UUID.randomUUID(), scheme.getId(), "Round " + cycle.getCycleNumber(), ADMIN_USER_ID, cycle.getId(),
                USER);
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

    private Long enroll(long organizationId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_program_enrollment (id, eqa_program_id, organization_id,"
                        + " enrollment_date, status, sys_user_id, lastupdated)"
                        + " VALUES (nextval('clinlims.eqa_enrollment_seq'), ?, ?, now(), 'Active', ?, now())",
                scheme.getId(), organizationId, USER);
        return jdbc.queryForObject(
                "SELECT id FROM clinlims.eqa_program_enrollment WHERE eqa_program_id = ?" + " AND organization_id = ?",
                Long.class, scheme.getId(), organizationId);
    }

    private void seedOrganization(long id, String name) {
        jdbc.update("INSERT INTO clinlims.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)"
                + " VALUES (?, ?, 'N', 'Y', now()) ON CONFLICT (id) DO NOTHING", id, name);
    }

    private void seedAnalyte() {
        jdbc.update("INSERT INTO clinlims.analyte (id, name, is_active, lastupdated)"
                + " SELECT ?, 'EQA Followup Analyte', 'Y', now()"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.analyte WHERE id = ?)", ANALYTE, ANALYTE);
    }

    private void seedTest() {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, 'EQA Followup CD4', 'EQA Followup CD4', 'Y', ?, now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                TEST_ID, UUID.randomUUID().toString(), TEST_ID);
    }
}
