package org.openelisglobal.qc.dao;

import java.util.Optional;
import java.util.Set;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.TestQcThreshold;

public interface TestQcThresholdDAO extends BaseDAO<TestQcThreshold, String> {

    Optional<TestQcThreshold> findByTestId(Integer testId) throws LIMSRuntimeException;

    /**
     * Returns the set of test IDs that have a configured QC threshold row. Used by
     * callers that need to check threshold-existence for many tests at once (e.g.
     * enabling/disabling QC quick-add buttons per test).
     */
    Set<Integer> findAllConfiguredTestIds() throws LIMSRuntimeException;
}
