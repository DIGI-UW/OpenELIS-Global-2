package org.openelisglobal.resultrun.form;

import java.util.List;

/**
 * The run header's Run settings and progress (OGC-1200 FR-E3, FR-E5), the chip
 * counts over the whole run (FR-E1a), the QC block (FR-E2) and the run's
 * unresolved exceptions (FR-E4). A workplan run also names its reagent lot and
 * the batch it is (FR-B2, FR-E3).
 */
public record ResultRunHeader(Long id, String label, String source, String state, String analyzerId,
        String analyzerName, String method, List<String> labUnits, String operatorName, String openedAt,
        String lastActivityAt, String completedAt, int dispositioned, int total, boolean rawAvailable,
        ResultRunChipCounts chipCounts, RunQcBlock qc, List<ResultRunException> exceptions, String reagentLot,
        Long batchWorkplanId) {
}
