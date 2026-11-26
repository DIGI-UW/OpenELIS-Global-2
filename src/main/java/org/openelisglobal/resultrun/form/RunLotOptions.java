package org.openelisglobal.resultrun.form;

import java.util.List;
import org.openelisglobal.batchworkplan.form.ReagentOptionResponse;

/**
 * OGC-1200 FR-E3: the reagent lots a workplan run may record, from the reagents
 * linked to its tests; the lot it records now; and the lot last recorded on
 * another run among them, for "Use last-used lot". Nothing is pre-selected.
 */
public record RunLotOptions(List<ReagentOptionResponse> reagents, Long currentLotId, Long lastUsedLotId) {
}
