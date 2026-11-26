package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
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
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.RequestDraft;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * A pathologist raises a bench request from the case view: an additional block,
 * a slide cut from a named block, or a stain on a named slide, with a priority
 * and an instruction (FR-10.3 to FR-10.7, FR-17).
 *
 * <p>
 * Before this a request was a pick from a list of 22 stain names, written again
 * by every save of the case, so it had no stable id, no author, no date and no
 * target. These tests pin what a raised request stores, who and when it is
 * stamped with, the one audit entry and the one case history line it leaves,
 * every refusal in the order it fires with nothing written, and three requests
 * raised at once.
 */
public class PathologyRequestRaiseTest extends PathologyCaseTestSupport {

    /** This class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_STAIN_CATEGORY = 9751;
    private static final int HE = 9752;
    private static final int GIEMSA_RETIRED = 9753;
    private static final int OTHER_CATEGORY = 9754;
    private static final int ENTRY_OF_ANOTHER_CATEGORY = 9755;

    /** A block on the fixture's other case. */
    private static final int BLOCK_OF_ANOTHER_CASE = 9761;

    private StageSwitches switches;
    private DictionaryRows.StainEntries stains;
    private boolean addedOtherCategory;

    /** Block A1 in use, with slide A1.1 in use and A1.2 retired. */
    private int blockA1;
    private int slideA11;
    private int retiredSlideA12;

    /** Block A2, retired. */
    private int retiredBlockA2;

    /**
     * Case 1 at Microtomy with A1, A1.1, a retired slide and a retired block. The
     * fixture's own slide moves to case 2, where it serves as a slide of another
     * case.
     */
    @Before
    public void cutBlocksAndSlides() {
        switches = StageSwitches.allOn();
        stains = DictionaryRows.stainEntries(jdbcTemplate, OWN_STAIN_CATEGORY).with(HE, "Haematoxylin and eosin", true)
                .with(GIEMSA_RETIRED, "Giemsa", false);
        addedOtherCategory = DictionaryRows.insertCategory(jdbcTemplate, OTHER_CATEGORY, "Not stain category",
                "Not stain category", "NotStain");
        DictionaryRows.removeEntries(jdbcTemplate, ENTRY_OF_ANOTHER_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, ENTRY_OF_ANOTHER_CATEGORY,
                DictionaryRows.idOfCategory(jdbcTemplate, "Not stain category"), "Not a stain", "NotS", true);
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
        saveBlocks(postedBlock(null, "Bench B"));
        retiredBlockA2 = idOfBlockDesignated("A2");
        pathologySampleService.deactivateBlock(retiredBlockA2, "empty", TECHNICIAN_ID);
        clearCaseAudit();
    }

    @After
    public void tearDown() {
        switches.close();
        stains.close();
        jdbcTemplate.update("UPDATE clinlims.pathology_request SET stain_id = NULL WHERE stain_id = ?",
                ENTRY_OF_ANOTHER_CATEGORY);
        DictionaryRows.removeEntries(jdbcTemplate, ENTRY_OF_ANOTHER_CATEGORY);
        if (addedOtherCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, OTHER_CATEGORY);
        }
    }

    // ---- what a raised request stores ----

    /** A stain request is stored open, as text, stamped with who and when. */
    @Test
    public void aStainRequest_isStoredOpenWithItsSlideStainPriorityAuthorAndTime() {
        Timestamp before = now();

        PathologyRequest raised = raise(
                new RequestDraft("STAIN", null, slideA11, String.valueOf(HE), "URGENT", "  PAS on A1.1  "));

        Map<String, Object> row = requestRow(raised.getId());
        assertEquals("a request just raised is open", "OPENED", row.get("status"));
        assertEquals("its instruction is text, so a case save never rewrites it", "TEXT", row.get("type"));
        assertEquals("the instruction is kept trimmed", "PAS on A1.1", row.get("value"));
        assertEquals("STAIN", row.get("request_kind"));
        assertEquals("the slide it names", slideA11, ((Number) row.get("target_slide_id")).intValue());
        assertNull("a stain request names no block", row.get("target_block_id"));
        assertEquals("the stain it asks for", HE, ((Number) row.get("stain_id")).intValue());
        assertEquals("URGENT", row.get("priority"));
        assertEquals("raised by the session user", Integer.parseInt(TECHNICIAN_ID),
                ((Number) row.get("raised_by")).intValue());
        Timestamp at = (Timestamp) row.get("raised_at");
        assertTrue("at the server's time", !at.before(before) && !at.after(now()));
        assertNull("nothing fulfilled it yet", row.get("result_slide_id"));
        assertNull(row.get("cancel_reason"));
    }

    /**
     * A slide request keeps its block; a slide and a stain posted with it are not
     * what it asks for and are dropped, and a blank priority is Normal.
     */
    @Test
    public void aSlideRequest_keepsItsBlockAndDropsASlideOrStainPostedWithIt() {
        PathologyRequest raised = raise(
                new RequestDraft("SLIDE", blockA1, slideA11, String.valueOf(HE), "  ", "Deeper levels"));

        Map<String, Object> row = requestRow(raised.getId());
        assertEquals("SLIDE", row.get("request_kind"));
        assertEquals("the block the slide is cut from", blockA1, ((Number) row.get("target_block_id")).intValue());
        assertNull("no slide is kept", row.get("target_slide_id"));
        assertNull("nor a stain", row.get("stain_id"));
        assertEquals("a blank priority is Normal", "NORMAL", row.get("priority"));
    }

    /** A block request names nothing on the case; what was posted is dropped. */
    @Test
    public void aBlockRequest_keepsNoTargetAndNoStain() {
        PathologyRequest raised = raise(new RequestDraft("BLOCK", blockA1, slideA11, String.valueOf(HE), null,
                "Another block from the margin"));

        Map<String, Object> row = requestRow(raised.getId());
        assertEquals("BLOCK", row.get("request_kind"));
        assertNull(row.get("target_block_id"));
        assertNull(row.get("target_slide_id"));
        assertNull(row.get("stain_id"));
        assertEquals("no priority is Normal", "NORMAL", row.get("priority"));
    }

    /** An instruction of exactly the column's width is taken. */
    @Test
    public void anInstructionOfTheColumnsWidth_isTaken() {
        PathologyRequest raised = raise(blockRequest(StringUtils.repeat("x", 255)));

        assertEquals(StringUtils.repeat("x", 255), requestRow(raised.getId()).get("value"));
    }

    /** No role is asked for, and any stage before sign-out takes a request. */
    @Test
    public void aRequest_isTakenAtAnyStageBeforeSignOut() {
        for (PathologyStatus stage : List.of(PathologyStatus.GROSSING, PathologyStatus.COVERSLIPPING,
                PathologyStatus.READY_PATHOLOGIST, PathologyStatus.UNDER_REVIEW)) {
            storeAt(stage);
            raise(blockRequest("Another block, raised at " + stage));
        }

        assertEquals("four raised beside the fixture's own", 5, requestsOnCase().size());
        assertEquals("and raising never moves the case", "UNDER_REVIEW", storedStage());
    }

    // ---- what a raised request leaves behind (FR-17) ----

    /**
     * One entry per request on the request itself, naming the kind, the priority,
     * the target by id and as printed, and the stain; nothing about the patient.
     */
    @Test
    public void raising_leavesOneEntryNamingWhatWasAskedFor() {
        PathologyRequest stain = raise(stainRequest(slideA11, String.valueOf(HE), "PAS on A1.1"));
        PathologyRequest slide = raise(slideRequest(blockA1, "Deeper levels"));
        PathologyRequest block = raise(blockRequest("Another block"));

        History stainEntry = onlyEntry(stain.getId(), PathologySampleService.PATHOLOGY_REQUEST_RAISED);
        assertEquals("recorded as an insert", "I", stainEntry.getActivity());
        assertEquals("by the session user", TECHNICIAN_ID, stainEntry.getSysUserId());
        assertEquals(payload("kind", "STAIN", "priority", "NORMAL", "targetSlideId", slideA11, "target", "A1.1",
                "stainId", String.valueOf(HE)), payloadOf(stainEntry));
        assertEquals(payload("kind", "SLIDE", "priority", "NORMAL", "targetBlockId", blockA1, "target", "A1"),
                payloadOf(onlyEntry(slide.getId(), PathologySampleService.PATHOLOGY_REQUEST_RAISED)));
        assertEquals("a block request carries no target and no stain", payload("kind", "BLOCK", "priority", "NORMAL"),
                payloadOf(onlyEntry(block.getId(), PathologySampleService.PATHOLOGY_REQUEST_RAISED)));
    }

    /**
     * The order's audit trail reads the raise as one change of the case's requests,
     * from what the case held to what it holds now, naming the request.
     */
    @Test
    public void raising_readsAsOneRequestsLineOnTheCaseHistory() {
        raise(slideRequest(blockA1, "Deeper levels"));

        List<List<String>> lines = requestLines();
        assertEquals("one line about the requests", 1, lines.size());
        assertTrue("the old value is the request on file, " + lines.get(0).get(0),
                lines.get(0).get(0).contains("Request: Histology review"));
        assertTrue("the new value names the request, its kind and its target, " + lines.get(0).get(1),
                lines.get(0).get(1).contains("Request: Deeper levels, Kind: SLIDE, Target: A1, Stain: , Priority:"
                        + " NORMAL, Status: Opened"));
        assertEquals("from one diff entry on the case", 1, caseSaveDiffs().size());
    }

    // ---- the refusals, in the order they fire (FR-10.2 to FR-10.7) ----

    /**
     * A stain request breaking every rule is refused for the first rule it breaks,
     * and each fix uncovers the next; nothing is written for any of them.
     */
    @Test
    public void aStainRequest_isRefusedRuleByRuleInOrder_writingNothing() {
        storeAt(PathologyStatus.COMPLETED);
        String tooLong = StringUtils.repeat("x", 256);

        assertNotRaised("pathology.error.requestKindRequired", new RequestDraft(null, null, null, null, "SOON", " "));
        assertNotRaised("pathology.error.requestKindRequired",
                new RequestDraft("SPECIMEN", null, null, null, "SOON", " "));
        assertNotRaised("pathology.error.requestInstructionRequired",
                new RequestDraft("STAIN", null, null, null, "SOON", " "));
        assertNotRaised("pathology.error.requestInstructionTooLong",
                new RequestDraft("STAIN", null, null, null, "SOON", tooLong));
        assertNotRaised("pathology.error.requestPriorityUnknown",
                new RequestDraft("STAIN", null, null, null, "SOON", "PAS"));
        assertNotRaised("pathology.error.caseCompleted", new RequestDraft("STAIN", null, null, null, "STAT", "PAS"));

        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertNotRaised("pathology.error.requestTargetRequired",
                new RequestDraft("STAIN", blockA1, null, null, "STAT", "PAS"));
        assertNotRaised("pathology.error.requestTargetNotOnCase",
                new RequestDraft("STAIN", null, LEGACY_SLIDE_ID, null, "STAT", "PAS"));
        assertNotRaised("pathology.error.requestTargetRetired",
                new RequestDraft("STAIN", null, retiredSlideA12, null, "STAT", "PAS"));
        assertNotRaised("pathology.error.requestStainRequired",
                new RequestDraft("STAIN", null, slideA11, "  ", "STAT", "PAS"));
        assertNotRaised("pathology.error.requestStainNotOnList",
                new RequestDraft("STAIN", null, slideA11, String.valueOf(ENTRY_OF_ANOTHER_CATEGORY), "STAT", "PAS"));
        assertEquals("a retired stain is refused by its name", Map.of("name", "Giemsa"),
                assertNotRaised("pathology.error.requestStainRetired",
                        new RequestDraft("STAIN", null, slideA11, String.valueOf(GIEMSA_RETIRED), "STAT", "PAS"))
                        .getParams());

        raise(new RequestDraft("STAIN", null, slideA11, String.valueOf(HE), "STAT", "PAS"));
        assertEquals("with every rule met it is raised", 2, requestsOnCase().size());
    }

    /** A slide request needs a block of the case that is in use. */
    @Test
    public void aSlideRequest_isRefusedWithoutABlockOfTheCaseInUse() {
        assertNotRaised("pathology.error.requestTargetRequired",
                new RequestDraft("SLIDE", null, slideA11, null, null, "Deeper levels"));
        assertNotRaised("pathology.error.requestTargetNotOnCase", slideRequest(BLOCK_OF_ANOTHER_CASE, "Deeper levels"));
        assertNotRaised("pathology.error.requestTargetRetired", slideRequest(retiredBlockA2, "Deeper levels"));
    }

    /**
     * A stain id that names nothing, or is not an id at all, is not on the list.
     */
    @Test
    public void aStainThatNamesNoEntry_isNotOnTheList() {
        assertNotRaised("pathology.error.requestStainNotOnList", stainRequest(slideA11, "987654", "PAS"));
        assertNotRaised("pathology.error.requestStainNotOnList", stainRequest(slideA11, "H&E", "PAS"));
    }

    // ---- three requests at once ----

    /**
     * Three pages raise a request on the same case at the same moment. Each one
     * that commits leaves its row and its entry; one that loses the case's version
     * check is answered as a conflict, never a server error, and leaves nothing.
     */
    @Test
    public void threeConcurrentRaises_leaveARowAndAnEntryForEachThatCommits() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<PathologyRequest>> raises = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                String instruction = "Deeper levels, page " + i;
                Callable<PathologyRequest> call = () -> {
                    startGate.await();
                    return raise(slideRequest(blockA1, instruction));
                };
                raises.add(pool.submit(call));
            }
            startGate.countDown();

            List<Integer> committed = new ArrayList<>();
            for (Future<PathologyRequest> call : raises) {
                try {
                    committed.add(call.get(60, TimeUnit.SECONDS).getId());
                } catch (ExecutionException e) {
                    assertTrue("a losing raise is a lost version check, not " + e.getCause(),
                            StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertTrue("at least one raise commits", !committed.isEmpty());
            assertEquals("one row per raise that committed, beside the fixture's own", committed.size() + 1,
                    requestsOnCase().size());
            for (Integer requestId : committed) {
                assertEquals("one entry per raise that committed", 1,
                        requestEntries(requestId, PathologySampleService.PATHOLOGY_REQUEST_RAISED).size());
            }
            assertEquals("and no entry for one that lost", committed.size(), requestAuditCount());
        } finally {
            pool.shutdownNow();
        }
    }

    // helpers

    /** Refused with the message id, leaving no row, no entry and no history. */
    private LocalizedValidationException assertNotRaised(String errorCode, RequestDraft draft) {
        int requestsBefore = requestsOnCase().size();
        LocalizedValidationException refusal = assertRefusedWith(errorCode, () -> raise(draft));
        assertEquals("no request is written for " + errorCode, requestsBefore, requestsOnCase().size());
        assertEquals("nor an entry", 0, requestAuditCount());
        assertTrue("nor a case history row", caseSaveDiffs().isEmpty());
        return refusal;
    }

    private History onlyEntry(int requestId, String verb) {
        List<History> entries = requestEntries(requestId, verb);
        assertEquals("one entry on request " + requestId, 1, entries.size());
        assertNotNull(entries.get(0));
        return entries.get(0);
    }

    /** A RAISED payload with the verb first and the pairs given. */
    private static Map<String, Object> payload(Object... pairs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("verb", PathologySampleService.PATHOLOGY_REQUEST_RAISED);
        for (int i = 0; i < pairs.length; i += 2) {
            payload.put((String) pairs[i], pairs[i + 1]);
        }
        return payload;
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
