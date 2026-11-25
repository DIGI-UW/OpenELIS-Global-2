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
                PathologyController.concurrentSaveConflict(failure));
    }

    @Test
    public void aSlideDesignationViolation_isACollision() {
        Throwable failure = nested("could not execute statement",
                "ERROR: duplicate key value violates unique constraint \"pathology_slide_designation_uk\"");

        assertTrue("slides are numbered within their block and race the same way",
                PathologyController.concurrentSaveConflict(failure));
    }

    @Test
    public void aLostVersionCheck_isACollision() {
        Throwable hibernate = new RuntimeException("could not save the case",
                new org.hibernate.StaleObjectStateException("PathologySample", 1));
        Throwable jpa = new RuntimeException(new jakarta.persistence.OptimisticLockException("Row was updated"));
        Throwable spring = new org.springframework.orm.ObjectOptimisticLockingFailureException("PathologySample", 1);

        assertTrue("two saves adding a cassette at once usually end on the case's version check, before either"
                + " reaches the designation index", PathologyController.concurrentSaveConflict(hibernate));
        assertTrue("whichever layer reports it", PathologyController.concurrentSaveConflict(jpa));
        assertTrue("including Spring's translation of it", PathologyController.concurrentSaveConflict(spring));
    }

    @Test
    public void anUnrelatedConstraint_isNotACollision() {
        Throwable failure = nested("could not execute statement",
                "ERROR: duplicate key value violates unique constraint \"pathology_block_pkey\"");

        assertFalse(
                "a failure on any other constraint is not the designation race, and answering it as a"
                        + " conflict would tell the bench to retry something that cannot succeed",
                PathologyController.concurrentSaveConflict(failure));
    }

    @Test
    public void aFailureWithNothingToWalk_isNotACollision() {
        assertFalse("nothing to walk is nothing to match", PathologyController.concurrentSaveConflict(null));
        assertFalse("and neither is a cause chain carrying no message", PathologyController
                .concurrentSaveConflict(new RuntimeException((String) null, new RuntimeException((String) null))));
    }

    /** A top-level failure whose innermost cause carries the database's message. */
    private Throwable nested(String outerMessage, String rootMessage) {
        return new RuntimeException(outerMessage,
                new IllegalStateException((String) null, new RuntimeException(rootMessage)));
    }
}
