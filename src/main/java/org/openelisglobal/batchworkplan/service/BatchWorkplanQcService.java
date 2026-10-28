package org.openelisglobal.batchworkplan.service;

import java.util.Collection;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;

public interface BatchWorkplanQcService {

    /**
     * The combined QC standing of a batch's tests on one analyzer, or on the bench
     * when {@code instrumentId} is null (OGC-427 FR-QC-002/003). Every usable
     * control lot of each (test, analyzer) pair is evaluated, one per control
     * level, and the worst status wins.
     */
    BatchQcStatusResponse evaluate(Collection<BatchTestRef> tests, String instrumentId);
}
