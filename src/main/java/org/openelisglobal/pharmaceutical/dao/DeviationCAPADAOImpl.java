package org.openelisglobal.pharmaceutical.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.pharmaceutical.valueholder.DeviationCAPA;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DeviationCAPADAOImpl extends BaseDAOImpl<DeviationCAPA, Integer> implements DeviationCAPADAO {

    public DeviationCAPADAOImpl() {
        super(DeviationCAPA.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findByAssayRunId(Integer assayRunId) {
        try {
            String hql = "FROM DeviationCAPA WHERE assayRunId = :assayRunId ORDER BY createdAt DESC";
            Query<DeviationCAPA> query = entityManager.unwrap(Session.class).createQuery(hql, DeviationCAPA.class);
            query.setParameter("assayRunId", assayRunId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding DeviationCAPAs by assayRunId", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findByStatus(DeviationCAPA.CAPAStatus status) {
        try {
            String hql = "FROM DeviationCAPA WHERE status = :status ORDER BY createdAt DESC";
            Query<DeviationCAPA> query = entityManager.unwrap(Session.class).createQuery(hql, DeviationCAPA.class);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding DeviationCAPAs by status", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviationCAPA> findOpenCAPAs() {
        try {
            String hql = "FROM DeviationCAPA WHERE status NOT IN ('CLOSED', 'REJECTED') ORDER BY dueDate ASC";
            Query<DeviationCAPA> query = entityManager.unwrap(Session.class).createQuery(hql, DeviationCAPA.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding open DeviationCAPAs", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DeviationCAPA findByDeviationNumber(String deviationNumber) {
        try {
            String hql = "FROM DeviationCAPA WHERE deviationNumber = :deviationNumber";
            Query<DeviationCAPA> query = entityManager.unwrap(Session.class).createQuery(hql, DeviationCAPA.class);
            query.setParameter("deviationNumber", deviationNumber);
            query.setMaxResults(1);
            List<DeviationCAPA> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding DeviationCAPA by deviationNumber", e);
        }
    }
}
