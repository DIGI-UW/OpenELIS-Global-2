package org.openelisglobal.batchworkplan.daoimpl;

import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BatchWorkplanDAOImpl extends BaseDAOImpl<BatchWorkplan, Long> implements BatchWorkplanDAO {

    public BatchWorkplanDAOImpl() {
        super(BatchWorkplan.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchWorkplan> getForUserInStatuses(Integer createdByUserId, List<BatchWorkplanStatus> statuses) {
        // An unresolvable caller owns nothing. Answering "every batch" here would
        // undo the point of the predicate.
        if (createdByUserId == null || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        try {
            String hql = "SELECT DISTINCT b FROM BatchWorkplan b LEFT JOIN FETCH b.items"
                    + " WHERE b.createdByUserId = :createdByUserId AND b.status IN (:statuses)"
                    + " ORDER BY b.createdAt DESC";
            Query<BatchWorkplan> query = entityManager.unwrap(Session.class).createQuery(hql, BatchWorkplan.class);
            query.setParameter("createdByUserId", createdByUserId);
            query.setParameterList("statuses", statuses);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting batch workplans", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchWorkplan> getWithItems(Long id) {
        try {
            String hql = "SELECT DISTINCT b FROM BatchWorkplan b LEFT JOIN FETCH b.items WHERE b.id = :id";
            Query<BatchWorkplan> query = entityManager.unwrap(Session.class).createQuery(hql, BatchWorkplan.class);
            query.setParameter("id", id);
            query.setMaxResults(1);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting batch workplan", e);
        }
    }
}
