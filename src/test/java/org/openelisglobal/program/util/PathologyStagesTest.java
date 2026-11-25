package org.openelisglobal.program.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.ACCESSIONED;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.COMPLETED;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.COVERSLIPPING;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.DECALCIFICATION;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.EMBEDDING;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.GROSSING;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.MICROTOMY;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.PROCESSING;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.READY_PATHOLOGIST;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.STAINING;
import static org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus.UNDER_REVIEW;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.Test;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * Before this, the dashboard's backend count tile and the dashboard's frontend
 * stage filter each hardcoded which statuses counted as "in progress" and
 * disagreed with each other, and three of the old {@code PathologyStatus}
 * constants ({@code CUTTING}, {@code SLICING}, {@code ADDITIONAL_REQUEST}) were
 * not bench stages at all. Plain JUnit: no Spring, no database, so the bench
 * sequence, its mandatory spine and the in-progress grouping (FR-2.3, FR-2.6,
 * AC-4) are pinned deterministically.
 */
public class PathologyStagesTest {

    @Test
    public void ordered_followsTheBenchSequence() {
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, DECALCIFICATION, PROCESSING, EMBEDDING,
                MICROTOMY, STAINING, COVERSLIPPING, READY_PATHOLOGIST, UNDER_REVIEW, COMPLETED);
        assertEquals("FR-2.1 fixes the bench sequence in the enum's declaration order", expected,
                PathologyStages.ordered());
    }

    @Test
    public void aNewCase_startsAtTheFirstBenchStage() {
        assertEquals("a case that has just been received is at the first stage of the bench, not the second",
                PathologyStages.ordered().get(0), new PathologySample().getStatus());
        assertEquals("the first bench stage is ACCESSIONED (FR-2.1 row 1)", ACCESSIONED,
                new PathologySample().getStatus());
    }

    @Test
    public void ordered_hasElevenStagesAndNoRetiredName() {
        assertEquals("FR-2.1 names eleven bench stages", 11, PathologyStages.ordered().size());
        assertThrows("CUTTING was retired in favor of the real bench stages (AC-5)", IllegalArgumentException.class,
                () -> PathologyStatus.valueOf("CUTTING"));
        assertThrows("SLICING was retired in favor of MICROTOMY (AC-5)", IllegalArgumentException.class,
                () -> PathologyStatus.valueOf("SLICING"));
        assertThrows("ADDITIONAL_REQUEST was retired in favor of the request sub-flow (AC-5)",
                IllegalArgumentException.class, () -> PathologyStatus.valueOf("ADDITIONAL_REQUEST"));
    }

    @Test
    public void isMandatory_isTrueForExactlyTheSpine() {
        List<PathologyStatus> spine = List.of(ACCESSIONED, GROSSING, READY_PATHOLOGIST, COMPLETED);
        for (PathologyStatus status : PathologyStages.ordered()) {
            if (spine.contains(status)) {
                assertTrue("the spine stage " + status + " must report mandatory", PathologyStages.isMandatory(status));
            } else {
                assertFalse("the non-spine stage " + status + " must not report mandatory",
                        PathologyStages.isMandatory(status));
            }
        }
    }

    @Test
    public void isEnabled_aMandatoryStageIgnoresADeploymentThatDisablesIt() {
        for (PathologyStatus mandatory : List.of(ACCESSIONED, GROSSING, READY_PATHOLOGIST, COMPLETED)) {
            assertTrue("a mandatory stage stays enabled when the deployment says otherwise",
                    PathologyStages.isEnabled(mandatory, status -> false));
        }
    }

    @Test
    public void isEnabled_anOptionalStageFollowsTheDeployment() {
        assertFalse("an optional stage the deployment switched off is not visited",
                PathologyStages.isEnabled(COVERSLIPPING, status -> false));
        assertTrue("an optional stage the deployment left on is visited",
                PathologyStages.isEnabled(COVERSLIPPING, status -> true));
    }

    @Test
    public void enabled_keepsBenchOrderAndDropsOnlyTheDisabledOptionalStages() {
        List<PathologyStatus> result = PathologyStages
                .enabled(status -> status != COVERSLIPPING && status != DECALCIFICATION);
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, PROCESSING, EMBEDDING, MICROTOMY, STAINING,
                READY_PATHOLOGIST, UNDER_REVIEW, COMPLETED);
        assertEquals("only the disabled optional stages are dropped, and the rest keep bench order", expected, result);
    }

    @Test
    public void enabled_withEverythingDisabledLeavesTheSpineInOrder() {
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, READY_PATHOLOGIST, COMPLETED);
        assertEquals("a deployment that disables everything still visits the spine, in bench order", expected,
                PathologyStages.enabled(status -> false));
    }

    @Test
    public void inProgress_isEveryStageExceptTheReviewQueueAndCompleted() {
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, DECALCIFICATION, PROCESSING, EMBEDDING,
                MICROTOMY, STAINING, COVERSLIPPING, UNDER_REVIEW);

        List<PathologyStatus> result = PathologyStages.inProgress();

        assertEquals("work in progress is the bench sequence minus the review queue and the finished cases", expected,
                result);
        assertFalse("a case queued for a pathologist is counted as awaiting review, not as in progress",
                result.contains(READY_PATHOLOGIST));
        assertFalse("a finished case is not in progress", result.contains(COMPLETED));
    }

    @Test
    public void displayKey_isTheReactIntlIdForEveryStage() {
        String oneWord = "a one-word stage keys off its own name in lower case";
        assertEquals(oneWord, "pathology.stage.accessioned", PathologyStages.displayKey(ACCESSIONED));
        assertEquals(oneWord, "pathology.stage.grossing", PathologyStages.displayKey(GROSSING));
        assertEquals(oneWord, "pathology.stage.decalcification", PathologyStages.displayKey(DECALCIFICATION));
        assertEquals(oneWord, "pathology.stage.processing", PathologyStages.displayKey(PROCESSING));
        assertEquals(oneWord, "pathology.stage.embedding", PathologyStages.displayKey(EMBEDDING));
        assertEquals(oneWord, "pathology.stage.microtomy", PathologyStages.displayKey(MICROTOMY));
        assertEquals(oneWord, "pathology.stage.staining", PathologyStages.displayKey(STAINING));
        assertEquals(oneWord, "pathology.stage.coverslipping", PathologyStages.displayKey(COVERSLIPPING));

        String twoWords = "an underscored stage keys off the camel-cased name the message bundle uses";
        assertEquals(twoWords, "pathology.stage.readyPathologist", PathologyStages.displayKey(READY_PATHOLOGIST));
        assertEquals(twoWords, "pathology.stage.underReview", PathologyStages.displayKey(UNDER_REVIEW));
        assertEquals(oneWord, "pathology.stage.completed", PathologyStages.displayKey(COMPLETED));
    }

    @Test
    public void ordered_isUnmodifiable() {
        assertThrows("callers share the bench sequence, so none of them may reorder or extend it",
                UnsupportedOperationException.class, () -> PathologyStages.ordered().add(ACCESSIONED));
    }

    @Test
    public void enablementProperty_existsForExactlyTheOptionalStages() {
        List<PathologyStatus> spine = List.of(ACCESSIONED, GROSSING, READY_PATHOLOGIST, COMPLETED);
        for (PathologyStatus status : PathologyStages.ordered()) {
            Optional<Property> property = PathologyStages.enablementProperty(status);
            if (spine.contains(status)) {
                assertEquals("a mandatory stage has no switch, since nothing may disable it", Optional.empty(),
                        property);
            } else {
                assertTrue("an optional stage has a switch a deployment can read", property.isPresent());
                assertEquals("the switch's stored name follows pathology.stage.<STATUS>.enabled",
                        "pathology.stage." + status.name() + ".enabled", property.get().getDBName());
                assertEquals("the switch's constant name follows PATHOLOGY_STAGE_<STATUS>_ENABLED",
                        "PATHOLOGY_STAGE_" + status.name() + "_ENABLED", property.get().name());
            }
        }
    }

    // ---- next and previous stage (FR-2.3 to FR-2.5, FR-4.1, AC-7) ----

    private static final Predicate<PathologyStatus> EVERY_STAGE_ON = status -> true;

    /**
     * With every stage switched on and the specimen flagged, a case walks the whole
     * bench one stage at a time and stops at the pathologist.
     */
    @Test
    public void next_withEverythingOnAndFlagged_isTheFollowingStageUpToThePathologist() {
        Map<PathologyStatus, Optional<PathologyStatus>> expected = new EnumMap<>(PathologyStatus.class);
        expected.put(ACCESSIONED, Optional.of(GROSSING));
        expected.put(GROSSING, Optional.of(DECALCIFICATION));
        expected.put(DECALCIFICATION, Optional.of(PROCESSING));
        expected.put(PROCESSING, Optional.of(EMBEDDING));
        expected.put(EMBEDDING, Optional.of(MICROTOMY));
        expected.put(MICROTOMY, Optional.of(STAINING));
        expected.put(STAINING, Optional.of(COVERSLIPPING));
        expected.put(COVERSLIPPING, Optional.of(READY_PATHOLOGIST));
        expected.put(READY_PATHOLOGIST, Optional.empty());
        expected.put(UNDER_REVIEW, Optional.empty());
        expected.put(COMPLETED, Optional.empty());

        for (PathologyStatus stage : PathologyStages.ordered()) {
            assertEquals("the stage after " + stage + " on a fully switched-on bench", expected.get(stage),
                    PathologyStages.next(stage, EVERY_STAGE_ON, true));
        }
    }

    /** An unflagged specimen goes from grossing straight to processing. */
    @Test
    public void next_skipsDecalcificationUnlessTheSpecimenIsFlagged() {
        assertEquals("a specimen nobody flagged for decalcification is not sent there", Optional.of(PROCESSING),
                PathologyStages.next(GROSSING, EVERY_STAGE_ON, false));
        assertEquals("a flagged one is", Optional.of(DECALCIFICATION),
                PathologyStages.next(GROSSING, EVERY_STAGE_ON, true));
    }

    /** A stage the deployment switched off is skipped on the way forward (AC-7). */
    @Test
    public void next_skipsAStageTheDeploymentSwitchedOff() {
        assertEquals("with coverslipping off, staining goes straight to the pathologist",
                Optional.of(READY_PATHOLOGIST),
                PathologyStages.next(STAINING, status -> status != COVERSLIPPING, true));
        assertEquals("a flagged specimen still skips decalcification when the deployment does not use it",
                Optional.of(PROCESSING), PathologyStages.next(GROSSING, status -> status != DECALCIFICATION, true));
        assertEquals("with every optional stage off, grossing goes straight to the pathologist",
                Optional.of(READY_PATHOLOGIST), PathologyStages.next(GROSSING, status -> false, true));
    }

    /**
     * A case parked on a stage that was switched off afterwards still moves on from
     * where it stands.
     */
    @Test
    public void next_fromAStageSwitchedOffAfterTheCaseReachedIt_isTheFollowingEnabledStage() {
        assertEquals("a case standing at a switched-off coverslipping still reaches the pathologist",
                Optional.of(READY_PATHOLOGIST),
                PathologyStages.next(COVERSLIPPING, status -> status != COVERSLIPPING, true));
        assertEquals("and one standing at a switched-off processing goes on to the next stage in use",
                Optional.of(MICROTOMY),
                PathologyStages.next(PROCESSING, status -> status != PROCESSING && status != EMBEDDING, false));
    }

    /**
     * From the pathologist's queue on, the bench has nothing to send the case to.
     */
    @Test
    public void next_isEmptyFromThePathologistsQueueOn() {
        for (PathologyStatus stage : List.of(READY_PATHOLOGIST, UNDER_REVIEW, COMPLETED)) {
            assertEquals(stage + " is moved on by the pathologist, not by the bench", Optional.empty(),
                    PathologyStages.next(stage, EVERY_STAGE_ON, true));
        }
    }

    /** Every stage returns to the nearest earlier stage that applies to it. */
    @Test
    public void previous_withEverythingOnAndFlagged_isTheStageBefore() {
        Map<PathologyStatus, Optional<PathologyStatus>> expected = new EnumMap<>(PathologyStatus.class);
        expected.put(ACCESSIONED, Optional.empty());
        expected.put(GROSSING, Optional.of(ACCESSIONED));
        expected.put(DECALCIFICATION, Optional.of(GROSSING));
        expected.put(PROCESSING, Optional.of(DECALCIFICATION));
        expected.put(EMBEDDING, Optional.of(PROCESSING));
        expected.put(MICROTOMY, Optional.of(EMBEDDING));
        expected.put(STAINING, Optional.of(MICROTOMY));
        expected.put(COVERSLIPPING, Optional.of(STAINING));
        expected.put(READY_PATHOLOGIST, Optional.of(COVERSLIPPING));
        expected.put(UNDER_REVIEW, Optional.of(READY_PATHOLOGIST));
        expected.put(COMPLETED, Optional.empty());

        for (PathologyStatus stage : PathologyStages.ordered()) {
            assertEquals("the stage " + stage + " returns to", expected.get(stage),
                    PathologyStages.previous(stage, EVERY_STAGE_ON, true));
        }
    }

    /** Processing returns to decalcification only when the specimen went there. */
    @Test
    public void previous_reachesDecalcificationOnlyWhenTheSpecimenIsFlagged() {
        assertEquals("an unflagged specimen goes back to grossing", Optional.of(GROSSING),
                PathologyStages.previous(PROCESSING, EVERY_STAGE_ON, false));
        assertEquals("a flagged one goes back to decalcification", Optional.of(DECALCIFICATION),
                PathologyStages.previous(PROCESSING, EVERY_STAGE_ON, true));
    }

    /** A stage the deployment switched off is skipped on the way back as well. */
    @Test
    public void previous_skipsAStageTheDeploymentSwitchedOff() {
        assertEquals("with coverslipping off, the pathologist's queue returns to staining", Optional.of(STAINING),
                PathologyStages.previous(READY_PATHOLOGIST, status -> status != COVERSLIPPING, true));
        assertEquals("with every optional stage off, the pathologist's queue returns to grossing",
                Optional.of(GROSSING), PathologyStages.previous(READY_PATHOLOGIST, status -> false, true));
    }

    /**
     * Nothing is earlier than accessioning, and a signed-out case is moved only by
     * a reopen.
     */
    @Test
    public void previous_isEmptyAtAccessionedAndCompleted() {
        assertEquals("accessioned has no earlier stage", Optional.empty(),
                PathologyStages.previous(ACCESSIONED, EVERY_STAGE_ON, true));
        assertEquals("a signed-out case is not returned, it is reopened", Optional.empty(),
                PathologyStages.previous(COMPLETED, EVERY_STAGE_ON, true));
    }
}
