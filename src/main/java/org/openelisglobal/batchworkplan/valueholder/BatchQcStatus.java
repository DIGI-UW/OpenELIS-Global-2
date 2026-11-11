package org.openelisglobal.batchworkplan.valueholder;

/**
 * The QC standing of a batch workplan's control lots (OGC-427 FR-QC-003/004),
 * declared from most to least severe so that the worst of several lots is the
 * one with the lowest ordinal.
 *
 * <p>
 * The two lifecycle states that no override can clear come first: with no
 * control lot configured, or only retired ones, there is nothing a release
 * could be checked against. The remaining non-pass states can be released by a
 * user holding {@code qa.qc.override}, which raises a non-conforming event.
 */
public enum BatchQcStatus {

    NO_CONTROL_LOT(true),

    LOT_EXPIRED(true),

    ESTABLISHMENT(false),

    VIOLATION(false),

    FAILED(false),

    OVERDUE(false),

    NOT_RUN(false),

    PASS(false);

    private final boolean blocking;

    BatchQcStatus(boolean blocking) {
        this.blocking = blocking;
    }

    /** True when no override may release a batch in this state. */
    public boolean isBlocking() {
        return blocking;
    }

    public boolean isPass() {
        return this == PASS;
    }

    public static BatchQcStatus worst(BatchQcStatus a, BatchQcStatus b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
