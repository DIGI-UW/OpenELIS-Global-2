package org.openelisglobal.batchworkplan.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.batchworkplan.form.PendingTestsQuery;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.dao.BaseDAO;

public interface BatchWorkplanDAO extends BaseDAO<BatchWorkplan, Long> {

    /**
     * One user's batches in the given statuses, newest first. Both predicates
     * matter to the working view: a batch belongs to the technician who built it,
     * and an archived batch is kept for audit but is no longer work in hand.
     */
    List<BatchWorkplan> getForUserInStatuses(Integer createdByUserId, List<BatchWorkplanStatus> statuses);

    Optional<BatchWorkplan> getWithItems(Long id);

    /**
     * "Last, First" for the patient on each sample, keyed by sample id. One query
     * for the whole pending list rather than one per row.
     */
    Map<String, String> getPatientNamesBySampleIds(Collection<String> sampleIds);

    /**
     * The ids of one page of pending analyses: in the given statuses, of a test the
     * user may work, in no open batch, and matching the query's filters and search,
     * in the query's order.
     */
    List<String> getPendingAnalysisIds(List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, PendingTestsQuery query);

    /** How many pending analyses match, across all pages. */
    long countPendingAnalyses(List<String> statusIds, List<String> testIds, Collection<String> excludedAnalysisIds,
            PendingTestsQuery query);

    /**
     * The distinct values one filter can take across every pending analysis the
     * user may work, as [id, name] rows; {@code field} is test, panel, unit or
     * priority.
     */
    List<Object[]> getPendingFilterValues(List<String> statusIds, List<String> testIds,
            Collection<String> excludedAnalysisIds, String field);
}
