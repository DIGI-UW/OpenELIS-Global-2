package org.openelisglobal.reports.dataexport.valueholder;

public enum ExportJobState {
    QUEUED, GENERATING, READY, FAILED, EXPIRED, CANCELLED;

    public boolean canTransitionTo(ExportJobState next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
        case QUEUED -> next == GENERATING || next == FAILED || next == CANCELLED;
        case GENERATING -> next == READY || next == FAILED;
        case READY -> next == EXPIRED;
        case FAILED, EXPIRED, CANCELLED -> false;
        };
    }
}
