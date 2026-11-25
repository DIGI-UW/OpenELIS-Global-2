package org.openelisglobal.batchworkplan.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
