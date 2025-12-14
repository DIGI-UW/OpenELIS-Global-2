package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.QCCheck;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class QCCheckDAOImpl extends BaseDAOImpl<QCCheck, Integer> implements QCCheckDAO {

    public QCCheckDAOImpl() {
        super(QCCheck.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCheck> findBySampleId(Integer sampleId) {
        try {
            String hql = "FROM QCCheck WHERE sample.id = :sampleId ORDER BY createdAt DESC";
            Query<QCCheck> query = entityManager.unwrap(Session.class).createQuery(hql, QCCheck.class);
            query.setParameter("sampleId", sampleId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QCChecks by sampleId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCheck> findByOutcome(QCCheck.QCOutcome outcome) {
        try {
            String hql = "FROM QCCheck WHERE outcome = :outcome ORDER BY createdAt DESC";
            Query<QCCheck> query = entityManager.unwrap(Session.class).createQuery(hql, QCCheck.class);
            query.setParameter("outcome", outcome);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding QCChecks by outcome", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QCCheck findLatestBySampleId(Integer sampleId) {
        try {
            String hql = "FROM QCCheck WHERE sample.id = :sampleId ORDER BY createdAt DESC";
            Query<QCCheck> query = entityManager.unwrap(Session.class).createQuery(hql, QCCheck.class);
            query.setParameter("sampleId", sampleId);
            query.setMaxResults(1);
            List<QCCheck> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding latest QCCheck by sampleId", e);
        }
    }
}
