package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
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
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCAlert> cq = cb.createQuery(QCAlert.class);
            Root<QCAlert> root = cq.from(QCAlert.class);
            cq.where(cb.equal(root.get("violationId"), violationId));
            cq.orderBy(cb.desc(root.get("sentDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by violation", e);
        }
    }

    @Override
    public List<QCAlert> findByRecipient(Integer userId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCAlert> cq = cb.createQuery(QCAlert.class);
            Root<QCAlert> root = cq.from(QCAlert.class);
            cq.where(cb.equal(root.get("recipientUserId"), userId));
            cq.orderBy(cb.desc(root.get("sentDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by recipient", e);
        }
    }

    @Override
    public List<QCAlert> findUnreadByRecipient(Integer userId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCAlert> cq = cb.createQuery(QCAlert.class);
            Root<QCAlert> root = cq.from(QCAlert.class);
            cq.where(cb.equal(root.get("recipientUserId"), userId), cb.equal(root.get("readStatus"), false));
            cq.orderBy(cb.desc(root.get("sentDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unread QC alerts by recipient", e);
        }
    }

    @Override
    public List<QCAlert> findByReadStatus(Boolean readStatus) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCAlert> cq = cb.createQuery(QCAlert.class);
            Root<QCAlert> root = cq.from(QCAlert.class);
            cq.where(cb.equal(root.get("readStatus"), readStatus));
            cq.orderBy(cb.desc(root.get("sentDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC alerts by read status", e);
        }
    }
}
