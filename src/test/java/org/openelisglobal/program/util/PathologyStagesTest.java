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

import java.util.List;
import java.util.Optional;
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
}
