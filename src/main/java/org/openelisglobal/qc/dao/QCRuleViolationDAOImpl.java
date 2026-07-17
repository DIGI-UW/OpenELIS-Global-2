package org.openelisglobal.qc.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QCRuleViolation entity.
 */
@Component
@Transactional
public class QCRuleViolationDAOImpl extends BaseDAOImpl<QCRuleViolation, String> implements QCRuleViolationDAO {

    public QCRuleViolationDAOImpl() {
        super(QCRuleViolation.class);
    }

    @Override
    public List<QCRuleViolation> findByInstrument(String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by instrument", e);
        }
    }

    @Override
    public List<QCRuleViolation> findUnresolved() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("resolutionStatus"), "UNRESOLVED"));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unresolved QC violations", e);
        }
    }

    @Override
    public List<QCRuleViolation> findBySeverity(String severity) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("severity"), severity));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by severity", e);
        }
    }

    @Override
    public List<QCRuleViolation> findByInstrumentAndDateRange(String instrumentId, Timestamp startDate,
            Timestamp endDate) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId),
                    cb.greaterThanOrEqualTo(root.get("violationDateTime"), startDate),
                    cb.lessThanOrEqualTo(root.get("violationDateTime"), endDate));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by instrument and date range", e);
        }
    }

    @Override
    public List<QCRuleViolation> findUnresolvedByInstrument(String instrumentId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("instrumentId"), instrumentId),
                    cb.equal(root.get("resolutionStatus"), "UNRESOLVED"));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unresolved QC violations by instrument", e);
        }
    }

    @Override
    public List<QCRuleViolation> findByTriggeringResultId(String triggeringResultId) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<QCRuleViolation> cq = cb.createQuery(QCRuleViolation.class);
            Root<QCRuleViolation> root = cq.from(QCRuleViolation.class);
            cq.where(cb.equal(root.get("triggeringResultId"), triggeringResultId));
            cq.orderBy(cb.desc(root.get("violationDateTime")));
            return entityManager.createQuery(cq).getResultList();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by triggering result ID", e);
        }
    }
}
