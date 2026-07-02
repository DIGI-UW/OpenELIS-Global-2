package org.openelisglobal.batchworkplan.service;

import java.util.List;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;

public interface BatchWorkplanService {

    List<PendingBatchTestResponse> getPendingTests(Integer limit);

    List<BatchWorkplanResponse> getBatches();

    BatchWorkplanResponse createBatch(BatchWorkplanRequest request, String sysUserId);

    BatchWorkplanResponse transitionBatch(Long id, BatchWorkplanStatus nextStatus, String sysUserId);
}
