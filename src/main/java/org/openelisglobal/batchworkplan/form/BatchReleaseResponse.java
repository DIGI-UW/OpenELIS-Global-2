package org.openelisglobal.batchworkplan.form;

import java.sql.Timestamp;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;

/**
 * One control lot checked at a batch's release, as the batch tile and the
 * printed workplan show it.
 */
public record BatchReleaseResponse(String controlLotId, String lotNumber, String testName, BatchQcStatus qcStatus,
        boolean overridden, Integer nceId, String nceNumber, Timestamp releasedAt, String releasedBy) {
}
