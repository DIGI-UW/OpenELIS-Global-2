package org.openelisglobal.qc.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCAlert entity.
 */
@Component
@Transactional
public class QCAlertDAOImpl extends BaseDAOImpl<QCAlert, String> implements QCAlertDAO {

    public QCAlertDAOImpl() {
        super(QCAlert.class);
    }

    @Override
    public List<QCAlert> findByViolation(String violationId) throws LIMSRuntimeException {
        String hql = "FROM QCAlert WHERE violationId = :violationId ORDER BY sentDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCAlert> query = session.createQuery(hql, QCAlert.class);
            query.setParameter("violationId", violationId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by violation", e);
        }
    }

    @Override
    public List<QCAlert> findByRecipient(Integer userId) throws LIMSRuntimeException {
        String hql = "FROM QCAlert WHERE recipientUserId = :userId ORDER BY sentDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCAlert> query = session.createQuery(hql, QCAlert.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by recipient", e);
        }
    }

    @Override
    public List<QCAlert> findUnreadByRecipient(Integer userId) throws LIMSRuntimeException {
        String hql = "FROM QCAlert WHERE recipientUserId = :userId AND readStatus = false ORDER BY sentDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCAlert> query = session.createQuery(hql, QCAlert.class);
            query.setParameter("userId", userId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unread QC alerts by recipient", e);
        }
    }

    @Override
    public List<QCAlert> findByReadStatus(Boolean readStatus) throws LIMSRuntimeException {
        String hql = "FROM QCAlert WHERE readStatus = :readStatus ORDER BY sentDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCAlert> query = session.createQuery(hql, QCAlert.class);
            query.setParameter("readStatus", readStatus);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by read status", e);
        }
    }
}
