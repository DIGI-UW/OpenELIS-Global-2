package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.controller.pathology.PathologySampleForm.PathologySlideForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologyStageConflictException;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * Coverslipping records a quality check on each slide, pass or fail, with the
 * session user and the server clock, and the batch it went in as a run
 * reference on the case's Coverslipping entry (FR-8.4).
 *
 * <p>
 * Before this the coverslipping section was a placeholder and nothing on the
 * record said whether a slide passed its check. These tests pin the transition
 * table of the check by stage, a check refused on a slide not marked stained
 * wherever the laboratory tracks Staining and taken where it does not, one
 * audit entry per slide changed and one case history row per request carrying
 * the old values, every refusal leaving the batch as it was, three identical
 * requests at once leaving one entry per slide, and the batch reference written
 * once on the Coverslipping entry and ignored at any other stage.
 */
public class PathologySampleCoverslipQcTest extends PathologyCaseTestSupport {

    /** This class's own dictionary rows, outside the ranges fixtures use. */
    private static final int OWN_CATEGORY = 9655;
    private static final int HE = 9656;

    private StageSwitches switches;

    private DictionaryRows.StainEntries stains;

    /** Three slides cut from block A1, stained with H&E. */
    private List<Integer> slides;

    /**
     * Three stained slides on a case standing at Coverslipping. The fixture's own
     * slide, which carries no stain, is moved to the other case, where it serves as
     * a slide that is not this case's. The fixture's open request is closed, so the
     * case moves on to the pathologist on its slides alone.
     */
    @Before
    public void stainThreeSlidesAndStandAtCoverslipping() {
        switches = StageSwitches.allOn();
        closeTheFixtureRequest();
        stains = DictionaryRows.stainEntries(jdbcTemplate, OWN_CATEGORY).with(HE, "Haematoxylin and eosin", true);
        jdbcTemplate.update("UPDATE clinlims.pathology_slide SET pathology_sample_id = 2 WHERE id = ?",
                LEGACY_SLIDE_ID);
        storeAt(PathologyStatus.MICROTOMY);
        slides = cutSlidesCarrying(3, String.valueOf(HE));
        storeAt(PathologyStatus.STAINING);
        pathologySampleService.recordStainStatus(CASE_ID, slides, StainStatus.STAINED, TECHNICIAN_ID);
        storeAt(PathologyStatus.COVERSLIPPING);
        clearSlideAudit();
        clearCaseAudit();
    }

    @After
    public void tearDown() {
        switches.close();
        stains.close();
    }

    // ---- the transition table: stored result x the result recorded ----

    @Test
    public void anUncheckedSlide_passed_readsPassWithWhoAndWhen() {
        Timestamp before = now();

        check(CoverslipQc.PASS, first());

        Map<String, Object> work = slideWork(first());
        assertEquals("the slide passed its check", "PASS", work.get("coverslip_qc"));
        assertEquals("recorded by the session user", Integer.parseInt(TECHNICIAN_ID),
                ((Number) work.get("coverslip_qc_by")).intValue());
        Timestamp at = (Timestamp) work.get("coverslip_qc_at");
        assertTrue("at the server's time", !at.before(before) && !at.after(now()));
        assertEquals("one audit entry, naming the result",
                List.of(Map.of("verb", PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED, "result", "PASS")),
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED));
        assertEquals("and the stain status is left as it was", "STAINED", work.get("stain_status"));
    }

    @Test
    public void anUncheckedSlide_failed_readsFail() {
        check(CoverslipQc.FAIL, first());

        assertEquals("a failed check is recorded as such", "FAIL", resultOf(first()));
    }

    @Test
    public void aPassedSlide_failed_readsFail() {
        check(CoverslipQc.PASS, first());

        check(CoverslipQc.FAIL, first());

        assertEquals("a check may be corrected", "FAIL", resultOf(first()));
        assertEquals("each change is its own entry", 2,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED).size());
    }

    @Test
    public void aFailedSlide_passed_readsPass() {
        check(CoverslipQc.FAIL, first());

        check(CoverslipQc.PASS, first());

        assertEquals("a slide coverslipped again and passed reads pass", "PASS", resultOf(first()));
    }

    @Test
    public void aPassedSlide_passedAgain_writesNothing() {
        check(CoverslipQc.PASS, first());
        Object firstStamp = slideWork(first()).get("coverslip_qc_at");

        check(CoverslipQc.PASS, first());

        assertEquals("the first stamp stands", firstStamp, slideWork(first()).get("coverslip_qc_at"));
        assertEquals("one entry for the one change", 1,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED).size());
        assertEquals("and one case history row", 1, caseSaveDiffs().size());
    }

    // ---- by stage ----

    @Test
    public void beforeCoverslipping_isRefused() {
        storeAt(PathologyStatus.STAINING);

        assertRefusedWith("pathology.error.qcBeforeStage", () -> check(CoverslipQc.PASS, first()));
        assertNothingRecorded();
    }

    /** With the pathologist the case is off the bench, so no check is recorded. */
    @Test
    public void inThePathologistsQueue_isRefused() {
        assertRefusedAfterTheBench(PathologyStatus.READY_PATHOLOGIST);
    }

    @Test
    public void underReview_isRefused() {
        assertRefusedAfterTheBench(PathologyStatus.UNDER_REVIEW);
    }

    @Test
    public void onASignedOutCase_isRefused() {
        storeAt(PathologyStatus.COMPLETED);

        assertRefusedWith("pathology.error.caseCompleted", () -> check(CoverslipQc.PASS, first()));
        assertNothingRecorded();
    }

    // ---- the slide's staining ----

    @Test
    public void aSlideNotMarkedStained_refusesTheBatchNamingIt() {
        pathologySampleService.recordStainStatus(CASE_ID, List.of(slides.get(1)), StainStatus.UNSATISFACTORY,
                TECHNICIAN_ID);
        clearSlideAudit();
        clearCaseAudit();

        LocalizedValidationException refusal = assertRefusedWith("pathology.error.qcBeforeStain",
                () -> check(CoverslipQc.PASS, slides.toArray(new Integer[0])));

        assertEquals("the refusal names the slide as its label does", Map.of("designations", "A1.2"),
                refusal.getParams());
        assertNothingRecorded();
    }

    /** With Staining switched off no slide is ever marked stained. */
    @Test
    public void withStainingSwitchedOff_anUnstainedSlideIsChecked() {
        PathologyCaseRows.storeStainStatus(jdbcTemplate, first(), null);
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_STAINING_ENABLED, "false");

        check(CoverslipQc.PASS, first());

        assertEquals("the check is taken on its own", "PASS", resultOf(first()));
    }

    // ---- a batch ----

    @Test
    public void aBatchOfThree_writesThreeEntriesAndOneCaseHistoryRowWithTheOldValues() {
        check(CoverslipQc.FAIL, slides.toArray(new Integer[0]));
        clearCaseAudit();

        check(CoverslipQc.PASS, slides.toArray(new Integer[0]));

        for (Integer slideId : slides) {
            assertEquals("every slide in the batch passed", "PASS", resultOf(slideId));
        }
        List<History> diffs = caseSaveDiffs();
        assertEquals("the batch is one change to the case", 1, diffs.size());
        assertTrue("which carries the result it replaced: " + changesOf(diffs.get(0)),
                changesOf(diffs.get(0)).contains("Coverslip QC: FAIL"));
    }

    // ---- refusals ----

    @Test
    public void noSlideOrNoResultNamed_isRefused() {
        assertRefusedWith("pathology.error.slideSelectionRequired",
                () -> pathologySampleService.recordCoverslipQc(CASE_ID, List.of(), CoverslipQc.PASS, TECHNICIAN_ID));
        assertRefusedWith("pathology.error.slideSelectionRequired",
                () -> pathologySampleService.recordCoverslipQc(CASE_ID, slides, null, TECHNICIAN_ID));
        assertNothingRecorded();
    }

    @Test
    public void aSlideOfAnotherCase_isRefusedAndTheBatchIsNotRecorded() {
        assertRefusedWith("pathology.error.slideNotOnCase", () -> check(CoverslipQc.PASS, first(), LEGACY_SLIDE_ID));
        assertNothingRecorded();
    }

    @Test
    public void aRetiredSlide_isRefusedAndTheBatchIsNotRecorded() {
        pathologySampleService.deactivateSlide(slides.get(2), "cracked", TECHNICIAN_ID);
        clearSlideAudit();

        assertRefusedWith("pathology.error.slideRetired",
                () -> check(CoverslipQc.PASS, slides.toArray(new Integer[0])));
        assertNothingRecorded();
    }

    // ---- three identical requests at once ----

    @Test
    public void threeConcurrentChecks_leaveOneEntryPerSlide() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<List<PathologySlide>>> checks = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Callable<List<PathologySlide>> passing = () -> {
                    startGate.await();
                    return check(CoverslipQc.PASS, slides.toArray(new Integer[0]));
                };
                checks.add(pool.submit(passing));
            }
            startGate.countDown();

            int answered = 0;
            for (Future<List<PathologySlide>> passing : checks) {
                try {
                    passing.get(60, TimeUnit.SECONDS);
                    answered++;
                } catch (ExecutionException e) {
                    assertTrue("a refused check lost a slide's version check, not " + e.getCause(),
                            StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertTrue("at least one check is answered", answered >= 1);
            for (Integer slideId : slides) {
                assertEquals("every slide passed", "PASS", resultOf(slideId));
                assertEquals("with exactly one entry recording it", 1,
                        slideEntries(slideId, PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED).size());
            }
            assertEquals("and exactly one row in the case's history", 1, caseSaveDiffs().size());
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- a check and a move, or a save, landing together ----

    /**
     * A check and a stage move write different rows, so without a guard both could
     * commit and a failed slide would reach the pathologist with nothing holding
     * it. The move is held open while the check runs: the check waits on the case
     * row and then loses its version check.
     */
    @Test
    public void aFailRacingAnAdvanceHeldOpen_losesAndTheSlideKeepsItsPass() {
        passEverySlide();

        Outcome outcome = holdOpenWhile(
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.COVERSLIPPING, TECHNICIAN_ID),
                () -> check(CoverslipQc.FAIL, first()));

        assertConsistent();
        assertNull("the move commits", outcome.held());
        assertTrue("the check loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the case reached the pathologist", "READY_PATHOLOGIST", storedStage());
        assertEquals("with the slide still passed", "PASS", resultOf(first()));
    }

    /**
     * The other order: the check is held open and the move runs. The check holds
     * the case row, so the move waits for it and then loses its version check.
     */
    @Test
    public void anAdvanceRacingAFailHeldOpen_waitsAndLoses() {
        passEverySlide();

        Outcome outcome = holdOpenWhile(() -> check(CoverslipQc.FAIL, first()),
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.COVERSLIPPING, TECHNICIAN_ID));

        assertConsistent();
        assertNull("the check commits", outcome.held());
        assertTrue("the move loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the case stays at coverslipping", "COVERSLIPPING", storedStage());
        assertEquals("held by the failed slide", "FAIL", resultOf(first()));
    }

    /**
     * A save that edits the very slide being checked, held open while the check
     * runs: the save holds the case row and then the slide row, the check waits on
     * the case row first, so the two never deadlock and the check loses its version
     * check.
     */
    @Test
    public void aSaveEditingTheSlideHeldOpen_makesTheCheckLose() {
        passEverySlide();

        Outcome outcome = holdOpenWhile(() -> saveSlides(slideMovedTo(first(), "Rack 9")),
                () -> check(CoverslipQc.FAIL, first()));

        assertNull("the save commits", outcome.held());
        assertTrue("the check loses its version check, never a deadlock, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the save's location stands", "Rack 9", slideRow(first()).get("location"));
        assertEquals("and the slide keeps its pass", "PASS", resultOf(first()));
    }

    /**
     * The reverse: the check is held open while a save edits the same slide. The
     * check holds the case row, the save waits on it and then loses its version
     * check, which the case save answers as its conflict.
     */
    @Test
    public void aCheckHeldOpen_makesASaveEditingTheSlideLose() {
        passEverySlide();

        Outcome outcome = holdOpenWhile(() -> check(CoverslipQc.FAIL, first()), () -> {
            saveSlides(slideMovedTo(first(), "Rack 9"));
            return null;
        });

        assertNull("the check commits", outcome.held());
        assertTrue("the save loses its version check, never a deadlock, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the slide failed its check", "FAIL", resultOf(first()));
        assertEquals("and keeps the location it had", "Slide Storage 1", slideRow(first()).get("location"));
    }

    /**
     * A check landing beside a save of the whole case loses too: the save writes
     * the case row first, and the check's version check then fails, which the
     * endpoint answers as a 409 the screen explains.
     */
    @Test
    public void aCheckRacingACaseSaveHeldOpen_loses() {
        passEverySlide();
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("Described again while the slide was checked");

        Outcome outcome = holdOpenWhile(() -> save(form), () -> check(CoverslipQc.FAIL, first()));

        assertNull("the save commits", outcome.held());
        assertTrue("the check loses its version check, not " + outcome.other(),
                StaleSaveGuard.isOptimisticLockFailure(outcome.other()));
        assertEquals("the save's change stands", "Described again while the slide was checked",
                pathologySampleService.get(CASE_ID).getGrossExam());
        assertEquals("and the slide keeps its pass", "PASS", resultOf(first()));
    }

    /**
     * Ten runs of a check and a move started together: whichever wins, the case
     * never reaches the pathologist with a slide failed.
     */
    @Test
    public void aFailAndAnAdvanceStartedTogether_neverLeaveAFailedSlideWithThePathologist() throws Exception {
        for (int run = 0; run < 10; run++) {
            storeAt(PathologyStatus.COVERSLIPPING);
            passEverySlide();
            CountDownLatch startGate = new CountDownLatch(1);
            ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(2));
            try {
                Future<?> move = pool.submit(() -> {
                    startGate.await();
                    return pathologySampleService.advance(CASE_ID, PathologyStatus.COVERSLIPPING, TECHNICIAN_ID);
                });
                Future<?> fail = pool.submit(() -> {
                    startGate.await();
                    return check(CoverslipQc.FAIL, first());
                });
                startGate.countDown();
                for (Future<?> call : List.of(move, fail)) {
                    try {
                        call.get(60, TimeUnit.SECONDS);
                    } catch (ExecutionException e) {
                        assertTrue("a call that loses is a refusal or a lost version check, not " + e.getCause(),
                                e.getCause() instanceof LocalizedValidationException
                                        || e.getCause() instanceof PathologyStageConflictException
                                        || StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                    }
                }
            } finally {
                pool.shutdownNow();
            }
            assertConsistent();
        }
    }

    // ---- the coverslipping batch (FR-8.4) ----

    /**
     * The batch is written once on the open Coverslipping entry, trimmed, with one
     * audit entry naming the stage; a later save neither changes nor clears it.
     */
    @Test
    public void aBatchSavedAtCoverslipping_isWrittenOnceAndAudited() {
        storeAt(PathologyStatus.STAINING);
        pathologySampleService.advance(CASE_ID, PathologyStatus.STAINING, TECHNICIAN_ID);
        assertTrue("an open coverslipping entry with no batch takes one", isBatchRecordable());

        save(withBatch("  CS-2026-07  "));

        assertEquals("the batch is kept on the coverslipping entry, trimmed", "CS-2026-07", coverslippingBatch());
        assertFalse("and the entry takes no other", isBatchRecordable());
        List<History> recorded = runReferenceAudit();
        assertEquals("the write is audited once", 1, recorded.size());
        assertEquals("with the verb, the stage and the batch",
                Map.of("verb", PathologySampleService.PATHOLOGY_RUN_REFERENCE_RECORDED, "stage", "COVERSLIPPING",
                        "runReference", "CS-2026-07"),
                payloadOf(recorded.get(0)));

        save(withBatch("CS-2026-08"));
        save(caseViewForm());

        assertEquals("a second batch, or none, leaves the first", "CS-2026-07", coverslippingBatch());
        assertEquals("and nothing more is audited", 1, runReferenceAudit().size());
    }

    @Test
    public void aBatchLongerThanTheColumn_isRefused() {
        storeAt(PathologyStatus.STAINING);
        pathologySampleService.advance(CASE_ID, PathologyStatus.STAINING, TECHNICIAN_ID);

        assertRefusedWith("pathology.error.runReferenceTooLong", () -> save(withBatch(StringUtils.repeat("x", 65))));
        assertNull("nothing is written", coverslippingBatch());
    }

    /**
     * Posted at Processing, the batch is not written on the processor run's entry:
     * each reference belongs to its own stage.
     */
    @Test
    public void aBatchPostedAtAnotherStage_isIgnored() {
        storeAt(PathologyStatus.GROSSING);
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        assertEquals("setup: the case stands at processing on an open entry", "PROCESSING", storedStage());

        save(withBatch("CS-2026-09"));

        assertNull("the processing entry takes no coverslipping batch",
                jdbcTemplate.queryForObject(
                        "SELECT run_reference FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?"
                                + " AND stage = 'PROCESSING'",
                        String.class, CASE_ID));
        assertTrue("and nothing is audited", runReferenceAudit().isEmpty());
    }

    // helpers

    /** The slide posted back by the case view with a new location. */
    private PathologySlideForm slideMovedTo(int slideId, String location) {
        PathologySlideForm posted = postedSlide(null, String.valueOf(HE));
        posted.setId(slideId);
        posted.setLocation(location);
        return posted;
    }

    private void passEverySlide() {
        slides.forEach(slideId -> {
            PathologyCaseRows.storeStainStatus(jdbcTemplate, slideId, StainStatus.STAINED);
            PathologyCaseRows.storeCoverslipQc(jdbcTemplate, slideId, "PASS");
        });
    }

    /** A case past the bench never holds a slide that failed its check. */
    private void assertConsistent() {
        boolean withThePathologist = "READY_PATHOLOGIST".equals(storedStage());
        assertFalse("the case is with the pathologist while a slide failed its check",
                withThePathologist && "FAIL".equals(resultOf(first())));
    }

    private void assertRefusedAfterTheBench(PathologyStatus stage) {
        check(CoverslipQc.PASS, first());
        storeAt(stage);

        assertRefusedWith("pathology.error.slideWorkAfterBench", () -> check(CoverslipQc.FAIL, first()));

        assertEquals("the slide keeps its check", "PASS", resultOf(first()));
        assertEquals("and only the first check is on record", 1,
                slideEntries(first(), PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED).size());
    }

    private int first() {
        return slides.get(0);
    }

    private List<PathologySlide> check(CoverslipQc result, Integer... slideIds) {
        return pathologySampleService.recordCoverslipQc(CASE_ID, List.of(slideIds), result, TECHNICIAN_ID);
    }

    private String resultOf(int slideId) {
        return (String) slideWork(slideId).get("coverslip_qc");
    }

    private void assertNothingRecorded() {
        for (Integer slideId : slides) {
            assertNull("no slide is checked", resultOf(slideId));
            assertTrue("and no entry is written",
                    slideEntries(slideId, PathologySampleService.PATHOLOGY_SLIDE_QC_RECORDED).isEmpty());
        }
        assertTrue("nor a case history row", caseSaveDiffs().isEmpty());
    }

    private PathologySampleForm withBatch(String batch) {
        PathologySampleForm form = caseViewForm();
        form.setCoverslipRunReference(batch);
        return form;
    }

    private boolean isBatchRecordable() {
        return pathologySampleService.isRunReferenceRecordable(pathologySampleService.get(CASE_ID),
                PathologyStatus.COVERSLIPPING);
    }

    private String coverslippingBatch() {
        List<String> batches = jdbcTemplate.queryForList("SELECT run_reference FROM clinlims.pathology_stage_event"
                + " WHERE pathology_sample_id = ? AND stage = 'COVERSLIPPING'", String.class, CASE_ID);
        return batches.isEmpty() ? null : batches.get(0);
    }

    private List<History> runReferenceAudit() {
        return caseAudit().stream().filter(history -> changesOf(history).startsWith("{"))
                .filter(history -> PathologySampleService.PATHOLOGY_RUN_REFERENCE_RECORDED
                        .equals(payloadOf(history).get("verb")))
                .sorted(Comparator.comparing(history -> Long.valueOf(history.getId()))).collect(Collectors.toList());
    }

    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }
}
