package org.openelisglobal.resultrun.form;

/** One run in the Runs chip's list (OGC-1200 FR-D1). */
public record ResultRunSummary(Long id, String label, String source, String state, String analyzerName, String openedAt,
        int dispositioned, int total) {
}
