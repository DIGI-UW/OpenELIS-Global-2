package org.openelisglobal.microbiology.form;

/**
 * Counts for the action-first worklist summary. These counts intentionally
 * exclude selected stage and due-action filters so a summary tile remains
 * useful for moving between queues.
 */
public class MicroWorklistSummaryForm {

    public int totalPending;
    public int incubating;
    public int positiveSignals;
    public int growthDetected;
    public int identification;
    public int needsAstReview;
    public int readyForCaseReview;
    public int openCriticalFollowUps;
    public int astInQueue;
    public int astPendingSetup;
    public int astInProgress;
    public int astAwaitingResults;
    public int astResultsIn;
}
