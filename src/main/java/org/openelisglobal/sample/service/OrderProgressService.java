package org.openelisglobal.sample.service;

import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * The order's progress through order entry (OGC-1266, FR-F5, FR-A4, FR-F3,
 * FR-F4): an explicit status the steps' saves advance, the release through the
 * optional Sample check, and cancellation with a reason.
 */
public interface OrderProgressService {

    /**
     * Records a step's save on the order: the first save marks it Entered, and a
     * save that completes Prepare Samples (progressStep SAMPLES_PREPARED) marks it
     * Samples prepared. Later saves never move the status back. A cancelled order
     * refuses the save. The step's storage decision (skip storage for the
     * unassigned samples) is stored with it when the save carries one.
     */
    void recordStepSave(Sample sample, String progressStep, Boolean storageSkipped);

    /**
     * The stored status, or the one the legacy step flags imply when none is
     * stored.
     */
    OrderProgressStatus statusOf(Sample sample, boolean legacyPrepared, boolean legacyReleased);

    /**
     * Whether the Sample check step is in use for the domain (its setting is not
     * Off).
     */
    boolean sampleCheckEnabled(String workflowType);

    /**
     * Whether the order has finished order entry: Samples prepared with the Sample
     * check off, or Ready for testing with it on.
     */
    boolean isComplete(OrderProgressStatus status, String workflowType);

    /**
     * Releases the order for testing from the Sample check step. Under Mandatory
     * acceptance the gate must be satisfied; under Optional a release with items
     * unanswered needs a note, which is recorded with who released and when.
     */
    Sample release(String sampleId, String releaseNote, String sysUserId);

    /**
     * Cancels an order that is not yet complete, with a reason: its tests are
     * cancelled and the order is marked Cancelled. Nothing is deleted.
     */
    Sample cancel(String sampleId, String reason, String sysUserId);
}
