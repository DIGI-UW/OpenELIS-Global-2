package org.openelisglobal.batchworkplan.form;

import java.util.List;

/**
 * What a batch can be set up with: the reagents its tests are linked to and the
 * analyzers bound to all of its tests. A batch may always run on the bench.
 */
public record BatchSetupOptionsResponse(List<ReagentOptionResponse> reagents,
        List<InstrumentOptionResponse> instruments) {
}
