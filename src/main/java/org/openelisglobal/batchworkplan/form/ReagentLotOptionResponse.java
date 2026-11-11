package org.openelisglobal.batchworkplan.form;

import java.sql.Timestamp;

/**
 * A usable reagent lot, in the first-expiry-first-out order the Results page
 * offers; {@code suggested} marks the first.
 */
public record ReagentLotOptionResponse(Long id, String lotNumber, Timestamp expirationDate, Double currentQuantity,
        String units, boolean suggested) {
}
