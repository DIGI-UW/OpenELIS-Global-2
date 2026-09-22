package org.openelisglobal.batchworkplan.service;

import java.util.List;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;

public interface BatchWorkplanService {

    /**
     * Pending tests the given user is allowed to work, scoped to the lab units
     * their Results role covers. A user with no lab unit assignment sees nothing,
     * which is how every other workplan screen behaves.
     */
    List<PendingBatchTestResponse> getPendingTests(Integer limit, String sysUserId);

    List<BatchWorkplanResponse> getBatches();

    BatchWorkplanResponse createBatch(BatchWorkplanRequest request, String sysUserId);

    BatchWorkplanResponse transitionBatch(Long id, BatchWorkplanStatus nextStatus, String sysUserId);
}
