package org.openelisglobal.resultrun.form;

import java.io.Serializable;

/**
 * The run a pending analysis has a value waiting in (OGC-1200 FR-D3). Kept on
 * worklist rows, which live in the session, so it is serializable. Held when a
 * control for the row's test failed in that run (FR-D4).
 */
public record WaitingRun(Long runId, String label, String source, String openedAt,
        boolean held) implements Serializable {

    public WaitingRun held(boolean isHeld) {
        return new WaitingRun(runId, label, source, openedAt, isHeld);
    }
}
