package org.openelisglobal.batchworkplan.valueholder;

public enum BatchWorkplanStatus {
    DRAFT, ACTIVE, COMPLETED, ARCHIVED;

    public boolean canTransitionTo(BatchWorkplanStatus next) {
        if (next == null || this == next) {
            return false;
        }
        switch (this) {
        case DRAFT:
            return next == ACTIVE || next == ARCHIVED;
        case ACTIVE:
            return next == COMPLETED || next == ARCHIVED;
        case COMPLETED:
            return next == ARCHIVED;
        case ARCHIVED:
        default:
            return false;
        }
    }
}
