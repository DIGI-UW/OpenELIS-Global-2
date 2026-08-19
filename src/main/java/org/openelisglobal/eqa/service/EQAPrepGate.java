package org.openelisglobal.eqa.service;

import java.util.List;
import org.openelisglobal.eqa.valueholder.EQAPanel;

/**
 * One evaluation of the ready-to-ship gate: AC-V2.1-13's homogeneity QC half
 * and FR-V2.5-12's inventory half, together.
 *
 * <p>
 * Both readers of the gate share this evaluation — the cycle transition refuses
 * the move while {@link #blockers()} is non-empty, and the prep workbench
 * renders the same numbers and the same sentences. One rule, one
 * implementation, one vocabulary; nothing to drift.
 */
public record EQAPrepGate(int participantCount, List<PanelRequirement> panels, List<String> blockers) {

    /** What FR-V2.5-12 requires of one panel, against what the panel holds. */
    public record PanelRequirement(EQAPanel panel, int sampleCount, int aliquotsNeeded) {

        public int produced() {
            return panel.getAliquotsProduced() == null ? 0 : panel.getAliquotsProduced();
        }

        public int shortfall() {
            return Math.max(0, aliquotsNeeded - produced());
        }
    }

    public boolean isClear() {
        return blockers.isEmpty();
    }
}
