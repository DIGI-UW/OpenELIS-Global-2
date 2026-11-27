package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED_NAME;
import static org.openelisglobal.pathology.PathologyConsultationFixture.DIFFICULT_DIAGNOSIS;
import static org.openelisglobal.pathology.PathologyConsultationFixture.INACTIVE_PATHOLOGIST;
import static org.openelisglobal.pathology.PathologyConsultationFixture.OLD_REASON;
import static org.openelisglobal.pathology.PathologyConsultationFixture.OUTSIDER;
import static org.openelisglobal.pathology.PathologyConsultationFixture.REQUESTER;
import static org.openelisglobal.pathology.PathologyConsultationFixture.TECHNICIAN;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.ConsultationAnswer;
import org.openelisglobal.program.service.ConsultationDraft;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Asking a colleague for a second opinion (FR-11.1 to FR-11.3): who may be
 * asked, where the case has to stand, what the request carries, and that it is
 * a row of its own that leaves the case as it was. The rules are pinned here
 * against the database; the endpoint's answers by the controller tests.
 */
public class PathologyConsultationRequestTest extends PathologyCaseTestSupport {

    private static final List<PathologyStatus> BENCH_STAGES = List.of(PathologyStatus.ACCESSIONED,
            PathologyStatus.GROSSING, PathologyStatus.DECALCIFICATION, PathologyStatus.PROCESSING,
            PathologyStatus.EMBEDDING, PathologyStatus.MICROTOMY, PathologyStatus.STAINING,
            PathologyStatus.COVERSLIPPING);

    @Autowired
    private PathologyConsultationService consultationService;

    private PathologyConsultationFixture fixture;
    private StageSwitches switches;
    private String consultationReferenceTableId;

    @Before
    public void seedThePeopleAndReasons() {
        switches = StageSwitches.allOn();
        fixture = PathologyConsultationFixture.seed(jdbcTemplate);
        consultationReferenceTableId = referenceTablesService
                .getReferenceTableByName(PathologyConsultationService.REFERENCE_TABLE).getId();
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void removeThePeopleAndReasons() {
        fixture.close();
        switches.close();
    }

    @Test
    public void aRequest_isStoredAsRequested_inTheActorsName_atTheServerClock() {
        long before = System.currentTimeMillis();
        PathologyConsultation stored = ask(REQUESTER, CONSULTED, DIFFICULT_DIAGNOSIS, true);
        long after = System.currentTimeMillis();

        Map<String, Object> row = consultationRow(stored.getId());
        assertEquals("on this case", CASE_ID, ((Number) row.get("pathology_sample_id")).intValue());
        assertEquals("awaited", "REQUESTED", row.get("status"));
        assertEquals("in the name of the signed-in user", 1002, ((Number) row.get("requested_by")).intValue());
        assertEquals("of the colleague named", 1003, ((Number) row.get("consulted_user_id")).intValue());
        assertEquals("with the reason picked", 9401, ((Number) row.get("reason_dictionary_id")).intValue());
        assertEquals("and the before-treatment answer", Boolean.TRUE, row.get("pre_treatment"));
        long requestedAt = ((Timestamp) row.get("requested_at")).getTime();
        assertTrue("at the server clock", requestedAt >= before && requestedAt <= after);
        assertNull("no outcome yet", row.get("outcome"));
        assertNull("no note", row.get("outcome_note"));
        assertNull("no answer time", row.get("answered_at"));
        assertEquals("one row", 1, consultationCount());
    }

    @Test
    public void aRequestIsAudited_withTheColleagueAndThePreTreatmentAnswer_andNothingElse() {
        PathologyConsultation stored = ask(REQUESTER, CONSULTED, DIFFICULT_DIAGNOSIS, true);

        List<History> entries = entriesOf(stored.getId());
        assertEquals("one entry", 1, entries.size());
        History entry = entries.get(0);
        assertEquals("as an insert", IActionConstants.AUDIT_TRAIL_INSERT, entry.getActivity());
        assertEquals("by the actor", REQUESTER, entry.getSysUserId());
        assertEquals("the colleague and the answer, never the reason or a name",
                Map.of("verb", PathologyConsultationService.PATHOLOGY_CONSULTATION_REQUESTED, "consultedUserId",
                        CONSULTED, "preTreatment", true),
                payloadOf(entry));
    }

    @Test
    public void onTheBench_aRequestIsRefused() {
        for (PathologyStatus stage : BENCH_STAGES) {
            storeAt(stage);
            assertRefusedWith("pathology.error.consultationOutsideReview", () -> ask(REQUESTER, CONSULTED, null, true));
        }
        assertEquals("nothing stored", 0, consultationCount());
    }

    @Test
    public void fromTheQueueUntilSignOut_aRequestIsTaken() {
        for (String review : List.of("true", "false")) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_UNDER_REVIEW_ENABLED,
                    review);
            for (PathologyStatus stage : List.of(PathologyStatus.READY_PATHOLOGIST, PathologyStatus.UNDER_REVIEW,
                    PathologyStatus.COMPLETED)) {
                storeAt(stage);
                PathologyConsultation stored = ask(REQUESTER, CONSULTED, null, false);
                assertEquals("taken at " + stage + " with review " + review, "REQUESTED",
                        consultationRow(stored.getId()).get("status"));
                consultationService.withdraw(CASE_ID, stored.getId(), REQUESTER);
            }
        }
        assertEquals("one row per stop", 6, consultationCount());
    }

    @Test
    public void onASignedOutCase_aRequestIsRefused() {
        signOut();

        assertRefusedWith("pathology.error.caseCompleted", () -> ask(REQUESTER, CONSULTED, null, true));
        assertEquals("no row", 0, consultationCount());
    }

    @Test
    public void aRequestWithoutAColleague_orWithoutThePreTreatmentAnswer_isRefused() {
        assertRefusedWith("pathology.error.consultationColleagueRequired",
                () -> consultationService.request(CASE_ID, null, REQUESTER));
        assertRefusedWith("pathology.error.consultationColleagueRequired", () -> ask(REQUESTER, null, null, true));
        assertRefusedWith("pathology.error.consultationColleagueRequired", () -> ask(REQUESTER, "  ", null, true));
        assertRefusedWith("pathology.error.consultationPreTreatmentRequired",
                () -> ask(REQUESTER, CONSULTED, null, null));
        assertEquals(0, consultationCount());
    }

    @Test
    public void askingOneself_isRefused() {
        assertRefusedWith("pathology.error.consultationSelf", () -> ask(REQUESTER, REQUESTER, null, true));
        assertRefusedWith("pathology.error.consultationSelf", () -> ask(REQUESTER, "00" + REQUESTER, null, true));
        assertEquals(0, consultationCount());
    }

    @Test
    public void aColleagueWhoIsNoPathologist_isInactive_orDoesNotExist_isRefused_notAnError() {
        for (String colleague : List.of(OUTSIDER, INACTIVE_PATHOLOGIST, "424242", "pathologist", "9999999999",
                "+" + CONSULTED)) {
            assertRefusedWith("pathology.error.consultationColleagueNotAvailable",
                    () -> ask(REQUESTER, colleague, null, true));
        }
        assertEquals(0, consultationCount());
    }

    @Test
    public void aColleagueStillAwaited_cannotBeAskedAgain_andCanOnceAnsweredOrWithdrawn() {
        PathologyConsultation first = ask(REQUESTER, CONSULTED, null, true);

        for (String actor : List.of(REQUESTER, OUTSIDER)) {
            LocalizedValidationException refusal = assertRefusedWith("pathology.error.consultationAlreadyOpen",
                    () -> ask(actor, CONSULTED, null, true));
            assertEquals("naming the colleague", Map.of("name", CONSULTED_NAME), refusal.getParams());
        }
        for (String actor : List.of(REQUESTER, OUTSIDER)) {
            LocalizedValidationException refusal = assertRefusedWith("pathology.error.consultationAlreadyOpen",
                    () -> ask(actor, "0" + CONSULTED, null, true));
            assertEquals("naming the colleague", Map.of("name", CONSULTED_NAME), refusal.getParams());
        }
        assertEquals("one row", 1, consultationCount());

        consultationService.answer(CASE_ID, first.getId(), new ConsultationAnswer("AGREE", null), CONSULTED);
        Map<String, Object> answered = consultationRow(first.getId());
        PathologyConsultation second = ask(REQUESTER, CONSULTED, null, true);
        assertNotEquals("a new row", first.getId(), second.getId());
        assertEquals("the answered one unchanged", answered, consultationRow(first.getId()));

        consultationService.withdraw(CASE_ID, second.getId(), REQUESTER);
        Map<String, Object> withdrawn = consultationRow(second.getId());
        PathologyConsultation third = ask(REQUESTER, CONSULTED, null, true);
        assertEquals("REQUESTED", consultationRow(third.getId()).get("status"));
        assertEquals("the withdrawn one unchanged", withdrawn, consultationRow(second.getId()));
    }

    @Test
    public void twoColleaguesCanBeAwaitedAtOnce() {
        PathologyConsultation fromOne = ask(TECHNICIAN, REQUESTER, null, true);
        PathologyConsultation fromOther = ask(TECHNICIAN, CONSULTED, null, false);

        assertEquals("REQUESTED", consultationRow(fromOne.getId()).get("status"));
        assertEquals("REQUESTED", consultationRow(fromOther.getId()).get("status"));
    }

    @Test
    public void aReasonIsOptional_andOneOfAnotherCategoryOrARetiredOneIsRefused() {
        PathologyConsultation withNone = ask(REQUESTER, CONSULTED, "  ", true);
        assertNull("no reason", consultationRow(withNone.getId()).get("reason_dictionary_id"));
        consultationService.withdraw(CASE_ID, withNone.getId(), REQUESTER);

        PathologyConsultation withOne = ask(REQUESTER, CONSULTED, DIFFICULT_DIAGNOSIS, true);
        assertEquals("the reason named", 9401,
                ((Number) consultationRow(withOne.getId()).get("reason_dictionary_id")).intValue());
        consultationService.withdraw(CASE_ID, withOne.getId(), REQUESTER);

        LocalizedValidationException retired = assertRefusedWith("pathology.error.consultationReasonNotInUse",
                () -> ask(REQUESTER, CONSULTED, OLD_REASON, true));
        assertEquals("naming it", Map.of("name", "Old reason"), retired.getParams());
        for (String notAReason : List.of(PathologyFindingsDictionary.ADENOCARCINOMA,
                PathologyFindingsDictionary.NO_ENTRY, "reason", "9999999999")) {
            assertThrows("the reload refusal for " + notAReason, PathologyCaseRuleException.class,
                    () -> ask(REQUESTER, CONSULTED, notAReason, true));
        }
        assertEquals("only the two accepted", 2, consultationCount());
    }

    @Test
    public void aRefusedRequest_writesNoRow_andNoHistory() {
        assertRefusedWith("pathology.error.consultationSelf", () -> ask(REQUESTER, REQUESTER, null, true));
        assertRefusedWith("pathology.error.consultationColleagueNotAvailable",
                () -> ask(REQUESTER, OUTSIDER, null, true));
        assertRefusedWith("pathology.error.consultationReasonNotInUse",
                () -> ask(REQUESTER, CONSULTED, OLD_REASON, true));
        assertThrows(PathologyCaseRuleException.class,
                () -> ask(REQUESTER, CONSULTED, PathologyFindingsDictionary.NO_ENTRY, true));
        storeAt(PathologyStatus.GROSSING);
        assertRefusedWith("pathology.error.consultationOutsideReview", () -> ask(REQUESTER, CONSULTED, null, true));

        assertEquals("no row", 0, consultationCount());
        assertEquals("no history", 0, consultationAuditCount());
    }

    @Test
    public void aRequest_leavesTheCaseRowAndItsVersionAsTheyWere() {
        clearCaseAudit();
        Map<String, Object> caseBefore = caseRow();

        PathologyConsultation asked = ask(REQUESTER, CONSULTED, null, true);
        consultationService.answer(CASE_ID, asked.getId(), new ConsultationAnswer("MODIFIED", "Grade 2"), CONSULTED);
        consultationService.withdraw(CASE_ID, ask(REQUESTER, CONSULTED, null, true).getId(), REQUESTER);

        assertEquals("the case row and its version as they were", caseBefore, caseRow());
        assertTrue("no case history row", caseAudit().isEmpty());
    }

    @Test
    public void theSchemaRefusesASecondAwaitedRowForOneColleague_andTakesOneOnceTheFirstIsClosed() {
        insertAwaitedRow();

        DataIntegrityViolationException duplicate = assertThrows(DataIntegrityViolationException.class,
                this::insertAwaitedRow);
        assertTrue("on the awaited index", duplicate.getMessage().contains("pathology_consultation_awaited_uk"));
        assertEquals(1, consultationCount());

        jdbcTemplate.update("UPDATE clinlims.pathology_consultation SET status = 'ANSWERED', outcome = 'AGREE',"
                + " answered_at = now()");
        insertAwaitedRow();
        assertEquals("a new awaited row once the first is answered", 2, consultationCount());
    }

    // helpers

    private PathologyConsultation ask(String actor, String colleague, String reason, Boolean preTreatment) {
        return consultationService.request(CASE_ID, new ConsultationDraft(colleague, reason, preTreatment), actor);
    }

    private void signOut() {
        closeTheFixtureRequest();
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }

    private void insertAwaitedRow() {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_consultation (id, pathology_sample_id, requested_by,"
                + " consulted_user_id, pre_treatment, status, requested_at, last_updated)"
                + " VALUES (nextval('clinlims.pathology_consultation_seq'), ?, 1002, 1003, true, 'REQUESTED', now(),"
                + " now())", CASE_ID);
    }

    private Map<String, Object> caseRow() {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_sample WHERE id = ?", CASE_ID);
    }

    private Map<String, Object> consultationRow(int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_consultation WHERE id = ?", id);
    }

    private int consultationCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_consultation", Integer.class);
    }

    private int consultationAuditCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_table = ?",
                Integer.class, Integer.parseInt(consultationReferenceTableId));
    }

    private List<History> entriesOf(int consultationId) {
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(consultationId),
                consultationReferenceTableId);
    }
}
