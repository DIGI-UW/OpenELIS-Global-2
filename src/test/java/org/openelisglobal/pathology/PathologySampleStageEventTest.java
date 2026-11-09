package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StaleSaveGuard;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologyStageConflictException;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

/**
 * A case moves between stages only by being sent on or returned, and each move
 * leaves a stage event naming who moved it and when (FR-2.4, FR-2.5, AC-8,
 * AC-9).
 *
 * <p>
 * Before this, the whole-case save took whatever stage the screen posted, so a
 * case could jump anywhere with nothing recording who moved it. These tests pin
 * the two stages that must be finished before a case leaves them, the
 * stale-page guard that makes a repeated send harmless, one audit entry per
 * move, the release from the review queue only (or once from Completed, for a
 * case set there without one), and the processor run written once per
 * Processing entry.
 *
 * <p>
 * The first group is the transition table for the decalcification flag: for a
 * stored stage, an action (a save, a move forward, a return, a release) and a
 * posted flag, which stage and which flag are stored. The rows that only save,
 * without a move, are pinned in {@link PathologySampleGrossingTest}.
 */
public class PathologySampleStageEventTest extends PathologyCaseTestSupport {

    @Autowired
    private IStatusService statusService;

    private StageSwitches switches;

    @Before
    public void switchEveryStageOn() {
        switches = StageSwitches.allOn();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    // ---- the transition table for the decalcification flag (FR-3.2, FR-4.1) ----

    /**
     * A stage posted with the save is ignored, so no move happens without an event
     * naming who made it.
     */
    @Test
    public void aPostedStatus_isIgnoredByTheSave() {
        PathologySampleForm form = flagged(true);
        form.setStatus(PathologyStatus.PROCESSING);

        save(form);

        assertEquals("the save no longer moves the case, whatever stage it posts", "GROSSING", storedStage());
        assertTrue("the flag posted with it is still taken", storedFlag());
        assertEquals("and no stage event is written, since nothing moved", 0, stageEvents().size());
    }

    /** A flagged specimen is sent from grossing to decalcification. */
    @Test
    public void advancingAFlaggedCaseFromGrossing_sendsItToDecalcification() {
        save(flagged(true));

        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        assertEquals("a flagged specimen goes to decalcification", "DECALCIFICATION", storedStage());
        assertTrue("and stays flagged", storedFlag());
    }

    /** An unflagged specimen skips decalcification. */
    @Test
    public void advancingAnUnflaggedCaseFromGrossing_sendsItToProcessing() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        assertEquals("a specimen nobody flagged goes straight to processing", "PROCESSING", storedStage());
        assertFalse("and is still not flagged", storedFlag());
    }

    /** At decalcification the bench may still clear a flag set in error. */
    @Test
    public void aSaveAtDecalcification_respectsAnUntickedFlag() {
        moveToDecalcification();

        save(flagged(false));

        assertFalse("a case standing at the stage keeps whatever the bench posts", storedFlag());
        assertEquals("and the save does not move it", "DECALCIFICATION", storedStage());
    }

    /**
     * A case that leaves decalcification forward went through it, so a flag cleared
     * while it stood there does not make it read as not required.
     */
    @Test
    public void advancingFromDecalcification_withTheFlagCleared_keepsItFlagged() {
        moveToDecalcification();
        save(flagged(false));

        pathologySampleService.advance(CASE_ID, PathologyStatus.DECALCIFICATION, TECHNICIAN_ID);

        assertEquals("the case goes on to processing", "PROCESSING", storedStage());
        assertTrue("and the specimen went through decalcification, so it reads as needing it", storedFlag());
    }

    /** A return from decalcification keeps the bench's choice. */
    @Test
    public void returningFromDecalcification_leavesTheFlagAsTheBenchSetIt() {
        moveToDecalcification();
        save(flagged(false));

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.DECALCIFICATION, "flagged in error",
                TECHNICIAN_ID);

        assertEquals("the case goes back to grossing", "GROSSING", storedStage());
        assertFalse("a case sent back never went through the stage, so a cleared flag stays cleared", storedFlag());
    }

    /** A flagged case at processing returns to decalcification. */
    @Test
    public void returningAFlaggedCaseFromProcessing_sendsItBackToDecalcification() {
        moveToDecalcification();
        pathologySampleService.advance(CASE_ID, PathologyStatus.DECALCIFICATION, TECHNICIAN_ID);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "under-decalcified",
                TECHNICIAN_ID);

        assertEquals("the stage before processing for this specimen is decalcification", "DECALCIFICATION",
                storedStage());
        assertTrue("and the flag is unchanged", storedFlag());
    }

    /**
     * An unflagged case at processing returns to grossing, where the save takes the
     * flag again.
     */
    @Test
    public void returningAnUnflaggedCaseFromProcessing_sendsItBackToGrossingWhereTheFlagIsTakenAgain() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "bone found", TECHNICIAN_ID);

        assertEquals("an unflagged specimen goes back to grossing", "GROSSING", storedStage());
        assertFalse("the flag is unchanged by the return", storedFlag());

        save(flagged(true));

        assertTrue("and back at grossing the bench can flag it", storedFlag());
    }

    /**
     * Releasing the case completes it through the same transition, so a signed-out
     * case also has its entry onto Completed (AC-8), and the move is audited as the
     * sign-out (FR-17).
     */
    @Test
    public void releasingTheCase_completesItWithOneForwardEvent() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);

        save(form);

        assertEquals("a released case is completed", "COMPLETED", storedStage());
        assertFalse("and its flag is unchanged", storedFlag());
        List<Map<String, Object>> events = stageEvents();
        assertEquals("one entry, onto Completed", 1, events.size());
        assertEquals("the entry is onto Completed", "COMPLETED", events.get(0).get("stage"));
        assertEquals("sent forward", "FORWARD", events.get(0).get("direction"));
        assertEquals("by the user who released it", TECHNICIAN_ID, operatorOf(events.get(0), "entered_by"));
        assertEquals("audited as the sign-out, from the pathologist's queue",
                Map.of("verb", PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT, "stageFrom", "READY_PATHOLOGIST"),
                payloadOf(onlyStageAudit()));
    }

    /**
     * The pathologist signs a case out from the review queue; a release posted from
     * a bench stage would skip every stage and precondition between.
     */
    @Test
    public void releasingFromABenchStage_isRefused() {
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);

        assertRefusedWith("pathology.error.releaseBeforeReview", () -> save(form));

        assertEquals("the case stays at grossing", "GROSSING", storedStage());
        assertEquals("no entry is written", 0, stageEvents().size());
        assertEquals("and nothing is audited", 0, stageAudit().size());
    }

    /** A case already under the pathologist's review may be released too. */
    @Test
    public void releasingFromUnderReview_signsTheCaseOut() {
        storeAt(PathologyStatus.UNDER_REVIEW);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);

        save(form);

        assertEquals("the case is completed", "COMPLETED", storedStage());
        assertEquals("and the sign-out names the stage it left", "UNDER_REVIEW",
                payloadOf(onlyStageAudit()).get("stageFrom"));
    }

    /**
     * The stage control once set Completed without signing the case out. Such a
     * case is released once, from where it stands: no second entry onto Completed,
     * but the sign-out is audited. It used to be refused as already signed out, so
     * it could never be released.
     */
    @Test
    public void aCaseLeftAtCompletedWithoutARelease_isReleasedOnce() {
        storeAt(PathologyStatus.COMPLETED);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);

        save(form);

        assertEquals("the case stays completed", "COMPLETED", storedStage());
        assertEquals("with no second entry onto the stage it already stands at", 0, stageEvents().size());
        assertEquals("the release is audited as the sign-out, from Completed",
                Map.of("verb", PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT, "stageFrom", "COMPLETED"),
                payloadOf(onlyStageAudit()));

        PathologySampleForm again = caseViewForm();
        again.setRelease(true);
        assertRefusedWith("pathology.error.caseCompleted", () -> save(again));
        assertEquals("the second release writes nothing", 1, stageAudit().size());
    }

    /** A case released from the pathologist's queue is not released again. */
    @Test
    public void aReleasedCase_isRefusedASecondRelease() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);

        PathologySampleForm again = caseViewForm();
        again.setRelease(true);

        assertRefusedWith("pathology.error.caseCompleted", () -> save(again));
        assertEquals("the case keeps its one entry onto Completed", 1, stageEvents().size());
        assertEquals("and its one sign-out", 1, stageAudit().size());
    }

    /**
     * A new result on the order, an immunohistochemistry one after a referral for
     * instance, sets the order back to Started. The release on record still stands,
     * so the case is not offered a second release, which would sign it out again
     * and finalize every analysis on the order.
     */
    @Test
    public void aReleasedCaseWhoseOrderIsStartedAgain_isStillRefusedASecondRelease() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);
        jdbcTemplate.update(
                "UPDATE clinlims.sample SET status_id = ?"
                        + " WHERE id = (SELECT sample_id FROM clinlims.pathology_sample WHERE id = ?)",
                Integer.parseInt(statusService.getStatusID(OrderStatus.Started)), CASE_ID);

        assertFalse("the released case does not read as releasable",
                pathologySampleService.isReleasable(pathologySampleService.get(CASE_ID)));
        PathologySampleForm again = caseViewForm();
        again.setRelease(true);
        assertRefusedWith("pathology.error.caseCompleted", () -> save(again));
        assertEquals("the case keeps its one sign-out", 1, stageAudit().size());
    }

    // ---- the event rows (AC-8, AC-9) ----

    /**
     * A move forward opens an entry on the new stage and completes the one it
     * leaves, each with the operator and the server clock.
     */
    @Test
    public void advancing_opensAnEntryAndCompletesTheOneTheCaseLeaves() {
        PathologyStageEvent first = pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        PathologyStageEvent second = pathologySampleService.advance(CASE_ID, PathologyStatus.PROCESSING, TECHNICIAN_ID);

        List<Map<String, Object>> events = stageEvents();
        assertEquals("two moves, two entries", 2, events.size());

        Map<String, Object> processing = events.get(0);
        assertEquals("the first entry is the one the first move returned", first.getId(), processing.get("id"));
        assertEquals("onto processing", "PROCESSING", processing.get("stage"));
        assertEquals("sent forward", "FORWARD", processing.get("direction"));
        assertEquals("by the session user", TECHNICIAN_ID, operatorOf(processing, "entered_by"));
        assertNotNull("at a time the server recorded", processing.get("entered_at"));
        assertNotNull("and completed when the case left it", processing.get("completed_at"));
        assertEquals("by whoever moved it on", TECHNICIAN_ID, operatorOf(processing, "completed_by"));
        assertNull("a move forward carries no reason", processing.get("reason"));

        Map<String, Object> embedding = events.get(1);
        assertEquals("the second entry is the one the second move returned", second.getId(), embedding.get("id"));
        assertEquals("onto embedding", "EMBEDDING", embedding.get("stage"));
        assertNull("still open while the case stands there", embedding.get("completed_at"));
        assertNull("so nobody has completed it", embedding.get("completed_by"));
        assertEquals("and the service reads the same history back",
                List.of(PathologyStatus.PROCESSING, PathologyStatus.EMBEDDING),
                pathologySampleService.getStageEvents(CASE_ID).stream().map(PathologyStageEvent::getStage)
                        .collect(Collectors.toList()));
    }

    /** A return is its own entry, marked RETURN and carrying the reason. */
    @Test
    public void returning_writesAReturnEntryWithTheReason() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "  cassette A1 not fixed  ",
                TECHNICIAN_ID);

        List<Map<String, Object>> events = stageEvents();
        assertEquals("one entry forward and one back", 2, events.size());
        assertNotNull("leaving processing completes its entry", events.get(0).get("completed_at"));
        Map<String, Object> returned = events.get(1);
        assertEquals("the case re-enters grossing", "GROSSING", returned.get("stage"));
        assertEquals("marked as a return", "RETURN", returned.get("direction"));
        assertEquals("with the reason, trimmed", "cassette A1 not fixed", returned.get("reason"));
        assertNull("and open while the case stands there", returned.get("completed_at"));
    }

    /**
     * A case on file before stage events were recorded has no entry to complete,
     * and none is invented for it.
     */
    @Test
    public void aLegacyCasesFirstMove_completesNothing() {
        assertEquals("the fixture's case has no stage history", 0, stageEvents().size());

        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        List<Map<String, Object>> events = stageEvents();
        assertEquals("the move writes only the entry onto the new stage, no invented grossing entry", 1, events.size());
        assertEquals("onto processing", "PROCESSING", events.get(0).get("stage"));
    }

    /**
     * A page that still shows a stage the case has left cannot move it, and the
     * refusal changes nothing.
     */
    @Test
    public void advancingFromAStageTheCaseHasLeft_isAConflictThatWritesNothing() {
        PathologyStageConflictException conflict = assertThrows(PathologyStageConflictException.class,
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.EMBEDDING, TECHNICIAN_ID));

        assertEquals("the conflict names where the case really stands", PathologyStatus.GROSSING,
                conflict.getStoredStage());
        assertEquals("the case is not moved", "GROSSING", storedStage());
        assertEquals("no entry is written", 0, stageEvents().size());
        assertEquals("and nothing is audited", 0, stageAudit().size());

        assertThrows("a return from a stale page is refused the same way", PathologyStageConflictException.class,
                () -> pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "reason",
                        TECHNICIAN_ID));
        assertEquals("and moves nothing either", "GROSSING", storedStage());
    }

    /**
     * A request that does not say where the case is cannot be checked, so it is
     * refused.
     */
    @Test
    public void aMoveThatDoesNotNameTheStage_isRefused() {
        assertRefusedWith("pathology.error.fromRequired",
                () -> pathologySampleService.advance(CASE_ID, null, TECHNICIAN_ID));
        assertEquals("and the case is not moved", "GROSSING", storedStage());
    }

    // ---- what must be finished before a case moves on (FR-3.4, FR-6.3) ----

    /** Grossing is finished once the specimen is described. */
    @Test
    public void advancingFromGrossingWithoutADescription_isRefused() {
        PathologySampleForm form = caseViewForm();
        form.setGrossExam("   ");
        save(form);

        assertRefusedWith("pathology.error.grossingIncomplete",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID));
        assertEquals("the case stays at grossing", "GROSSING", storedStage());
        assertEquals("and no entry is written", 0, stageEvents().size());
    }

    /** Grossing is finished once at least one cassette in use is cut. */
    @Test
    public void advancingFromGrossingWithNoCassetteInUse_isRefused() {
        pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, "dropped", TECHNICIAN_ID);

        assertRefusedWith("pathology.error.grossingIncomplete",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID));
        assertEquals("the case stays at grossing", "GROSSING", storedStage());
    }

    /**
     * Embedding is finished once every cassette in use is a block; the refusal
     * names the ones still waiting.
     */
    @Test
    public void advancingFromEmbeddingWithACassetteOutstanding_isRefusedNamingIt() {
        storeAt(PathologyStatus.EMBEDDING);

        LocalizedValidationException refusal = assertRefusedWith("pathology.error.embeddingIncomplete",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.EMBEDDING, TECHNICIAN_ID));

        assertEquals("the refusal names the legacy cassette still to embed", Map.of("designations", "11"),
                refusal.getParams());
        assertEquals("and the case stays at embedding", "EMBEDDING", storedStage());

        pathologySampleService.embedBlock(LEGACY_BLOCK_ID, TECHNICIAN_ID);
        pathologySampleService.advance(CASE_ID, PathologyStatus.EMBEDDING, TECHNICIAN_ID);

        assertEquals("once it is a block the case moves on", "MICROTOMY", storedStage());
    }

    /**
     * A case standing at embedding with no cassette in use has nothing embedded, so
     * it is refused on its own terms rather than naming an empty list.
     */
    @Test
    public void advancingFromEmbeddingWithNoCassetteInUse_isRefused() {
        storeAt(PathologyStatus.EMBEDDING);
        pathologySampleService.deactivateBlock(LEGACY_BLOCK_ID, "dropped", TECHNICIAN_ID);

        LocalizedValidationException refusal = assertRefusedWith("pathology.error.embeddingNoCassettes",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.EMBEDDING, TECHNICIAN_ID));

        assertTrue("the refusal names nothing", refusal.getParams() == null || refusal.getParams().isEmpty());
        assertEquals("and the case stays at embedding", "EMBEDDING", storedStage());
    }

    /**
     * Microtomy is finished once at least one slide is cut and in use; a block may
     * be held with no slide, so none is asked of every block.
     */
    @Test
    public void advancingFromMicrotomyWithNoSlide_isRefused() {
        storeAt(PathologyStatus.MICROTOMY);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id = ?", LEGACY_SLIDE_ID);

        assertRefusedWith("pathology.error.microtomyIncomplete",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.MICROTOMY, TECHNICIAN_ID));
        assertEquals("the case stays at microtomy", "MICROTOMY", storedStage());
        assertEquals("and no entry is written", 0, stageEvents().size());
    }

    /** A retired slide is not a slide to stain. */
    @Test
    public void advancingFromMicrotomyWithOnlyARetiredSlide_isRefused() {
        storeAt(PathologyStatus.MICROTOMY);
        pathologySampleService.deactivateSlide(LEGACY_SLIDE_ID, "section folded", TECHNICIAN_ID);

        assertRefusedWith("pathology.error.microtomyIncomplete",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.MICROTOMY, TECHNICIAN_ID));
        assertEquals("the case stays at microtomy", "MICROTOMY", storedStage());
    }

    @Test
    public void advancingFromMicrotomyWithOneSlideInUse_sendsItOn() {
        storeAt(PathologyStatus.MICROTOMY);
        jdbcTemplate.update("DELETE FROM clinlims.pathology_slide WHERE id = ?", LEGACY_SLIDE_ID);
        storeAsBlock(LEGACY_BLOCK_ID);
        saveSlides(postedSlide(LEGACY_BLOCK_ID));

        pathologySampleService.advance(CASE_ID, PathologyStatus.MICROTOMY, TECHNICIAN_ID);

        assertEquals("one slide cut from the block is enough to send the case to staining", "STAINING", storedStage());
    }

    /** From the pathologist's queue the bench has nothing to send the case to. */
    @Test
    public void advancingBeyondTheBench_isRefused() {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        assertRefusedWith("pathology.error.advanceBeyondBench",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.READY_PATHOLOGIST, TECHNICIAN_ID));

        storeAt(PathologyStatus.COMPLETED);
        assertRefusedWith("pathology.error.caseCompleted",
                () -> pathologySampleService.advance(CASE_ID, PathologyStatus.COMPLETED, TECHNICIAN_ID));
        assertEquals("and nothing is written", 0, stageEvents().size());
    }

    /** A stage the deployment switched off is skipped (AC-7). */
    @Test
    public void advancing_skipsAStageTheDeploymentSwitchedOff() {
        storeAt(PathologyStatus.STAINING);
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATHOLOGY_STAGE_COVERSLIPPING_ENABLED, "false");

        pathologySampleService.advance(CASE_ID, PathologyStatus.STAINING, TECHNICIAN_ID);

        assertEquals("with coverslipping off, staining goes straight to the pathologist", "READY_PATHOLOGIST",
                storedStage());
    }

    // ---- returning (FR-2.5) ----

    /** A return needs a reason, and the reason fits its column. */
    @Test
    public void returning_needsAReasonThatFits() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        assertRefusedWith("pathology.error.returnReasonRequired", () -> pathologySampleService
                .returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "   ", TECHNICIAN_ID));
        assertRefusedWith("pathology.error.reasonTooLong", () -> pathologySampleService.returnToPreviousStage(CASE_ID,
                PathologyStatus.PROCESSING, StringUtils.repeat("x", 256), TECHNICIAN_ID));
        assertEquals("a refused return leaves the case where it was", "PROCESSING", storedStage());
        assertEquals("and writes no entry", 1, stageEvents().size());

        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, StringUtils.repeat("x", 255),
                TECHNICIAN_ID);

        assertEquals("a reason of exactly the column's width is accepted", "GROSSING", storedStage());
    }

    /**
     * Nothing is earlier than accessioning, and a signed-out case is reopened, not
     * returned.
     */
    @Test
    public void returning_fromAccessionedOrCompleted_isRefused() {
        storeAt(PathologyStatus.ACCESSIONED);
        assertRefusedWith("pathology.error.nothingToReturnTo", () -> pathologySampleService
                .returnToPreviousStage(CASE_ID, PathologyStatus.ACCESSIONED, "reason", TECHNICIAN_ID));

        storeAt(PathologyStatus.COMPLETED);
        assertRefusedWith("pathology.error.caseCompleted", () -> pathologySampleService.returnToPreviousStage(CASE_ID,
                PathologyStatus.COMPLETED, "reason", TECHNICIAN_ID));
        assertEquals("and nothing is written", 0, stageEvents().size());
    }

    // ---- one audit entry per move (FR-17) ----

    /**
     * Each move leaves one entry on the case naming the verb and the two stages,
     * and a return its reason; nothing about the patient.
     */
    @Test
    public void eachMove_leavesOneAuditEntryWithTheVerbAndTheStages() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "bone found", TECHNICIAN_ID);

        List<History> audit = stageAudit();
        assertEquals("two moves, two entries", 2, audit.size());

        History advanced = audit.get(0);
        assertEquals("recorded as an update of the case", "U", advanced.getActivity());
        assertEquals("by the session user", TECHNICIAN_ID, advanced.getSysUserId());
        assertEquals("the move forward carries exactly the verb and the two stages",
                Map.of("verb", "PATHOLOGY_STAGE_ADVANCED", "stageFrom", "GROSSING", "stageTo", "PROCESSING"),
                payloadOf(advanced));

        assertEquals("the return carries the reason as well", Map.of("verb", "PATHOLOGY_STAGE_RETURNED", "stageFrom",
                "PROCESSING", "stageTo", "GROSSING", "reason", "bone found"), payloadOf(audit.get(1)));
    }

    // ---- the processor run (FR-5.1) ----

    /**
     * At processing the run is written once on the open entry, trimmed, with one
     * audit entry; a later save neither changes nor clears it.
     */
    @Test
    public void aRunReferenceSavedAtProcessing_isWrittenOnceAndAudited() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        assertTrue("an open processing entry with no run takes one", isRunReferenceRecordable());

        save(withRunReference("  RUN-2026-114  "));

        assertEquals("the run is kept on the processing entry, trimmed", "RUN-2026-114",
                stageEvents().get(0).get("run_reference"));
        assertFalse("and the entry takes no other", isRunReferenceRecordable());
        List<History> recorded = runReferenceAudit();
        assertEquals("the write is audited once", 1, recorded.size());
        assertEquals("by the session user", TECHNICIAN_ID, recorded.get(0).getSysUserId());
        assertEquals("with the verb, the stage and the run",
                Map.of("verb", PathologySampleService.PATHOLOGY_RUN_REFERENCE_RECORDED, "stage", "PROCESSING",
                        "runReference", "RUN-2026-114"),
                payloadOf(recorded.get(0)));

        save(withRunReference("RUN-2026-115"));
        save(withRunReference("   "));
        save(caseViewForm());

        assertEquals("a second run, a blank or no run at all leaves the first", "RUN-2026-114",
                stageEvents().get(0).get("run_reference"));
        assertEquals("and nothing more is audited", 1, runReferenceAudit().size());
    }

    /** The run fits its column. */
    @Test
    public void aRunReferenceLongerThanTheColumn_isRefused() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);

        assertRefusedWith("pathology.error.runReferenceTooLong",
                () -> save(withRunReference(StringUtils.repeat("R", 65))));

        assertNull("nothing is stored", stageEvents().get(0).get("run_reference"));
        assertEquals("nor audited", 0, runReferenceAudit().size());

        save(withRunReference(StringUtils.repeat("R", 64)));

        assertEquals("a run of exactly the column's width is accepted", StringUtils.repeat("R", 64),
                stageEvents().get(0).get("run_reference"));
    }

    /**
     * Past processing the entry is completed and a posted run no longer changes it.
     */
    @Test
    public void aRunReferencePostedAfterProcessing_isIgnored() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        save(withRunReference("RUN-1"));
        pathologySampleService.advance(CASE_ID, PathologyStatus.PROCESSING, TECHNICIAN_ID);

        save(withRunReference("RUN-2"));

        assertEquals("the completed processing entry keeps the run it was given", "RUN-1",
                stageEvents().get(0).get("run_reference"));
        assertNull("and the embedding entry is given none", stageEvents().get(1).get("run_reference"));
    }

    /**
     * A case sent back from processing has left the entry, so a run posted at the
     * earlier stage is ignored and the completed entry keeps its own.
     */
    @Test
    public void aRunReferencePostedAfterAReturnFromProcessing_isIgnored() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        save(withRunReference("RUN-1"));
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.PROCESSING, "bone found", TECHNICIAN_ID);

        save(withRunReference("RUN-2"));

        assertEquals("the completed processing entry keeps its run", "RUN-1",
                stageEvents().get(0).get("run_reference"));
        assertNull("and the grossing entry the case returned to is given none",
                stageEvents().get(1).get("run_reference"));
        assertEquals("only the first run is audited", 1, runReferenceAudit().size());
    }

    /**
     * A case returned to processing enters it again on a new entry, which takes its
     * own run; the earlier pass keeps the run it was given. This is how a mistyped
     * run is put right.
     */
    @Test
    public void aCaseReturnedToProcessing_takesARunOnItsNewEntry() {
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        save(withRunReference("RUN-1"));
        pathologySampleService.advance(CASE_ID, PathologyStatus.PROCESSING, TECHNICIAN_ID);
        pathologySampleService.returnToPreviousStage(CASE_ID, PathologyStatus.EMBEDDING, "run mistyped", TECHNICIAN_ID);
        assertTrue("the new processing entry takes a run", isRunReferenceRecordable());

        save(withRunReference("RUN-2"));

        List<Map<String, Object>> events = stageEvents();
        assertEquals("forward onto processing, forward onto embedding, back onto processing", 3, events.size());
        assertEquals("the first pass keeps its run", "RUN-1", events.get(0).get("run_reference"));
        assertEquals("the second pass is a return", "RETURN", events.get(2).get("direction"));
        assertEquals("and holds the run written on it", "RUN-2", events.get(2).get("run_reference"));
        assertEquals("each write audited once", 2, runReferenceAudit().size());
    }

    /**
     * A case that reached processing before entries were recorded has none to hold
     * the run, and none is invented for it.
     */
    @Test
    public void aRunReferenceWithNoOpenEntry_isIgnored() {
        storeAt(PathologyStatus.PROCESSING);
        assertFalse("there is no entry to write a run on", isRunReferenceRecordable());

        save(withRunReference("RUN-1"));

        assertEquals("no entry is created to hold it", 0, stageEvents().size());
        assertEquals("and nothing is audited", 0, runReferenceAudit().size());
    }

    // ---- three sends at once ----

    /**
     * Three pages send the same case on at the same moment. The first move wins;
     * the others either find the case already moved or lose the case's version
     * check, and either way they write nothing.
     */
    @Test
    public void threeConcurrentAdvances_leaveOneEventRow() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(3));
        List<Future<PathologyStageEvent>> sends = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Callable<PathologyStageEvent> send = () -> {
                    startGate.await();
                    return pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
                };
                sends.add(pool.submit(send));
            }
            startGate.countDown();

            int won = 0;
            for (Future<PathologyStageEvent> send : sends) {
                try {
                    send.get(60, TimeUnit.SECONDS);
                    won++;
                } catch (ExecutionException e) {
                    assertTrue(
                            "a losing send is either the stage conflict or a lost version check, not " + e.getCause(),
                            e.getCause() instanceof PathologyStageConflictException
                                    || StaleSaveGuard.isOptimisticLockFailure(e.getCause()));
                }
            }
            assertEquals("exactly one send moved the case", 1, won);
            assertEquals("exactly one entry is written", 1, stageEvents().size());
            assertEquals("the case is exactly one stage on", "PROCESSING", storedStage());
            assertEquals("and exactly one move is audited", 1, stageAudit().size());
        } finally {
            pool.shutdownNow();
        }
    }

    // helpers

    private PathologySampleForm withRunReference(String runReference) {
        PathologySampleForm form = caseViewForm();
        form.setRunReference(runReference);
        return form;
    }

    private void moveToDecalcification() {
        save(flagged(true));
        pathologySampleService.advance(CASE_ID, PathologyStatus.GROSSING, TECHNICIAN_ID);
        assertEquals("setup: the flagged case stands at decalcification", "DECALCIFICATION", storedStage());
    }

    private boolean isRunReferenceRecordable() {
        return pathologySampleService.isRunReferenceRecordable(pathologySampleService.get(CASE_ID));
    }

    private List<Map<String, Object>> stageEvents() {
        return jdbcTemplate.queryForList("SELECT id, stage, direction, entered_at, entered_by, completed_at,"
                + " completed_by, reason, run_reference FROM clinlims.pathology_stage_event"
                + " WHERE pathology_sample_id = ? ORDER BY id", CASE_ID);
    }

    /**
     * entered_by and completed_by are numeric(10), which JDBC answers as a
     * BigDecimal.
     */
    private String operatorOf(Map<String, Object> event, String column) {
        Object operator = event.get(column);
        return operator == null ? null : String.valueOf(((Number) operator).intValue());
    }

    /**
     * The case's stage move entries, oldest first. The case's own save diff, which
     * is XML, shares the reference row and is left out.
     */
    private List<History> stageAudit() {
        return caseEntriesWithVerb(Set.of(PathologySampleService.PATHOLOGY_STAGE_ADVANCED,
                PathologySampleService.PATHOLOGY_STAGE_RETURNED, PathologySampleService.PATHOLOGY_CASE_SIGNED_OUT));
    }

    private List<History> runReferenceAudit() {
        return caseEntriesWithVerb(Set.of(PathologySampleService.PATHOLOGY_RUN_REFERENCE_RECORDED));
    }

    private List<History> caseEntriesWithVerb(Set<String> verbs) {
        return caseAudit().stream().filter(history -> changesOf(history).startsWith("{"))
                .filter(history -> verbs.contains(payloadOf(history).get("verb")))
                .sorted(Comparator.comparing(history -> Long.valueOf(history.getId()))).collect(Collectors.toList());
    }

    private History onlyStageAudit() {
        List<History> audit = stageAudit();
        assertEquals("one move, one audit entry", 1, audit.size());
        return audit.get(0);
    }

    private LocalizedValidationException assertRefusedWith(String errorCode, org.junit.function.ThrowingRunnable call) {
        LocalizedValidationException refusal = assertThrows(LocalizedValidationException.class, call);
        assertEquals("the refusal carries the message the screen translates", errorCode, refusal.getErrorCode());
        return refusal;
    }
}
