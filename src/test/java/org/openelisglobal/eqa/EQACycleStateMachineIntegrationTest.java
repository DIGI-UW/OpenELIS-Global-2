package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.eqa.controller.rest.EQACycleRestController;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelReceiptDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAInvalidTransitionException;
import org.openelisglobal.eqa.service.EQAProgramService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStateTransition;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQAStateMachine;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQATriggerEvent;
import org.openelisglobal.eqa.valueholder.EQATriggerType;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * OGC-609 [EQA V2.1 / T-10] — the two cycle state machines, their audit trail,
 * and the derived per-lab participant state, against a real DB.
 */
public class EQACycleStateMachineIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER = "1";
    private static final long ADMIN_USER_ID = 1L;
    private static final long ENROLLMENT_ID = 9905L;
    private static final long OTHER_ENROLLMENT_ID = 9906L;
    private static final long ANALYTE_HIV_VL = 9802L;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private EQAProgramService eqaProgramService;

    @Autowired
    private EQACycleDAO eqaCycleDAO;

    @Autowired
    private EQARoundDAO eqaRoundDAO;

    @Autowired
    private EQAParticipantResultDAO eqaParticipantResultDAO;

    @Autowired
    private EQAPanelReceiptDAO eqaPanelReceiptDAO;

    @Autowired
    private EQAPanelDAO eqaPanelDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/eqa-cycle-spine.xml");
        clean();
        seedEnrollment(ENROLLMENT_ID, "Cycle machine enrollment");
        seedEnrollment(OTHER_ENROLLMENT_ID, "Second lab enrollment");
    }

    @After
    public void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("DELETE FROM clinlims.eqa_panel_sample");
        jdbc.update("DELETE FROM clinlims.eqa_panel");
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_panel_receipt");
        jdbc.update("DELETE FROM clinlims.eqa_cycle_state_transition");
        jdbc.update("DELETE FROM clinlims.eqa_round");
        jdbc.update("DELETE FROM clinlims.eqa_cycle");
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE id IN (?, ?)", ENROLLMENT_ID,
                OTHER_ENROLLMENT_ID);
        jdbc.update("DELETE FROM clinlims.eqa_program_test");
        jdbc.update("DELETE FROM clinlims.eqa_program");
    }

    // ---- FR-V2.1-04 / FR-V2.1-18: legal and illegal edges ----

    @Test
    public void participantCycleWalksItsHappyPathAndAuditsEveryStep() {
        EQACycle cycle = insertCycle();

        EQACycleStatus[] path = { EQACycleStatus.PANEL_RECEIVED, EQACycleStatus.TESTING, EQACycleStatus.READY_TO_SUBMIT,
                EQACycleStatus.SUBMITTED, EQACycleStatus.SCORED, EQACycleStatus.CLOSED };
        for (EQACycleStatus next : path) {
            cycleService.transition(cycle.getId(), next, EQAStateMachine.PARTICIPANT, EQATriggerType.AUTO,
                    EQATriggerEvent.LAST_VALIDATED_RESULT, null, null, USER);
        }

        assertEquals(EQACycleStatus.CLOSED,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());

        List<EQACycleStateTransition> audit = cycleService.getTransitions(cycle.getId());
        assertEquals("one audit row per transition", path.length, audit.size());
        assertEquals("PLANNED", audit.get(0).getPriorState());
        assertEquals("PANEL_RECEIVED", audit.get(0).getNewState());
        assertEquals("CLOSED", audit.get(audit.size() - 1).getNewState());
        assertEquals(EQAStateMachine.PARTICIPANT, audit.get(0).getStateMachine());
        assertEquals(EQATriggerType.AUTO, audit.get(0).getTriggerType());
        assertNull("an automatic transition has no actor", audit.get(0).getTriggeredBy());
    }

    @Test
    public void providerCycleWalksItsOwnLongerPath() {
        EQACycle cycle = insertCycle();
        // A QC-passed panel is a precondition of ready_to_ship, so the happy path
        // has to seed one — walking this edge with no panel is the bug below.
        insertPanel(cycle, true);

        EQACycleStatus[] path = { EQACycleStatus.PREP_IN_PROGRESS, EQACycleStatus.READY_TO_SHIP, EQACycleStatus.SHIPPED,
                EQACycleStatus.DELIVERED, EQACycleStatus.SUBMISSIONS_OPEN, EQACycleStatus.SUBMISSIONS_CLOSED,
                EQACycleStatus.SCORING, EQACycleStatus.SCORED, EQACycleStatus.CLOSED };
        for (EQACycleStatus next : path) {
            cycleService.transition(cycle.getId(), next, EQAStateMachine.PROVIDER, EQATriggerType.AUTO,
                    EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
        }

        assertEquals(EQACycleStatus.CLOSED,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());
        assertEquals(path.length, cycleService.getTransitions(cycle.getId()).size());
    }

    @Test
    public void skippingAStateIsRefusedAndWritesNoAudit() {
        // AC-V2.1-05: planned -> testing skips panel_received.
        EQACycle cycle = insertCycle();
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.TESTING, EQAStateMachine.PARTICIPANT,
                    EQATriggerType.AUTO, EQATriggerEvent.LAST_VALIDATED_RESULT, null, null, USER);
            fail("planned -> testing is not an edge in the participant machine");
        } catch (EQAInvalidTransitionException expected) {
            assertEquals(EQACycleStatus.PLANNED, expected.getPriorState());
            assertEquals(EQACycleStatus.TESTING, expected.getAttemptedState());
        }

        assertEquals("the cycle must not have moved", EQACycleStatus.PLANNED,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());
        assertTrue("a refused transition writes no audit row", cycleService.getTransitions(cycle.getId()).isEmpty());
    }

    @Test
    public void aProviderEdgeIsNotAvailableToAParticipant() {
        // The same row, read through the other machine, has different edges.
        EQACycle cycle = insertCycle();
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PARTICIPANT,
                    EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
            fail("prep_in_progress belongs to the provider machine only");
        } catch (EQAInvalidTransitionException expected) {
            assertEquals(EQACycleStatus.PLANNED, expected.getPriorState());
        }
    }

    @Test
    public void closedIsFinalOnBothMachines() {
        EQACycle cycle = insertCycle();
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'CLOSED' WHERE id = ?", cycle.getId());
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.SCORED, EQAStateMachine.PROVIDER,
                    EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, ADMIN_USER_ID, "reopen", USER);
            fail("a closed cycle has no outgoing edges");
        } catch (EQAInvalidTransitionException expected) {
            assertEquals(EQACycleStatus.CLOSED, expected.getPriorState());
        }
    }

    // ---- FR-V2.1-18 / AC-V2.1-13: the prep -> ready_to_ship gate ----

    @Test
    public void aPanelThatFailedHomogeneityQcCannotBeShipped() {
        // The ISO 17043 supervisor gate. Without it a failed panel is marked
        // shippable and the audit row recording it looks entirely legitimate.
        EQACycle cycle = insertCycle();
        insertPanel(cycle, false);
        cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);

        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.READY_TO_SHIP, EQAStateMachine.PROVIDER,
                    EQATriggerType.AUTO, EQATriggerEvent.HOMOGENEITY_QC_PASSED, null, null, USER);
            fail("AC-V2.1-13: homogeneity_qc_passed = false must block ready_to_ship");
        } catch (EQAInvalidTransitionException expected) {
            assertEquals(EQACycleStatus.PREP_IN_PROGRESS, expected.getPriorState());
            assertTrue(expected.getMessage().contains("homogeneity"));
        }
        assertEquals("the cycle must not have moved", EQACycleStatus.PREP_IN_PROGRESS,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void aCycleWithNoPanelCannotBeShipped() {
        EQACycle cycle = insertCycle();
        cycleService.transition(cycle.getId(), EQACycleStatus.PREP_IN_PROGRESS, EQAStateMachine.PROVIDER,
                EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.READY_TO_SHIP, EQAStateMachine.PROVIDER,
                    EQATriggerType.AUTO, EQATriggerEvent.SCHEDULED_JOB, null, null, USER);
            fail("there is nothing to ship");
        } catch (EQAInvalidTransitionException expected) {
            assertTrue(expected.getMessage().contains("no panel"));
        }
    }

    @Test
    public void theGateOnlyAppliesToThatOneProviderEdge() {
        // A participant cycle with a failed panel is unaffected — the gate must not
        // leak onto edges FR-V2.1-18 does not name.
        EQACycle cycle = insertCycle();
        insertPanel(cycle, false);
        cycleService.transition(cycle.getId(), EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                EQATriggerType.AUTO, EQATriggerEvent.LAST_VALIDATED_RESULT, null, null, USER);
        assertEquals(EQACycleStatus.PANEL_RECEIVED,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    // ---- FR-V2.1-21: manual transitions carry a reason and an actor ----

    @Test
    public void anAutomaticTransitionDiscardsAnyActorHandedToIt() {
        // The service stores triggeredBy only for MANUAL rows. Passing null with
        // AUTO — as every other test does — cannot tell that ternary from one that
        // stores the actor unconditionally, so this hands AUTO a real user id and
        // asserts it is dropped. An AUTO row naming a person would misattribute a
        // system event to them, permanently.
        EQACycle cycle = insertCycle();
        cycleService.transition(cycle.getId(), EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                EQATriggerType.AUTO, EQATriggerEvent.LAST_VALIDATED_RESULT, ADMIN_USER_ID, null, USER);

        EQACycleStateTransition audit = cycleService.getTransitions(cycle.getId()).get(0);
        assertEquals(EQATriggerType.AUTO, audit.getTriggerType());
        assertNull("an automatic transition must not name a person, even when handed one", audit.getTriggeredBy());
    }

    @Test
    public void aManualTransitionRecordsWhoAndWhy() {
        EQACycle cycle = insertCycle();
        cycleService.transition(cycle.getId(), EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, ADMIN_USER_ID, "Courier delivered early", USER);

        EQACycleStateTransition audit = cycleService.getTransitions(cycle.getId()).get(0);
        assertEquals(EQATriggerType.MANUAL, audit.getTriggerType());
        assertEquals(Long.valueOf(ADMIN_USER_ID), audit.getTriggeredBy());
        assertEquals("Courier delivered early", audit.getReason());
    }

    @Test
    public void aManualTransitionWithoutAReasonIsRefused() {
        // AC-V2.1-19 requires a reason even on a happy-path manual move, which is
        // stricter than FR-V2.1-21's "off the happy path" wording.
        EQACycle cycle = insertCycle();
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                    EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, ADMIN_USER_ID, "   ", USER);
            fail("a manual transition needs a reason");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("reason"));
        }
        assertEquals(EQACycleStatus.PLANNED,
                eqaCycleDAO.get(cycle.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    @Test
    public void aManualTransitionWithoutAnActorIsRefused() {
        // An unattributed manual override is worse than no audit row, because it
        // still looks like one. AUTO rows legitimately have no actor; MANUAL never.
        EQACycle cycle = insertCycle();
        try {
            cycleService.transition(cycle.getId(), EQACycleStatus.PANEL_RECEIVED, EQAStateMachine.PARTICIPANT,
                    EQATriggerType.MANUAL, EQATriggerEvent.MANUAL_OVERRIDE, null, "no actor supplied", USER);
            fail("a manual transition must name the person who made it");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("acting user"));
        }
        assertTrue("nothing is audited when the actor is unknown",
                cycleService.getTransitions(cycle.getId()).isEmpty());
    }

    @Test
    public void theTransitionGuardNamesAPermissionThatActuallyExists() throws Exception {
        // A @PreAuthorize referencing an authority no migration creates fails
        // closed and silently locks out every user, which looks identical to a
        // working guard until someone tries to use the feature. This ties the
        // annotation to the migration so the two cannot drift apart.
        //
        // Deliberately NOT asserted against system_module rows: five dbUnit
        // fixtures declare that table, and loading any of them truncates it
        // (BaseWebContextSensitiveTest wipes every table a dataset names), so a
        // row-count assertion is a coin flip on suite order — green under
        // -Dtest='EQA*', red in full-suite CI. The migration source plus its
        // databasechangelog execution record survive any fixture load.
        Method handler = EQACycleRestController.class.getMethod("transition", HttpServletRequest.class, Long.class,
                Map.class);
        PreAuthorize guard = handler.getAnnotation(PreAuthorize.class);
        assertNotNull("advancing a cycle must carry a write guard, not the class-level read roles", guard);

        Matcher authority = Pattern.compile("hasAuthority\\('([^']+)'\\)").matcher(guard.value());
        assertTrue("the guard should name an authority: " + guard.value(), authority.find());
        String permission = authority.group(1);

        String changeset;
        try (java.io.InputStream in = getClass().getClassLoader()
                .getResourceAsStream("liquibase/qa/023-add-eqa-manage-permission.xml")) {
            assertNotNull("the permission migration must exist on the classpath", in);
            changeset = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        assertTrue("the guard names '" + permission + "' but qa/023 does not register it",
                changeset.contains("<column name=\"name\" value=\"" + permission + "\"/>"));
        assertTrue("qa/023 must also grant '" + permission + "' to at least one role",
                changeset.contains("m.name = '" + permission + "'"));

        assertEquals("qa/023's register+grant changesets must have executed against this schema", Integer.valueOf(2),
                jdbc.queryForObject("SELECT count(*) FROM databasechangelog WHERE id IN"
                        + " ('qa-035-add-qa-manage-eqa-module', 'qa-036-grant-qa-manage-eqa')"
                        + " AND exectype IN ('EXECUTED', 'RERAN')", Integer.class));
    }

    // ---- FR-V2.1-18: the derivation table ----

    @Test
    public void participantStateIsDerivedFromReceiptsAndResults() {
        EQACycle cycle = insertCycle();

        assertEquals("no receipt yet", EQACycleStatus.PLANNED,
                cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));

        insertReceipt(cycle);
        assertEquals("receipt but no results", EQACycleStatus.PANEL_RECEIVED,
                cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));

        EQARound round = insertRound(cycle);
        Long draftId = insertResult(cycle, round, EQASubmissionStatus.DRAFT);
        assertEquals("a draft result means testing", EQACycleStatus.TESTING,
                cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));

        setResultStatus(draftId, EQASubmissionStatus.VALIDATED_PARTIAL);
        assertEquals("all validated, none submitted", EQACycleStatus.READY_TO_SUBMIT,
                cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));

        setResultStatus(draftId, EQASubmissionStatus.SUBMITTED);
        assertEquals(EQACycleStatus.SUBMITTED, cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));

        setResultStatus(draftId, EQASubmissionStatus.SCORED);
        assertEquals(EQACycleStatus.SCORED, cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));
    }

    @Test
    public void theMostAdvancedResultWinsWhenRowsDisagree() {
        // The FRS table's rows overlap and it never states precedence; a lab that
        // has been scored is scored even while another analyte sits in draft.
        EQACycle cycle = insertCycle();
        insertReceipt(cycle);
        EQARound round = insertRound(cycle);
        insertResultForAnalyte(cycle, round, EQASubmissionStatus.DRAFT, ANALYTE_HIV_VL);
        insertResultForAnalyte(cycle, round, EQASubmissionStatus.SCORED, 9801L);

        assertEquals(EQACycleStatus.SCORED, cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));
    }

    @Test
    public void oneLabsProgressDoesNotLeakIntoAnother() {
        EQACycle cycle = insertCycle();
        insertReceipt(cycle);

        assertEquals(EQACycleStatus.PANEL_RECEIVED, cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));
        assertEquals("the second lab has not received anything", EQACycleStatus.PLANNED,
                cycleService.deriveParticipantState(cycle.getId(), OTHER_ENROLLMENT_ID));
    }

    @Test
    public void aClosedCycleReadsClosedForEveryLab() {
        EQACycle cycle = insertCycle();
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'CLOSED' WHERE id = ?", cycle.getId());
        assertEquals(EQACycleStatus.CLOSED, cycleService.deriveParticipantState(cycle.getId(), ENROLLMENT_ID));
    }

    // ---- helpers ----

    private void seedEnrollment(long id, String name) {
        jdbc.update("INSERT INTO clinlims.eqa_lab_program_enrollment"
                + " (id, program_name, provider, is_active, created_date, sys_user_id, lastupdated)"
                + " VALUES (?, ?, 'NHLS', true, now(), ?, now())", id, name, USER);
    }

    private EQACycle insertCycle() {
        EQAProgram scheme = new EQAProgram();
        scheme.setName("Machine scheme " + System.nanoTime());
        scheme.setSchemeType(EQASchemeType.INTERNATIONAL_PT);
        scheme.setProvider("NHLS");
        scheme.setSysUserId(USER);
        scheme.setId(eqaProgramService.insert(scheme));

        EQACycle cycle = new EQACycle();
        cycle.setScheme(scheme);
        cycle.setCycleNumber(1);
        cycle.setCreatedBy(systemUserService.get(String.valueOf(ADMIN_USER_ID)));
        cycle.setSysUserId(USER);
        return eqaCycleDAO.get(eqaCycleDAO.insert(cycle)).orElseThrow(AssertionError::new);
    }

    private void insertPanel(EQACycle cycle, boolean homogeneityPassed) {
        EQAPanel panel = new EQAPanel();
        panel.setScheme(cycle.getScheme());
        panel.setCycle(cycle);
        panel.setPanelName("Panel for cycle " + cycle.getId());
        panel.setHomogeneityQcPassed(homogeneityPassed);
        panel.setSysUserId(USER);
        eqaPanelDAO.insert(panel);
    }

    private EQARound insertRound(EQACycle cycle) {
        EQARound round = new EQARound();
        round.setCycle(cycle);
        round.setRoundNumber(1);
        round.setSysUserId(USER);
        return eqaRoundDAO.get(eqaRoundDAO.insert(round)).orElseThrow(AssertionError::new);
    }

    private void insertReceipt(EQACycle cycle) {
        EQAPanelReceipt receipt = new EQAPanelReceipt();
        receipt.setCycle(cycle);
        receipt.setLabEnrollmentId(ENROLLMENT_ID);
        receipt.setReceivedDate(Date.valueOf("2026-08-14"));
        receipt.setReceivedBy(ADMIN_USER_ID);
        receipt.setSysUserId(USER);
        eqaPanelReceiptDAO.insert(receipt);
    }

    private Long insertResult(EQACycle cycle, EQARound round, EQASubmissionStatus status) {
        return insertResultForAnalyte(cycle, round, status, ANALYTE_HIV_VL);
    }

    private Long insertResultForAnalyte(EQACycle cycle, EQARound round, EQASubmissionStatus status, long analyteId) {
        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(cycle);
        result.setRound(round);
        result.setLabEnrollmentId(ENROLLMENT_ID);
        result.setAnalyteId(analyteId);
        result.setSubmissionStatus(status);
        result.setSysUserId(USER);
        return eqaParticipantResultDAO.insert(result);
    }

    private void setResultStatus(Long resultId, EQASubmissionStatus status) {
        jdbc.update("UPDATE clinlims.eqa_participant_result SET submission_status = ? WHERE id = ?", status.name(),
                resultId);
    }
}
