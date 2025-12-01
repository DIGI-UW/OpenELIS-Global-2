package org.openelisglobal.qc.dao;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
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
    public List<QCRuleViolation> findByInstrument(Integer instrumentId) throws LIMSRuntimeException {
        String hql = "FROM QCRuleViolation WHERE instrumentId = :instrumentId ORDER BY violationDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCRuleViolation> query = session.createQuery(hql, QCRuleViolation.class);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by instrument", e);
        }
    }

    @Override
    public List<QCRuleViolation> findUnresolved() throws LIMSRuntimeException {
        String hql = "FROM QCRuleViolation WHERE resolutionStatus = 'UNRESOLVED' ORDER BY violationDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCRuleViolation> query = session.createQuery(hql, QCRuleViolation.class);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unresolved QC violations", e);
        }
    }

    @Override
    public List<QCRuleViolation> findBySeverity(String severity) throws LIMSRuntimeException {
        String hql = "FROM QCRuleViolation WHERE severity = :severity ORDER BY violationDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCRuleViolation> query = session.createQuery(hql, QCRuleViolation.class);
            query.setParameter("severity", severity);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by severity", e);
        }
    }

    @Override
    public List<QCRuleViolation> findByInstrumentAndDateRange(Integer instrumentId, Timestamp startDate,
            Timestamp endDate) throws LIMSRuntimeException {
        String hql = "FROM QCRuleViolation WHERE instrumentId = :instrumentId AND violationDateTime >= :startDate AND violationDateTime <= :endDate ORDER BY violationDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCRuleViolation> query = session.createQuery(hql, QCRuleViolation.class);
            query.setParameter("instrumentId", instrumentId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving QC violations by instrument and date range", e);
        }
    }

    @Override
    public List<QCRuleViolation> findUnresolvedByInstrument(Integer instrumentId) throws LIMSRuntimeException {
        String hql = "FROM QCRuleViolation WHERE instrumentId = :instrumentId AND resolutionStatus = 'UNRESOLVED' ORDER BY violationDateTime DESC";
        try {
            Session session = entityManager.unwrap(Session.class);
            Query<QCRuleViolation> query = session.createQuery(hql, QCRuleViolation.class);
            query.setParameter("instrumentId", instrumentId);
            return query.list();
        } catch (RuntimeException e) {
            throw new LIMSRuntimeException("Error retrieving unresolved QC violations by instrument", e);
        }
    }
}
