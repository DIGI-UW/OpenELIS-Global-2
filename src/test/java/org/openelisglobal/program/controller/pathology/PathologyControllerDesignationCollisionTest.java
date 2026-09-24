package org.openelisglobal.program.controller.pathology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Designations are worked out in memory, so two saves adding a cassette at the
 * same moment can both settle on the same one and the unique index is the last
 * thing standing between them. The save that loses that race has to be
 * answerable, not a bare failure, and the violation reaches the controller
 * wrapped several layers deep inside a persistence exception.
 */
public class PathologyControllerDesignationCollisionTest {

    @Test
    public void aBlockDesignationViolation_isACollision() {
        Throwable failure = nested("could not execute statement",
                "ERROR: duplicate key value violates unique constraint \"pathology_block_designation_uk\"");

        assertTrue("the index the two saves collided on is named in the cause chain, not in the top exception",
                PathologyController.designationCollision(failure));
    }

    @Test
    public void aSlideDesignationViolation_isACollision() {
        Throwable failure = nested("could not execute statement",
                "ERROR: duplicate key value violates unique constraint \"pathology_slide_designation_uk\"");

        assertTrue("slides are numbered within their block and race the same way",
                PathologyController.designationCollision(failure));
    }

    @Test
    public void anUnrelatedConstraint_isNotACollision() {
        Throwable failure = nested("could not execute statement",
                "ERROR: duplicate key value violates unique constraint \"pathology_block_pkey\"");

        assertFalse(
                "a failure on any other constraint is not the designation race, and answering it as a"
                        + " conflict would tell the bench to retry something that cannot succeed",
                PathologyController.designationCollision(failure));
    }

    @Test
    public void aFailureWithNothingToWalk_isNotACollision() {
        assertFalse("nothing to walk is nothing to match", PathologyController.designationCollision(null));
        assertFalse("and neither is a cause chain carrying no message", PathologyController
                .designationCollision(new RuntimeException((String) null, new RuntimeException((String) null))));
    }

    /** A top-level failure whose innermost cause carries the database's message. */
    private Throwable nested(String outerMessage, String rootMessage) {
        return new RuntimeException(outerMessage,
                new IllegalStateException((String) null, new RuntimeException(rootMessage)));
    }
}
