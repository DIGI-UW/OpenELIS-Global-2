package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * Staining records on each slide whether its stain took, with the session user
 * and the server clock, one slide or a batch alike (FR-8.1, FR-8.2, FR-8.3).
 *
 * <p>
 * The stain status column was on every slide row and nothing wrote it, so a
 * case went from Staining to the pathologist with no record of which slides
 * were stained or by whom. These tests pin the transition table of the status
 * (pending, stained, unsatisfactory, by stage), the stain frozen while a slide
 * reads stained and free again once it is found unsatisfactory, the coverslip
 * check cleared by any change of status, one audit entry per slide changed and
 * one case history row per request carrying the old values, nothing written by
 * a request that changes nothing, every refusal leaving the whole batch as it
 * was, and three identical requests at once leaving one entry per slide.
 */
public class PathologySampleStainingTest extends PathologyCaseTestSupport {

    /** This class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_CATEGORY = 9650;
    private static final int HE = 9651;
    private static final int PAS = 9652;

    private StageSwitches switches;

    private DictionaryRows.StainEntries stains;

    /**
     * Three slides cut from block A1, each carrying H&E, on a case standing at
     * Staining. The fixture's own slide carries no stain and is left out unless a
     * test names it.
     */
    private List<Integer> slides;

    @Before
    public void cutThreeSlidesAndStandAtStaining() {
        switches = StageSwitches.allOn();
        stains = DictionaryRows.stainEntries(jdbcTemplate, OWN_CATEGORY).with(HE, "Haematoxylin and eosin", true)
                .with(PAS, "Periodic acid-Schiff", true);
        storeAt(PathologyStatus.MICROTOMY);
        slides = cutSlidesCarrying(3, String.valueOf(HE));
        storeAt(PathologyStatus.STAINING);
        clearSlideAudit();
        clearCaseAudit();
    }

    @After
    public void tearDown() {
        switches.close();
        stains.close();
    }

    // ---- the transition table: stored status x the status recorded ----

    @Test
    public void aPendingSlide_markedStained_readsStainedWithWhoAndWhen() {
        Timestamp before = now();

        stain(StainStatus.STAINED, first());

        Map<String, Object> work = slideWork(first());
        assertEquals("the slide reads stained", "STAINED", work.get("stain_status"));
        assertEquals("recorded by the session user", Integer.parseInt(TECHNICIAN_ID),
                ((Number) work.get("stain_status_by")).intValue());
        Timestamp at = (Timestamp) work.get("stain_status_at");
        assertTrue("at the server's time, not one posted", !at.before(before) && !at.after(now()));
        assertEquals("one audit entry, naming the stain applied and the status", List.of(Map.of("verb",
                PathologySampleService.PATHOLOGY_SLIDE_STAINED, "stainId", String.valueOf(HE), "status", "STAINED")),
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED));
    }

    @Test
    public void aPendingSlide_markedUnsatisfactory_readsUnsatisfactory() {
        stain(StainStatus.UNSATISFACTORY, first());

        assertEquals("a stain that did not take is recorded as such", "UNSATISFACTORY", statusOf(first()));
        assertEquals("under the same verb, with the status saying which", "UNSATISFACTORY",
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).get(0).get("status"));
    }

    @Test
    public void aStainedSlide_markedUnsatisfactory_readsUnsatisfactory() {
        stain(StainStatus.STAINED, first());

        stain(StainStatus.UNSATISFACTORY, first());

        assertEquals("a slide found unreadable after it was marked stained is recorded as such", "UNSATISFACTORY",
                statusOf(first()));
        assertEquals("each change is its own entry", 2,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
    }

    @Test
    public void anUnsatisfactorySlide_restained_readsStained() {
        stain(StainStatus.UNSATISFACTORY, first());

        stain(StainStatus.STAINED, first());

        assertEquals("a restain is recorded as stained", "STAINED", statusOf(first()));
    }

    @Test
    public void aStainedSlide_markedStainedAgain_writesNothing() {
        stain(StainStatus.STAINED, first());
        Object firstStamp = slideWork(first()).get("stain_status_at");

        stain(StainStatus.STAINED, first());

        assertEquals("the first stamp stands", firstStamp, slideWork(first()).get("stain_status_at"));
        assertEquals("and no second entry is written", 1,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
        assertEquals("nor a second case history row", 1, caseSaveDiffs().size());
    }

    @Test
    public void anUnsatisfactorySlide_markedUnsatisfactoryAgain_writesNothing() {
        stain(StainStatus.UNSATISFACTORY, first());

        stain(StainStatus.UNSATISFACTORY, first());

        assertEquals("one entry for the one change", 1,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
    }

    // ---- by stage ----

    @Test
    public void beforeStaining_isRefused() {
        storeAt(PathologyStatus.MICROTOMY);

        assertRefusedWith("pathology.error.stainBeforeStage", () -> stain(StainStatus.STAINED, first()));
        assertNothingRecorded();
    }

    @Test
    public void afterStaining_isStillRecorded() {
        storeAt(PathologyStatus.COVERSLIPPING);

        stain(StainStatus.UNSATISFACTORY, first());

        assertEquals("a slide found unreadable at coverslipping is still recorded", "UNSATISFACTORY",
                statusOf(first()));
    }

    /**
     * With the pathologist the case is off the bench: a slide found unreadable
     * sends it back with the return's reason, so a mark there is refused and the
     * slide keeps what it had, its coverslip check included.
     */
    @Test
    public void inThePathologistsQueue_isRefusedAndTheSlideKeepsItsCheck() {
        assertRefusedAfterTheBench(PathologyStatus.READY_PATHOLOGIST);
    }

    @Test
    public void underReview_isRefusedAndTheSlideKeepsItsCheck() {
        assertRefusedAfterTheBench(PathologyStatus.UNDER_REVIEW);
    }

    @Test
    public void onASignedOutCase_isRefused() {
        storeAt(PathologyStatus.COMPLETED);

        assertRefusedWith("pathology.error.caseCompleted", () -> stain(StainStatus.STAINED, first()));
        assertNothingRecorded();
    }

    // ---- the stain's editable window (FR-8.1: the stain on the row when marked is
    // the stain applied) ----

    @Test
    public void aPendingSlide_takesAChangedStainFromTheSave() {
        saveStain(first(), PAS);

        assertEquals("before marking, the stain is the bench's to correct", Integer.valueOf(PAS), stainOf(first()));
    }

    @Test
    public void aStainedSlide_keepsItsStainWhateverTheSavePosts() {
        stain(StainStatus.STAINED, first());

        saveStain(first(), PAS);

        assertEquals("the stain applied is what the slide was marked with", Integer.valueOf(HE), stainOf(first()));
    }

    @Test
    public void anUnsatisfactorySlide_takesAChangedStain_andTheRestainRecordsIt() {
        stain(StainStatus.UNSATISFACTORY, first());

        saveStain(first(), PAS);
        stain(StainStatus.STAINED, first());

        assertEquals("a slide found unsatisfactory may be restained with another stain", Integer.valueOf(PAS),
                stainOf(first()));
        List<Map<String, Object>> entries = slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED);
        assertEquals("and the restain's entry names the stain applied", String.valueOf(PAS),
                entries.get(entries.size() - 1).get("stainId"));
    }

    // ---- the coverslip check ----

    @Test
    public void aChangeOfStainStatus_clearsTheCoverslipCheck() {
        stain(StainStatus.STAINED, first());
        storeAt(PathologyStatus.COVERSLIPPING);
        pathologySampleService.recordCoverslipQc(CASE_ID, List.of(first()), CoverslipQc.PASS, TECHNICIAN_ID);

        stain(StainStatus.UNSATISFACTORY, first());

        Map<String, Object> work = slideWork(first());
        assertNull("a slide found unsatisfactory is checked again once restained", work.get("coverslip_qc"));
        assertNull("so who checked it", work.get("coverslip_qc_by"));
        assertNull("and when no longer speak for it", work.get("coverslip_qc_at"));
    }

    // ---- a batch (FR-8.2) ----

    @Test
    public void aBatchOfThree_writesThreeEntriesAndOneCaseHistoryRowWithTheOldValues() {
        stain(StainStatus.UNSATISFACTORY, slides.toArray(new Integer[0]));
        clearCaseAudit();

        stain(StainStatus.STAINED, slides.toArray(new Integer[0]));

        for (Integer slideId : slides) {
            assertEquals("every slide in the batch reads stained", "STAINED", statusOf(slideId));
            assertEquals("each slide has its own entry for the change", 2,
                    slideEntries(slideId, PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
        }
        List<History> diffs = caseSaveDiffs();
        assertEquals("the batch is one change to the case", 1, diffs.size());
        assertTrue("which carries the status it replaced: " + changesOf(diffs.get(0)),
                changesOf(diffs.get(0)).contains("Stain status: UNSATISFACTORY"));
    }

    @Test
    public void aBatch_answersTheNamedSlidesInTheCasesOrder() {
        List<PathologySlide> answered = stain(StainStatus.STAINED, slides.get(2), slides.get(0), slides.get(2));

        assertEquals("each slide once, in the order the case lists them", List.of(slides.get(0), slides.get(2)),
                answered.stream().map(PathologySlide::getId).toList());
    }

    // ---- refusals, each leaving every slide as it was ----

    @Test
    public void noSlideNamed_isRefused() {
        assertRefusedWith("pathology.error.slideSelectionRequired",
                () -> pathologySampleService.recordStainStatus(CASE_ID, List.of(), StainStatus.STAINED, TECHNICIAN_ID));
        assertRefusedWith("pathology.error.slideSelectionRequired",
                () -> pathologySampleService.recordStainStatus(CASE_ID, null, StainStatus.STAINED, TECHNICIAN_ID));
    }

    @Test
    public void noStatusNamed_isRefused() {
        assertRefusedWith("pathology.error.slideSelectionRequired",
                () -> pathologySampleService.recordStainStatus(CASE_ID, slides, null, TECHNICIAN_ID));
        assertNothingRecorded();
    }

    @Test
    public void aSlideOfAnotherCase_isRefusedAndTheBatchIsNotRecorded() {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET pathology_sample_id = 2 WHERE id = ?",
                LEGACY_SLIDE_ID);

        assertRefusedWith("pathology.error.slideNotOnCase", () -> stain(StainStatus.STAINED, first(), LEGACY_SLIDE_ID));
        assertNothingRecorded();
    }

    @Test
    public void aRetiredSlide_isRefusedAndTheBatchIsNotRecorded() {
        pathologySampleService.deactivateSlide(slides.get(1), "folded", TECHNICIAN_ID);
        clearSlideAudit();

        assertRefusedWith("pathology.error.slideRetired",
                () -> stain(StainStatus.STAINED, slides.toArray(new Integer[0])));
        assertNothingRecorded();
    }

    /**
     * One slide with no stain in a batch of three refuses the batch, naming that
     * slide, and none of the three is marked.
     */
    @Test
    public void aSlideWithNoStain_refusesTheWholeBatchNamingIt() {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = NULL WHERE id = ?", slides.get(2));

        LocalizedValidationException refusal = assertRefusedWith("pathology.error.stainMissing",
                () -> stain(StainStatus.STAINED, slides.toArray(new Integer[0])));

        assertEquals("the refusal names the slide as its label does", Map.of("designations", "A1.3"),
                refusal.getParams());
        assertNothingRecorded();
    }

    @Test
    public void aSlideWithNoStain_mayStillBeFoundUnsatisfactory() {
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = NULL WHERE id = ?", first());

        stain(StainStatus.UNSATISFACTORY, first());

        assertEquals("a slide that took no stain can be sent back", "UNSATISFACTORY", statusOf(first()));
        assertEquals("and its entry names no stain",
                Map.of("verb", PathologySampleService.PATHOLOGY_SLIDE_STAINED, "status", "UNSATISFACTORY"),
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).get(0));
    }

    // ---- three identical requests at once ----

    @Test
    public void threeConcurrentMarks_leaveOneEntryPerSlide() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<List<PathologySlide>>> marks = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Callable<List<PathologySlide>> mark = () -> {
                    startGate.await();
                    return stain(StainStatus.STAINED, slides.toArray(new Integer[0]));
                };
                marks.add(pool.submit(mark));
            }
            startGate.countDown();

            int answered = 0;
            for (Future<List<PathologySlide>> mark : marks) {
                try {
                    mark.get(60, TimeUnit.SECONDS);
                    answered++;
                } catch (ExecutionException e) {
                    assertTrue("a refused mark lost a slide's version check, not " + e.getCause(),
                            StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertTrue("at least one mark is answered", answered >= 1);
            for (Integer slideId : slides) {
                assertEquals("every slide reads stained", "STAINED", statusOf(slideId));
                assertEquals("with exactly one entry recording it", 1,
                        slideEntries(slideId, PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
            }
            assertEquals("and exactly one row in the case's history", 1, caseSaveDiffs().size());
        } finally {
            pool.shutdownNow();
        }
    }

    // helpers

    private void assertRefusedAfterTheBench(PathologyStatus stage) {
        stain(StainStatus.STAINED, first());
        storeAt(PathologyStatus.COVERSLIPPING);
        pathologySampleService.recordCoverslipQc(CASE_ID, List.of(first()), CoverslipQc.PASS, TECHNICIAN_ID);
        storeAt(stage);
        int entriesBefore = slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).size();
        int diffsBefore = caseSaveDiffs().size();

        assertRefusedWith("pathology.error.slideWorkAfterBench", () -> stain(StainStatus.UNSATISFACTORY, first()));

        Map<String, Object> work = slideWork(first());
        assertEquals("the slide still reads stained", "STAINED", work.get("stain_status"));
        assertEquals("and its coverslip check is not cleared", "PASS", work.get("coverslip_qc"));
        assertEquals("no entry is written", entriesBefore,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_STAINED).size());
        assertEquals("nor a case history row", diffsBefore, caseSaveDiffs().size());
    }

    private int first() {
        return slides.get(0);
    }

    private List<PathologySlide> stain(StainStatus status, Integer... slideIds) {
        return pathologySampleService.recordStainStatus(CASE_ID, List.of(slideIds), status, TECHNICIAN_ID);
    }

    /** Posts the slide back through the case save with another stain. */
    private void saveStain(int slideId, int stainId) {
        PathologySlideForm posted = postedSlide(null, String.valueOf(stainId));
        posted.setId(slideId);
        saveSlides(posted);
    }

    private String statusOf(int slideId) {
        return (String) slideWork(slideId).get("stain_status");
    }

    private Integer stainOf(int slideId) {
        Object stain = slideWork(slideId).get("stain_id");
        return stain == null ? null : ((Number) stain).intValue();
    }

    private void assertNothingRecorded() {
        for (Integer slideId : slides) {
            assertNull("no slide is marked", statusOf(slideId));
            assertNull("or stamped", slideWork(slideId).get("stain_status_at"));
            assertTrue("and no entry is written",
                    slideEntries(slideId, PathologySampleService.PATHOLOGY_SLIDE_STAINED).isEmpty());
        }
        assertTrue("nor a case history row", caseSaveDiffs().isEmpty());
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
