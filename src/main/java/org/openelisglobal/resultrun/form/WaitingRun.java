package org.openelisglobal.resultrun.form;

import java.io.Serializable;

/**
 * The run a pending analysis has a value waiting in (OGC-1200 FR-D3). Kept on
 * worklist rows, which live in the session, so it is serializable.
 */
public record WaitingRun(Long runId, String label, String source, String openedAt) implements Serializable {
}
