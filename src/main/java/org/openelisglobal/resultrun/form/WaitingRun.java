package org.openelisglobal.resultrun.form;

import java.io.Serializable;

/**
 * The run a pending analysis has a value waiting in (OGC-1200 FR-D3). Kept on
 * worklist rows, which live in the session, so it is serializable. Held when
 * the run holds the row's test, for a failed control or a required control that
 * is missing (FR-D4); {@code heldCause} says which.
 */
public record WaitingRun(Long runId, String label, String source, String openedAt, boolean held,
        String heldCause) implements Serializable {

    public WaitingRun held(RunHoldCause cause) {
        return new WaitingRun(runId, label, source, openedAt, cause != null, cause == null ? null : cause.name());
    }

    /** The message that refuses an entry the run holds or waits for. */
    public String refusalKey() {
        if (!held) {
            return "run.refusal.waiting";
        }
        return RunHoldCause.QC_MISSING.name().equals(heldCause) ? "run.refusal.heldMissing" : "run.refusal.held";
    }
}
