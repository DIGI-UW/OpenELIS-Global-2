package org.openelisglobal.batchworkplan.form;

import java.util.List;

/**
 * A reagent linked to at least one test in the batch, with its usable lots and
 * the most one of those tests uses of it per result ({@code quantityPerTest},
 * null when no link names a quantity).
 */
public record ReagentOptionResponse(Long id, String name, String units, List<String> testNames,
        List<ReagentLotOptionResponse> lots, java.math.BigDecimal quantityPerTest) {
}
