package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologyRequestForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.openelisglobal.program.valueholder.pathology.StainStatus;

/**
 * A case is sent to the pathologist with bench requests still open only once
 * the sender has seen them (FR-10.11, AC-20), and the case save no longer
 * writes requests at all.
 *
 * <p>
 * Before this nothing about a request held a case back, and every save of the
 * case deleted the requests and wrote them again from the form. These tests pin
 * the transition table of the send onto the pathologist's queue against the
 * open requests and the ids the page acknowledged, the save leaving every
 * request and its id alone, and a send or a save racing a request being raised:
 * whichever reads the case before the raise commits loses its version check, so
 * a request is never passed over unseen and never loses its case.
 */
public class PathologyRequestSendTest extends PathologyCaseTestSupport {

    private static final String DEEPER_LEVELS = "Deeper levels on A1";
    private static final String ANOTHER_BLOCK = "Another block from the margin";

    private StageSwitches switches;

    private int blockA1;
    private List<Integer> slides;

    /**
     * Case 1 at Coverslipping with two slides from A1 stained and passed, so the
     * stage lets it go, and the fixture's own request closed.
     */
    @Before
    public void standAtCoverslippingWithEverySlidePassed() {
        switches = StageSwitches.allOn();
        closeTheFixtureRequest();
        storeAt(PathologyStatus.MICROTOMY);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id = ?", LEGACY_SLIDE_ID);
        blockA1 = cutEmbeddedBlock();
        saveSlides(postedSlide(blockA1), postedSlide(blockA1));
        slides = slidesCutFrom(blockA1).stream().map(slide -> ((Number) slide.get("id")).intValue())
                .collect(Collectors.toList());
        slides.forEach(slideId -> {
            PathologyCaseRows.storeStainStatus(jdbcTemplate, slideId, StainStatus.STAINED);
            PathologyCaseRows.storeCoverslipQc(jdbcTemplate, slideId, "PASS");
        });
        storeAt(PathologyStatus.COVERSLIPPING);
        clearCaseAudit();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    // ---- the send onto the pathologist's queue: open requests x acknowledged ids
    // ----

    @Test
    public void withNoRequestOpen_noAcknowledgement_movesAndRecordsNoOverride() {
        sendAcknowledging(null);

        assertEquals("READY_PATHOLOGIST", storedStage());
        assertTrue("nothing was passed over", overriddenEntries().isEmpty());
    }

    @Test
    public void withNoRequestOpen_anyAcknowledgement_isIgnored() {
        sendAcknowledging(List.of(424242, FIXTURE_REQUEST_ID));

        assertEquals("READY_PATHOLOGIST", storedStage());
        assertTrue("nothing open was passed over", overriddenEntries().isEmpty());
    }

    /**
     * Two open requests and nothing acknowledged: refused, naming each instruction
     * newest first in one sentence, and nothing is written.
     */
    @Test
    public void withTwoOpen_noneOrNoAcknowledgement_isRefusedNamingBoth() {
        raiseTwo();

        for (List<Integer> acknowledged : Arrays.asList(null, List.<Integer>of())) {
            LocalizedValidationException refusal = assertRefusedWith("pathology.error.openRequestsUnacknowledged",
                    () -> sendAcknowledging(acknowledged));
            assertEquals("each open instruction, newest first, joined",
                    Map.of("instructions", ANOTHER_BLOCK + ", " + DEEPER_LEVELS), refusal.getParams());
        }
        assertNothingMoved();
    }

    @Test
    public void withTwoOpen_oneAcknowledged_isRefused() {
        List<Integer> open = raiseTwo();

        assertRefusedWith("pathology.error.openRequestsUnacknowledged", () -> sendAcknowledging(List.of(open.get(0))));
        assertNothingMoved();
    }

    /**
     * A request closed since the page was drawn stands for none of the open ones.
     */
    @Test
    public void withTwoOpen_onlyAClosedIdAcknowledged_isRefused() {
        List<Integer> open = raiseTwo();
        int closed = raise(blockRequest("Recut A1")).getId();
        changeStatus(closed, "OPENED", "COMPLETED");
        clearCaseAudit();

        assertRefusedWith("pathology.error.openRequestsUnacknowledged", () -> sendAcknowledging(List.of(closed)));
        assertNothingMoved();
        assertEquals("the open ones stay open", 2,
                open.stream().filter(id -> "OPENED".equals(requestRow(id).get("status"))).count());
    }

    /**
     * Every open id acknowledged beside one closed since: the move goes ahead, and
     * only the ids open at that moment are recorded.
     */
    @Test
    public void withTwoOpen_bothAcknowledgedBesideAClosedId_moves() {
        List<Integer> open = raiseTwo();
        int closed = raise(blockRequest("Recut A1")).getId();
        changeStatus(closed, "OPENED", "COMPLETED");

        sendAcknowledging(List.of(closed, open.get(0), open.get(1)));

        assertEquals("READY_PATHOLOGIST", storedStage());
        assertEquals("only the open ones, newest first", List.of(open.get(1), open.get(0)),
                payloadOf(onlyOverriddenEntry()).get("requestIds"));
    }

    /**
     * Both acknowledged, in either order: the case moves, one entry on the case
     * records the two passed over, written before the move's own entry, and the
     * requests stay open.
     */
    @Test
    public void withTwoOpen_bothAcknowledgedInAnyOrder_movesWithOneOverrideBeforeTheMove() {
        List<Integer> open = raiseTwo();

        sendAcknowledging(List.of(open.get(0), open.get(1)));

        assertEquals("READY_PATHOLOGIST", storedStage());
        History overridden = onlyOverriddenEntry();
        assertEquals("recorded as an update of the case", "U", overridden.getActivity());
        assertEquals("by the session user", TECHNICIAN_ID, overridden.getSysUserId());
        assertEquals(Map.of("verb", PathologySampleService.PATHOLOGY_REQUEST_OVERRIDDEN, "requestIds",
                List.of(open.get(1), open.get(0))), payloadOf(overridden));
        History advanced = caseEntries(Set.of(PathologySampleService.PATHOLOGY_STAGE_ADVANCED)).get(0);
        assertTrue("the override is written before the move",
                Long.parseLong(overridden.getId()) < Long.parseLong(advanced.getId()));
        for (Integer requestId : open) {
            assertEquals("sending the case does not close a request", "OPENED", requestRow(requestId).get("status"));
        }
    }

    /** The stage's own rule comes first, acknowledged or not. */
    @Test
    public void withTwoOpen_andTheStageUnfinished_isRefusedByTheStageRuleFirst() {
        List<Integer> open = raiseTwo();
        PathologyCaseRows.storeCoverslipQc(jdbcTemplate, slides.get(1), "FAIL");

        assertRefusedWith("pathology.error.coverslipQcIncomplete", () -> sendAcknowledging(null));
        assertRefusedWith("pathology.error.coverslipQcIncomplete", () -> sendAcknowledging(open));
        assertNothingMoved();
    }

    /** A return, and a send onto any other stage, take no acknowledgement. */
    @Test
    public void withTwoOpen_aReturnOrASendElsewhere_isUnchanged() {
        raiseTwo();

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.COVERSLIPPING, "restain A1.2",
                TECHNICIAN_ID);
        assertEquals("STAINING", storedStage());
        pathologySampleService.advance(CASE_ID, PathologyStatus.STAINING, TECHNICIAN_ID);
        assertEquals("COVERSLIPPING", storedStage());
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.READY_PATHOLOGIST, "slide cracked",
                TECHNICIAN_ID);
        assertEquals("COVERSLIPPING", storedStage());
        assertTrue("nothing was passed over", overriddenEntries().isEmpty());
    }

    /**
     * The send the controller has made since before requests held a case back
     * acknowledges nothing, so it is refused while one is open.
     */
    @Test
    public void theSendWithNoAcknowledgement_isRefusedWhileARequestIsOpen() {
        raiseTwo();

        assertRefusedWith("pathology.error.openRequestsUnacknowledged",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.COVERSLIPPING, TECHNICIAN_ID));
        assertNothingMoved();
    }

    // ---- the case save leaves requests alone ----

    /**
     * A save posting no requests, none, or a different list, as an older page
     * would, leaves every request and its id as they were, and writes no requests
     * line in the case history.
     */
    @Test
    public void aCaseSave_whateverRequestsItPosts_leavesEveryRequestAndItsId() {
        raiseTwo();
        List<Map<String, Object>> before = requestsOnCase();
        clearCaseAudit();

        PathologySampleForm none = caseViewForm();
        none.setGrossExam("Described again with no requests posted");
        save(none);
        PathologySampleForm empty = caseViewForm();
        empty.setGrossExam("Described again with an empty list");
        empty.setRequests(new ArrayList<>());
        save(empty);
        PathologySampleForm different = caseViewForm();
        different.setGrossExam("Described again with another list");
        PathologyRequestForm posted = new PathologyRequestForm();
        posted.setValue("a request the page made up");
        posted.setStatus(RequestStatus.CANCELLED);
        different.setRequests(List.of(posted));
        save(different);

        assertEquals("every request, its id, status and version are as they were", before, requestsOnCase());
        assertEquals("three saves, three diff entries", 3, caseSaveDiffs().size());
        for (History diff : caseSaveDiffs()) {
            assertTrue("no diff carries the requests", !changesOf(diff).contains("<requests>"));
        }
    }

    // ---- a send or a save racing a raise or a reopen (FR-10.11) ----

    /**
     * A request is raised while a send reads the case: the send saw no open request
     * and would pass the new one over unseen, so it loses the case's version check.
     * No override is recorded and the request is kept.
     */
    @Test
    public void aRaiseHeldOpen_makesASendLose_andTheRequestIsKept() {
        Outcome outcome = holdOpenWhile(() -> raise(slideRequest(blockA1, DEEPER_LEVELS)),
                () -> sendAcknowledging(List.of()));

        assertNull("the raise commits", outcome.held());
        assertTrue("the send loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the case stays at coverslipping", "COVERSLIPPING", storedStage());
        assertEquals("the request is kept, open", 2, requestsOnCase().size());
        assertTrue("no override is recorded", overriddenEntries().isEmpty());
        assertTrue("nor a move", caseEntries(Set.of(PathologySampleService.PATHOLOGY_STAGE_ADVANCED)).isEmpty());
    }

    /** The other order: the send holds the case, and the raise loses. */
    @Test
    public void aSendHeldOpen_makesARaiseLose() {
        Outcome outcome = holdOpenWhile(() -> sendAcknowledging(List.of()),
                () -> raise(slideRequest(blockA1, DEEPER_LEVELS)));

        assertNull("the send commits", outcome.held());
        assertTrue("the raise loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("READY_PATHOLOGIST", storedStage());
        assertEquals("no request is added", 1, requestsOnCase().size());
        assertEquals("nor audited", 0, requestAuditCount());
    }

    /**
     * A completed request is reopened while a send reads the case: the send saw
     * none open and would pass the reopened one over unseen. The reopen writes only
     * the request's own row, so it is the case's version, raised by the reopen,
     * that makes the send lose. No override is recorded.
     */
    @Test
    public void aReopenHeldOpen_makesASendLose() {
        int completed = raise(slideRequest(blockA1, DEEPER_LEVELS)).getId();
        changeStatus(completed, "OPENED", "COMPLETED");
        clearCaseAudit();

        Outcome outcome = holdOpenWhile(() -> changeStatus(completed, "COMPLETED", "OPENED"),
                () -> sendAcknowledging(List.of()));

        assertNull("the reopen commits", outcome.held());
        assertTrue("the send loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the case stays at coverslipping", "COVERSLIPPING", storedStage());
        assertEquals("the request is open again", "OPENED", requestRow(completed).get("status"));
        assertTrue("no override is recorded", overriddenEntries().isEmpty());
        assertTrue("nor a move", caseEntries(Set.of(PathologySampleService.PATHOLOGY_STAGE_ADVANCED)).isEmpty());
    }

    /**
     * A request is raised while a save reads the case: the save merges a case that
     * does not hold the new request, so it loses the case's version check before it
     * could take the request off the case.
     */
    @Test
    public void aRaiseHeldOpen_makesACaseSaveLose_andTheNewRequestStaysOnTheCase() {
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("Described again while a request was raised");

        Outcome outcome = holdOpenWhile(() -> raise(slideRequest(blockA1, DEEPER_LEVELS)), () -> {
            save(form);
            return null;
        });

        assertNull("the raise commits", outcome.held());
        assertTrue("the save loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        List<Map<String, Object>> requests = requestsOnCase();
        assertEquals("the new request is still on the case", 2, requests.size());
        assertEquals(DEEPER_LEVELS, requests.get(1).get("value"));
    }

    /** The other order: the save holds the case, and the raise loses. */
    @Test
    public void aCaseSaveHeldOpen_makesARaiseLose() {
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("Described again while a request was raised");

        Outcome outcome = holdOpenWhile(() -> save(form), () -> raise(slideRequest(blockA1, DEEPER_LEVELS)));

        assertNull("the save commits", outcome.held());
        assertTrue("the raise loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("no request is added", 1, requestsOnCase().size());
    }

    // helpers

    /** Two open requests, a slide request then a block request, oldest first. */
    private List<Integer> raiseTwo() {
        int first = raise(slideRequest(blockA1, DEEPER_LEVELS)).getId();
        int second = raise(blockRequest(ANOTHER_BLOCK)).getId();
        clearCaseAudit();
        return List.of(first, second);
    }

    private PathologyStageEvent sendAcknowledging(List<Integer> acknowledgedRequestIds) {
        return pathologySampleService.advance(CASE_ID, PathologyStatus.COVERSLIPPING, TECHNICIAN_ID,
                acknowledgedRequestIds);
    }

    private void assertNothingMoved() {
        assertEquals("the case stays at coverslipping", "COVERSLIPPING", storedStage());
        assertEquals("no stage event is written", 0,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?",
                        Integer.class, CASE_ID).intValue());
        assertTrue("nor any entry on the case", caseAudit().isEmpty());
    }

    private List<History> overriddenEntries() {
        return caseEntries(Set.of(PathologySampleService.PATHOLOGY_REQUEST_OVERRIDDEN));
    }

    private History onlyOverriddenEntry() {
        List<History> entries = overriddenEntries();
        assertEquals("one override entry", 1, entries.size());
        return entries.get(0);
    }

    private List<History> caseEntries(Set<String> verbs) {
        return caseAudit().stream().filter(history -> changesOf(history).startsWith("{"))
                .filter(history -> verbs.contains(payloadOf(history).get("verb")))
                .sorted(Comparator.comparing(history -> Long.valueOf(history.getId()))).collect(Collectors.toList());
    }
}
