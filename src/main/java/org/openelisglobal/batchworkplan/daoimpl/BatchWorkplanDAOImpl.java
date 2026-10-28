package org.openelisglobal.batchworkplan.daoimpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getPatientNamesBySampleIds(Collection<String> sampleIds) {
        Map<String, String> names = new HashMap<>();
        if (sampleIds == null || sampleIds.isEmpty()) {
            return names;
        }
        try {
            String hql = "SELECT sh.sampleId, p.person.lastName, p.person.firstName FROM SampleHuman sh, Patient p"
                    + " WHERE p.id = sh.patientId AND sh.sampleId IN (:sampleIds)";
            Query<Object[]> query = entityManager.unwrap(Session.class).createQuery(hql, Object[].class);
            query.setParameterList("sampleIds", sampleIds);
            for (Object[] row : query.list()) {
                String last = StringUtils.trimToEmpty((String) row[1]);
                String first = StringUtils.trimToEmpty((String) row[2]);
                String name = last.isEmpty() || first.isEmpty() ? last + first : last + ", " + first;
                if (!name.isEmpty()) {
                    names.put(String.valueOf(row[0]), name);
                }
            }
            return names;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting patient names for batch workplan samples", e);
        }
    }
}
