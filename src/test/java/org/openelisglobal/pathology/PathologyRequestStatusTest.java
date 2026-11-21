package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.program.service.PathologyRequestConflictException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.RequestStatusChange;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * The bench completes a request, naming what fulfilled it if it likes, or
 * cancels it with a reason, and either can be reopened (FR-10.8, FR-10.9).
 *
 * <p>
 * Before this a request's status was a dropdown on the case form and every save
 * wrote the request again, so nothing recorded who closed a request or when. A
 * change of status is now its own write, audited on the request and on the case
 * history. These tests pin the transition table of the status, every refusal in
 * the order it fires with nothing written, the page that shows a status the
 * request has left, a request on file with no kind or no version, and three
 * completions at once.
 */
public class PathologyRequestStatusTest extends PathologyCaseTestSupport {

    /** This class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_STAIN_CATEGORY = 9771;
    private static final int HE = 9772;

    /** A block on the fixture's other case. */
    private static final int BLOCK_OF_ANOTHER_CASE = 9781;

    /** The fixture's other case's request, closed. */
    private static final int REQUEST_OF_ANOTHER_CASE = 302;

    /** A request on file inserted with no last_updated, as SQL once wrote them. */
    private static final int REQUEST_WITH_NO_VERSION = 9791;

    private StageSwitches switches;
    private DictionaryRows.StainEntries stains;

    private int blockA1;
    private int slideA11;
    private int retiredSlideA12;

    /**
     * A stain request on A1.1, a block request and a slide request on A1, all open.
     */
    private int stainRequest;
    private int blockRequest;
    private int slideRequest;

    @Before
    public void raiseOneRequestOfEachKind() {
        switches = StageSwitches.allOn();
        stains = DictionaryRows.stainEntries(jdbcTemplate, OWN_STAIN_CATEGORY).with(HE, "Haematoxylin and eosin", true);
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET pathology_sample_id = 2 WHERE id = ?",
                LEGACY_SLIDE_ID);
        jdbcTemplate.update("INSERT INTO clinlims.pathology_block (id, pathology_sample_id, designation, barcode,"
                + " cassette_state, active, last_updated) VALUES (?, 2, 'A1', 'OTHER.A1', 'BLOCK', true, now())",
                BLOCK_OF_ANOTHER_CASE);

        storeAt(PathologyStatus.MICROTOMY);
        blockA1 = cutEmbeddedBlock();
        saveSlides(postedSlide(blockA1), postedSlide(blockA1));
        List<Map<String, Object>> slides = slidesCutFrom(blockA1);
        slideA11 = ((Number) slides.get(0).get("id")).intValue();
        retiredSlideA12 = ((Number) slides.get(1).get("id")).intValue();
        pathologySampleService.deactivateSlide(retiredSlideA12, "cracked", TECHNICIAN_ID);

        stainRequest = raise(stainRequest(slideA11, String.valueOf(HE), "PAS on A1.1")).getId();
        blockRequest = raise(blockRequest("Another block from the margin")).getId();
        slideRequest = raise(slideRequest(blockA1, "Deeper levels")).getId();
        clearRequestAudit();
        clearCaseAudit();
    }

    @After
    public void tearDown() {
        switches.close();
        stains.close();
    }

    // ---- the transition table: stored status x posted status ----

    @Test
    public void anOpenRequest_completedWithNoResult_isCompletedWithOneEntryAndOneHistoryLine() {
        PathologyRequest changed = changeStatus(stainRequest, "OPENED", "COMPLETED");

        assertEquals(RequestStatus.COMPLETED, changed.getStatus());
        Map<String, Object> row = requestRow(stainRequest);
        assertEquals("COMPLETED", row.get("status"));
        assertNull("nothing is named as fulfilling it", row.get("result_slide_id"));
        History entry = onlyStatusEntry(stainRequest);
        assertEquals("recorded as an update", "U", entry.getActivity());
        assertEquals("by the session user", TECHNICIAN_ID, entry.getSysUserId());
        assertEquals(changed("OPENED", "COMPLETED"), payloadOf(entry));
        assertEquals("one diff entry on the case", 1, caseSaveDiffs().size());
        assertTrue("carrying the requests", changesOf(caseSaveDiffs().get(0)).contains("<requests>"));
    }

    @Test
    public void anOpenRequest_completedWithWhatFulfilledIt_storesItAndNamesItInTheEntry() {
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
        changeStatus(blockRequest, new RequestStatusChange("OPENED", "COMPLETED", null, blockA1, null));

        assertEquals(slideA11, ((Number) requestRow(stainRequest).get("result_slide_id")).intValue());
        assertEquals(blockA1, ((Number) requestRow(blockRequest).get("result_block_id")).intValue());
        assertEquals("the slide by id and as printed",
                changed("OPENED", "COMPLETED", "resultSlideId", slideA11, "result", "A1.1"),
                payloadOf(onlyStatusEntry(stainRequest)));
        assertEquals("the block by id and as printed",
                changed("OPENED", "COMPLETED", "resultBlockId", blockA1, "result", "A1"),
                payloadOf(onlyStatusEntry(blockRequest)));
    }

    @Test
    public void anOpenRequest_completedWithASlideOfAnotherCase_isRefused() {
        assertNotChanged("pathology.error.requestResultNotOnCase", stainRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, LEGACY_SLIDE_ID));
        assertNotChanged("pathology.error.requestResultNotOnCase", blockRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, BLOCK_OF_ANOTHER_CASE, null));
    }

    @Test
    public void anOpenRequest_completedWithARetiredSlide_isRefused() {
        assertNotChanged("pathology.error.requestResultRetired", slideRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, retiredSlideA12));
    }

    @Test
    public void anOpenRequest_completedWithTheOtherKindsField_isRefused() {
        assertNotChanged("pathology.error.requestResultWrongKind", stainRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, blockA1, null));
        assertNotChanged("pathology.error.requestResultWrongKind", slideRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, blockA1, slideA11));
        assertNotChanged("pathology.error.requestResultWrongKind", blockRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
    }

    @Test
    public void anOpenRequest_cancelledWithAReason_keepsTheReasonTrimmed() {
        changeStatus(slideRequest,
                new RequestStatusChange("OPENED", "CANCELLED", "  done on another case  ", null, null));

        Map<String, Object> row = requestRow(slideRequest);
        assertEquals("CANCELLED", row.get("status"));
        assertEquals("done on another case", row.get("cancel_reason"));
        assertEquals(changed("OPENED", "CANCELLED", "reason", "done on another case"),
                payloadOf(onlyStatusEntry(slideRequest)));
    }

    @Test
    public void anOpenRequest_cancelledWithABlankReason_isRefused() {
        assertNotChanged("pathology.error.requestCancelReasonRequired", slideRequest,
                new RequestStatusChange("OPENED", "CANCELLED", "   ", null, null));
    }

    /**
     * The reason fits its column, and one of exactly the column's width is taken.
     */
    @Test
    public void aCancelReason_fitsItsColumn() {
        assertNotChanged("pathology.error.reasonTooLong", slideRequest,
                new RequestStatusChange("OPENED", "CANCELLED", StringUtils.repeat("r", 256), null, null));

        changeStatus(slideRequest,
                new RequestStatusChange("OPENED", "CANCELLED", StringUtils.repeat("r", 255), null, null));

        assertEquals(StringUtils.repeat("r", 255), requestRow(slideRequest).get("cancel_reason"));
    }

    @Test
    public void anOpenRequest_postedOpenAgain_isLeftAsItIs() {
        Object version = requestRow(slideRequest).get("last_updated");

        PathologyRequest unchanged = changeStatus(slideRequest, "OPENED", "OPENED");

        assertEquals(RequestStatus.OPENED, unchanged.getStatus());
        assertNothingWritten(slideRequest, version);
    }

    @Test
    public void aCompletedRequest_reopened_isOpenWithWhatFulfilledItCleared() {
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
        clearRequestAudit();
        clearCaseAudit();

        changeStatus(stainRequest, "COMPLETED", "OPENED");

        Map<String, Object> row = requestRow(stainRequest);
        assertEquals("OPENED", row.get("status"));
        assertNull("the result is cleared", row.get("result_slide_id"));
        assertEquals(changed("COMPLETED", "OPENED"), payloadOf(onlyStatusEntry(stainRequest)));
        List<List<String>> lines = requestLines();
        assertEquals("one line about the requests", 1, lines.size());
        assertTrue("the old value says what was undone, " + lines.get(0).get(0),
                lines.get(0).get(0).contains("Status: Completed, Cancel reason: , Fulfilled by: A1.1"));
    }

    @Test
    public void aCompletedRequest_cancelled_isRefused() {
        changeStatus(blockRequest, "OPENED", "COMPLETED");
        clearRequestAudit();
        clearCaseAudit();

        assertNotChanged("pathology.error.requestTransitionNotAllowed", blockRequest,
                new RequestStatusChange("COMPLETED", "CANCELLED", "not needed", null, null));
        assertEquals("COMPLETED", requestRow(blockRequest).get("status"));
    }

    /**
     * Completing again is left as it is, even naming another result: a wrong
     * "Fulfilled by" is put right by reopening and completing again.
     */
    @Test
    public void aCompletedRequest_completedAgain_isLeftAsItIsWhateverResultIsPosted() {
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
        clearRequestAudit();
        clearCaseAudit();
        Object version = requestRow(stainRequest).get("last_updated");

        changeStatus(stainRequest, new RequestStatusChange("COMPLETED", "COMPLETED", null, null, retiredSlideA12));

        assertEquals("the first result stands", slideA11,
                ((Number) requestRow(stainRequest).get("result_slide_id")).intValue());
        assertNothingWritten(stainRequest, version);
    }

    @Test
    public void aCancelledRequest_reopened_isOpenWithItsReasonCleared() {
        changeStatus(slideRequest, new RequestStatusChange("OPENED", "CANCELLED", "duplicate", null, null));
        clearRequestAudit();

        changeStatus(slideRequest, "CANCELLED", "OPENED");

        Map<String, Object> row = requestRow(slideRequest);
        assertEquals("OPENED", row.get("status"));
        assertNull("the reason is cleared", row.get("cancel_reason"));
        assertEquals(changed("CANCELLED", "OPENED"), payloadOf(onlyStatusEntry(slideRequest)));
    }

    @Test
    public void aCancelledRequest_completed_isRefused() {
        changeStatus(slideRequest, new RequestStatusChange("OPENED", "CANCELLED", "duplicate", null, null));
        clearRequestAudit();
        clearCaseAudit();

        assertNotChanged("pathology.error.requestTransitionNotAllowed", slideRequest,
                new RequestStatusChange("CANCELLED", "COMPLETED", null, null, slideA11));
    }

    @Test
    public void aCancelledRequest_cancelledAgainWithAReason_isLeftAsItIs() {
        changeStatus(slideRequest, new RequestStatusChange("OPENED", "CANCELLED", "duplicate", null, null));
        clearRequestAudit();
        clearCaseAudit();
        Object version = requestRow(slideRequest).get("last_updated");

        changeStatus(slideRequest, new RequestStatusChange("CANCELLED", "CANCELLED", "another reason", null, null));

        assertEquals("the first reason stands", "duplicate", requestRow(slideRequest).get("cancel_reason"));
        assertNothingWritten(slideRequest, version);
    }

    @Test
    public void aCancelledRequest_cancelledAgainWithABlankReason_isRefused() {
        changeStatus(slideRequest, new RequestStatusChange("OPENED", "CANCELLED", "duplicate", null, null));
        clearRequestAudit();
        clearCaseAudit();

        assertNotChanged("pathology.error.requestCancelReasonRequired", slideRequest,
                new RequestStatusChange("CANCELLED", "CANCELLED", " ", null, null));
    }

    /** A reason posted with a completion or a reopening is not what they take. */
    @Test
    public void aReasonPostedWithACompletionOrAReopening_isIgnored() {
        changeStatus(blockRequest, new RequestStatusChange("OPENED", "COMPLETED", "ignored", null, null));
        assertNull(requestRow(blockRequest).get("cancel_reason"));

        changeStatus(blockRequest, new RequestStatusChange("COMPLETED", "OPENED", "ignored too", null, null));
        assertNull(requestRow(blockRequest).get("cancel_reason"));
    }

    @Test
    public void anyChange_onASignedOutCase_isRefused() {
        changeStatus(blockRequest, "OPENED", "COMPLETED");
        clearRequestAudit();
        clearCaseAudit();
        storeAt(PathologyStatus.COMPLETED);

        assertNotChanged("pathology.error.caseCompleted", slideRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, null));
        assertNotChanged("pathology.error.caseCompleted", slideRequest,
                new RequestStatusChange("OPENED", "CANCELLED", "too late", null, null));
        assertNotChanged("pathology.error.caseCompleted", blockRequest,
                new RequestStatusChange("COMPLETED", "OPENED", null, null, null));
    }

    /**
     * A request on file has no kind, so nothing can be said to fulfil it; result
     * ids posted with its completion are ignored rather than refused.
     */
    @Test
    public void aRequestOnFile_completedWithResultIds_isCompletedWithoutThem() {
        changeStatus(FIXTURE_REQUEST_ID, new RequestStatusChange("OPENED", "COMPLETED", null, blockA1, slideA11));

        Map<String, Object> row = requestRow(FIXTURE_REQUEST_ID);
        assertEquals("COMPLETED", row.get("status"));
        assertNull(row.get("result_block_id"));
        assertNull(row.get("result_slide_id"));
        assertEquals(changed("OPENED", "COMPLETED"), payloadOf(onlyStatusEntry(FIXTURE_REQUEST_ID)));
    }

    // ---- the page that shows a status the request has left ----

    /**
     * Tab A still shows the request cancelled while tab B reopened and completed
     * it. Tab A's reopen would clear what fulfilled it; it is a conflict instead,
     * and the result is kept.
     */
    @Test
    public void aPageShowingAStatusTheRequestHasLeft_isAConflictAndTheResultIsKept() {
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "CANCELLED", "duplicate", null, null));
        changeStatus(stainRequest, "CANCELLED", "OPENED");
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
        clearRequestAudit();
        clearCaseAudit();
        Object version = requestRow(stainRequest).get("last_updated");

        PathologyRequestConflictException conflict = assertThrows(PathologyRequestConflictException.class,
                () -> changeStatus(stainRequest, "CANCELLED", "OPENED"));

        assertSame("the conflict names the status the request holds", RequestStatus.COMPLETED,
                conflict.getStoredStatus());
        assertEquals("COMPLETED", requestRow(stainRequest).get("status"));
        assertEquals("what fulfilled it is kept", slideA11,
                ((Number) requestRow(stainRequest).get("result_slide_id")).intValue());
        assertNothingWritten(stainRequest, version);
    }

    // ---- the refusals, in the order they fire (FR-10.8, FR-10.9) ----

    /**
     * A change breaking every rule is refused for the first rule it breaks, and
     * each fix uncovers the next; nothing is written for any of them.
     */
    @Test
    public void aChange_isRefusedRuleByRuleInOrder_writingNothing() {
        storeAt(PathologyStatus.COMPLETED);
        String tooLong = StringUtils.repeat("r", 256);

        assertNotChanged("pathology.error.requestStatusRequired", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("OPENED", null, " ", null, null));
        assertNotChanged("pathology.error.requestStatusRequired", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("OPENED", "DONE", " ", null, null));
        assertNotChanged("pathology.error.requestStatusRequired", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange(null, "CANCELLED", " ", null, null));
        assertNotChanged("pathology.error.requestStatusRequired", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("SHUT", "CANCELLED", " ", null, null));
        assertNotChanged("pathology.error.requestCancelReasonRequired", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("COMPLETED", "CANCELLED", " ", null, null));
        assertNotChanged("pathology.error.reasonTooLong", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("COMPLETED", "CANCELLED", tooLong, null, null));
        assertNotChanged("pathology.error.caseCompleted", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("COMPLETED", "CANCELLED", "duplicate", null, null));

        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertNotChanged("pathology.error.requestNotOnCase", REQUEST_OF_ANOTHER_CASE,
                new RequestStatusChange("COMPLETED", "CANCELLED", "duplicate", null, null));
        assertThrows("a status the request has left is a conflict, ahead of the transition rules",
                PathologyRequestConflictException.class, () -> changeStatus(stainRequest,
                        new RequestStatusChange("COMPLETED", "CANCELLED", "duplicate", blockA1, null)));
        Object version = requestRow(stainRequest).get("last_updated");
        changeStatus(stainRequest, new RequestStatusChange("OPENED", "OPENED", null, blockA1, retiredSlideA12));
        assertNothingWritten(stainRequest, version);

        changeStatus(blockRequest, "OPENED", "COMPLETED");
        clearRequestAudit();
        clearCaseAudit();
        assertNotChanged("pathology.error.requestTransitionNotAllowed", blockRequest,
                new RequestStatusChange("COMPLETED", "CANCELLED", "duplicate", null, LEGACY_SLIDE_ID));

        assertNotChanged("pathology.error.requestResultWrongKind", stainRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, BLOCK_OF_ANOTHER_CASE, retiredSlideA12));
        assertNotChanged("pathology.error.requestResultNotOnCase", stainRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, LEGACY_SLIDE_ID));
        assertNotChanged("pathology.error.requestResultRetired", stainRequest,
                new RequestStatusChange("OPENED", "COMPLETED", null, null, retiredSlideA12));

        changeStatus(stainRequest, new RequestStatusChange("OPENED", "COMPLETED", null, null, slideA11));
        assertEquals("with every rule met it is completed", "COMPLETED", requestRow(stainRequest).get("status"));
    }

    /** No request id names no request on the case. */
    @Test
    public void aChangeNamingNoRequest_isNotOnTheCase() {
        assertRefusedWith("pathology.error.requestNotOnCase", () -> pathologySampleService.changeRequestStatus(CASE_ID,
                null, new RequestStatusChange("OPENED", "COMPLETED", null, null, null), TECHNICIAN_ID));
    }

    // ---- a request on file with no version ----

    /**
     * A request inserted by SQL with no last_updated has no version to check, so it
     * could never be changed; the upgrade gives it one, after which it completes.
     */
    @Test
    public void aRequestOnFileWithNoVersion_isCompletedOnceTheUpgradeGivesItOne() {
        insertRequestWithNoVersion();

        jdbcTemplate.execute(LAST_UPDATED_BACKFILL);
        changeStatus(REQUEST_WITH_NO_VERSION, "OPENED", "COMPLETED");

        assertEquals("COMPLETED", requestRow(REQUEST_WITH_NO_VERSION).get("status"));
    }

    /** Without the upgrade, the same completion loses its version check. */
    @Test
    public void aRequestOnFileWithNoVersion_withoutTheUpgrade_cannotBeChanged() {
        insertRequestWithNoVersion();

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> changeStatus(REQUEST_WITH_NO_VERSION, "OPENED", "COMPLETED"));

        assertTrue("the failure is the version check, not " + failure, StaleSaveGuard.isOptimisticLockFailure(failure));
        assertEquals("OPENED", requestRow(REQUEST_WITH_NO_VERSION).get("status"));
    }

    // ---- three completions at once ----

    /**
     * Three pages complete the same request at the same moment. One commits; the
     * others find it already completed or lose the case's version check, and write
     * nothing.
     */
    @Test
    public void threeConcurrentCompletions_leaveOneStatusEntry() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<PathologyRequest>> completions = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Callable<PathologyRequest> call = () -> {
                    startGate.await();
                    return changeStatus(blockRequest, "OPENED", "COMPLETED");
                };
                completions.add(pool.submit(call));
            }
            startGate.countDown();

            int won = 0;
            for (Future<PathologyRequest> call : completions) {
                try {
                    call.get(60, TimeUnit.SECONDS);
                    won++;
                } catch (ExecutionException e) {
                    assertTrue(
                            "a losing completion is the request conflict or a lost version check, not " + e.getCause(),
                            e.getCause() instanceof PathologyRequestConflictException
                                    || StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertEquals("exactly one completion commits", 1, won);
            assertEquals("COMPLETED", requestRow(blockRequest).get("status"));
            assertEquals("and exactly one is audited", 1, statusEntries(blockRequest).size());
            assertEquals("with one diff entry on the case", 1, caseSaveDiffs().size());
        } finally {
            pool.shutdownNow();
        }
    }

    // helpers

    /**
     * What 010's backfill does to a request with no version. The upgrade test
     * covers the changeset itself; this pins how a request behaves after it.
     */
    private static final String LAST_UPDATED_BACKFILL = "UPDATE clinlims.pathology_request SET last_updated = now()"
            + " WHERE last_updated IS NULL";

    private void insertRequestWithNoVersion() {
        jdbcTemplate.update(
                "INSERT INTO clinlims.pathology_request (id, pathology_sample_id, status, type, value,"
                        + " last_updated) VALUES (?, ?, 'OPENED', 'TEXT', 'Histology review', NULL)",
                REQUEST_WITH_NO_VERSION, CASE_ID);
    }

    private void clearRequestAudit() {
        jdbcTemplate.update("DELETE FROM clinlims.history WHERE reference_table = ?",
                Integer.parseInt(requestReferenceTableId));
    }

    private List<History> statusEntries(int requestId) {
        return requestEntries(requestId, PathologySampleService.PATHOLOGY_REQUEST_STATUS_CHANGED);
    }

    private History onlyStatusEntry(int requestId) {
        List<History> entries = statusEntries(requestId);
        assertEquals("one status entry on request " + requestId, 1, entries.size());
        return entries.get(0);
    }

    /** A STATUS_CHANGED payload with the verb and the two statuses first. */
    private static Map<String, Object> changed(String from, String to, Object... pairs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PathologySampleService.PATHOLOGY_REQUEST_STATUS_CHANGED);
        payload.put("statusFrom", from);
        payload.put("statusTo", to);
        for (int i = 0; i < pairs.length; i += 2) {
            payload.put((String) pairs[i], pairs[i + 1]);
        }
        return payload;
    }

    /** Refused with the message id, the request as it was, nothing audited. */
    private void assertNotChanged(String errorCode, int requestId, RequestStatusChange change) {
        Map<String, Object> before = requestRow(requestId);
        assertRefusedWith(errorCode, () -> changeStatus(requestId, change));
        assertEquals("the request is left as it was after " + errorCode, before, requestRow(requestId));
        assertEquals("no entry is written", 0, requestAuditCount());
        assertTrue("nor a case history row", caseSaveDiffs().isEmpty());
    }

    private void assertNothingWritten(int requestId, Object versionBefore) {
        assertEquals("the row is not written", versionBefore, requestRow(requestId).get("last_updated"));
        assertEquals("no entry is written", 0, requestAuditCount());
        assertTrue("nor a case history row", caseSaveDiffs().isEmpty());
    }
}
