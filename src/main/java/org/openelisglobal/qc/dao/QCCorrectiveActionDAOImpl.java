package org.openelisglobal.qc.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCCorrectiveAction entity.
 */
@Component
@Transactional
public class QCCorrectiveActionDAOImpl extends BaseDAOImpl<QCCorrectiveAction, String>
        implements QCCorrectiveActionDAO {

    public QCCorrectiveActionDAOImpl() {
        super(QCCorrectiveAction.class);
    }

    @Override
    public List<QCCorrectiveAction> findByViolation(String violationId) throws LIMSRuntimeException {
        String hql = "FROM QCCorrectiveAction WHERE violationId = :violationId ORDER BY createdDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCCorrectiveAction> query = session.createQuery(hql, QCCorrectiveAction.class);
            query.setParameter("violationId", violationId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving corrective actions by violation", e);
        }
    }

    @Override
    public List<QCCorrectiveAction> findByAssignedUser(Integer userId) throws LIMSRuntimeException {
        String hql = "FROM QCCorrectiveAction WHERE assignedUserId = :userId ORDER BY createdDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCCorrectiveAction> query = session.createQuery(hql, QCCorrectiveAction.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving corrective actions by assigned user", e);
        }
    }

    @Override
    public List<QCCorrectiveAction> findByStatus(String status) throws LIMSRuntimeException {
        String hql = "FROM QCCorrectiveAction WHERE status = :status ORDER BY createdDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCCorrectiveAction> query = session.createQuery(hql, QCCorrectiveAction.class);
            query.setParameter("status", status);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving corrective actions by status", e);
        }
    }

    @Override
    public List<QCCorrectiveAction> findPendingByAssignedUser(Integer userId) throws LIMSRuntimeException {
        String hql = "FROM QCCorrectiveAction WHERE assignedUserId = :userId AND status = 'PENDING' ORDER BY createdDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCCorrectiveAction> query = session.createQuery(hql, QCCorrectiveAction.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving pending corrective actions by assigned user", e);
        }
    }
}
