package org.openelisglobal.batchworkplan.form;

import java.util.List;

/** A reagent linked to at least one test in the batch, with its usable lots. */
public record ReagentOptionResponse(Long id, String name, String units, List<String> testNames,
        List<ReagentLotOptionResponse> lots) {
}
