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
            // Back to DRAFT is Edit (OGC-427 FR-BL-003): the batch is set up again and
            // released again through the QC check.
            return next == DRAFT || next == COMPLETED || next == ARCHIVED;
        case COMPLETED:
            return next == ARCHIVED;
        case ARCHIVED:
        default:
            return false;
        }
    }
}
