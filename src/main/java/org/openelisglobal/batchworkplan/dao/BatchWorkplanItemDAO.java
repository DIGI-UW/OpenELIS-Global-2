package org.openelisglobal.batchworkplan.dao;

import java.util.List;
import java.util.Set;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.dao.BaseDAO;

public interface BatchWorkplanItemDAO extends BaseDAO<BatchWorkplanItem, Long> {

    Set<String> getAnalysisIdsInStatuses(List<BatchWorkplanStatus> statuses);

    Set<String> getExistingAnalysisIds(List<String> analysisIds, List<BatchWorkplanStatus> statuses);

    /**
     * As {@link #getExistingAnalysisIds}, ignoring one batch's own items: an edit
     * that keeps an analysis in its batch is not claiming it from another.
     */
    Set<String> getExistingAnalysisIdsOutsideBatch(List<String> analysisIds, List<BatchWorkplanStatus> statuses,
            Long batchWorkplanId);
}
