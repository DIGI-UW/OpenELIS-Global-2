package org.openelisglobal.batchworkplan.daoimpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanItemDAO;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BatchWorkplanItemDAOImpl extends BaseDAOImpl<BatchWorkplanItem, Long> implements BatchWorkplanItemDAO {

    public BatchWorkplanItemDAOImpl() {
        super(BatchWorkplanItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAnalysisIdsInStatuses(List<BatchWorkplanStatus> statuses) {
        try {
            String hql = "SELECT i.analysisId FROM BatchWorkplanItem i WHERE i.batchWorkplan.status IN (:statuses)";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            query.setParameterList("statuses", statuses);
            return new HashSet<>(query.list());
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting assigned batch workplan analysis ids", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getExistingAnalysisIds(List<String> analysisIds, List<BatchWorkplanStatus> statuses) {
        if (analysisIds == null || analysisIds.isEmpty()) {
            return new HashSet<>();
        }
        try {
            String hql = "SELECT i.analysisId FROM BatchWorkplanItem i WHERE i.analysisId IN (:analysisIds)"
                    + " AND i.batchWorkplan.status IN (:statuses)";
            Query<String> query = entityManager.unwrap(Session.class).createQuery(hql, String.class);
            query.setParameterList("analysisIds", analysisIds);
            query.setParameterList("statuses", statuses);
            return new HashSet<>(query.list());
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error checking batch workplan analysis ids", e);
        }
    }
}
