package org.openelisglobal.program.bean;

public class CytologyDashBoardCount {
    Long inProgress;

    Long awaitingReview;

    Long complete;

    public Long getInProgress() {
        return inProgress;
    }

    public void setInProgress(Long inProgress) {
        this.inProgress = inProgress;
    }

    public Long getAwaitingReview() {
        return awaitingReview;
    }

    public void setAwaitingReview(Long awaitingReview) {
        this.awaitingReview = awaitingReview;
    }

    public Long getComplete() {
        return complete;
    }

    public void setComplete(Long complete) {
        this.complete = complete;
    }
}
