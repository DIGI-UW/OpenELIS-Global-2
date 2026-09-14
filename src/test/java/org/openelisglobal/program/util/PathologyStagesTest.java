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
import org.junit.Test;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

/**
 * OGC-264. Before this, the dashboard and the case view each hardcoded which
 * statuses counted as "in progress" and disagreed with each other, and three of
 * the old {@code PathologyStatus} constants ({@code CUTTING}, {@code SLICING},
 * {@code ADDITIONAL_REQUEST}) were not bench stages at all. Plain JUnit: no
 * Spring, no database, so the bench sequence and its mandatory spine (FR-2.3,
 * FR-2.6) are pinned deterministically.
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
    public void ordered_hasElevenStagesAndNoRetiredName() {
        assertEquals(11, PathologyStages.ordered().size());
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
        assertFalse(PathologyStages.isEnabled(COVERSLIPPING, status -> false));
        assertTrue(PathologyStages.isEnabled(COVERSLIPPING, status -> true));
    }

    @Test
    public void enabled_keepsBenchOrderAndDropsOnlyTheDisabledOptionalStages() {
        List<PathologyStatus> result = PathologyStages
                .enabled(status -> status != COVERSLIPPING && status != DECALCIFICATION);
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, PROCESSING, EMBEDDING, MICROTOMY, STAINING,
                READY_PATHOLOGIST, UNDER_REVIEW, COMPLETED);
        assertEquals(expected, result);
    }

    @Test
    public void enabled_withEverythingDisabledLeavesTheSpineInOrder() {
        List<PathologyStatus> expected = List.of(ACCESSIONED, GROSSING, READY_PATHOLOGIST, COMPLETED);
        assertEquals(expected, PathologyStages.enabled(status -> false));
    }

    @Test
    public void displayKey_isTheReactIntlIdForEveryStage() {
        assertEquals("pathology.stage.accessioned", PathologyStages.displayKey(ACCESSIONED));
        assertEquals("pathology.stage.grossing", PathologyStages.displayKey(GROSSING));
        assertEquals("pathology.stage.decalcification", PathologyStages.displayKey(DECALCIFICATION));
        assertEquals("pathology.stage.processing", PathologyStages.displayKey(PROCESSING));
        assertEquals("pathology.stage.embedding", PathologyStages.displayKey(EMBEDDING));
        assertEquals("pathology.stage.microtomy", PathologyStages.displayKey(MICROTOMY));
        assertEquals("pathology.stage.staining", PathologyStages.displayKey(STAINING));
        assertEquals("pathology.stage.coverslipping", PathologyStages.displayKey(COVERSLIPPING));
        assertEquals("pathology.stage.readyPathologist", PathologyStages.displayKey(READY_PATHOLOGIST));
        assertEquals("pathology.stage.underReview", PathologyStages.displayKey(UNDER_REVIEW));
        assertEquals("pathology.stage.completed", PathologyStages.displayKey(COMPLETED));
    }

    @Test
    public void ordered_isUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> PathologyStages.ordered().add(ACCESSIONED));
    }
}
