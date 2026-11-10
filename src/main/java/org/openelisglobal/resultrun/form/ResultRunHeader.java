package org.openelisglobal.resultrun.form;

import java.util.List;

/** The run header's Run settings and progress (OGC-1200 FR-E3, FR-E5). */
public record ResultRunHeader(Long id, String label, String source, String state, String analyzerId,
        String analyzerName, String method, List<String> labUnits, String operatorName, String openedAt,
        String lastActivityAt, String completedAt, int dispositioned, int total, boolean rawAvailable) {
}
