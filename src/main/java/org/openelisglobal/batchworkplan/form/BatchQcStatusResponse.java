package org.openelisglobal.batchworkplan.form;

import java.util.List;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;

/**
 * A batch's combined QC standing: the worst status across every control lot of
 * every test in it, plus the tests no control lot covers at all.
 *
 * @param overridable true when a user holding {@code qa.qc.override} may
 *                    release the batch anyway, raising an NCE
 */
public record BatchQcStatusResponse(BatchQcStatus status, boolean blocking, boolean overridable,
        boolean evaluationPending, String instrumentId, String instrumentName, List<ControlLotQcResponse> lots,
        List<String> testsWithoutControlLot) {
}
