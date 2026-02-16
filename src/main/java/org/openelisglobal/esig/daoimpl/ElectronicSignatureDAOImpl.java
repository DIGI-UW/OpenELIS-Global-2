package org.openelisglobal.esig.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.esig.dao.ElectronicSignatureDAO;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data access implementation for electronic signatures and first-use
 * certifications.
 */
@Component
@Transactional
public class ElectronicSignatureDAOImpl extends BaseDAOImpl<ElectronicSignature, Long>
        implements ElectronicSignatureDAO {

    public ElectronicSignatureDAOImpl() {
        super(ElectronicSignature.class);
    }

    // ========================
    // Signature Queries
    // ========================

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesByRecord(String recordType, Long recordId)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM ElectronicSignature e WHERE e.recordType = :recordType "
                    + "AND e.recordId = :recordId ORDER BY e.signedAt ASC";
            Query<ElectronicSignature> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ElectronicSignature.class);
            query.setParameter("recordType", recordType);
            query.setParameter("recordId", recordId);
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getSignaturesByRecord()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesBySigner(Long signerId) throws LIMSRuntimeException {
        try {
            String sql = "FROM ElectronicSignature e WHERE e.signerId = :signerId " + "ORDER BY e.signedAt DESC";
            Query<ElectronicSignature> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ElectronicSignature.class);
            query.setParameter("signerId", signerId);
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getSignaturesBySigner()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesInDateRange(Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        try {
            String sql = "FROM ElectronicSignature e WHERE e.signedAt BETWEEN :startDate AND :endDate "
                    + "ORDER BY e.signedAt DESC";
            Query<ElectronicSignature> query = entityManager.unwrap(Session.class).createQuery(sql,
                    ElectronicSignature.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getSignaturesInDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesByMeaning(SignatureMeaning meaning) throws LIMSRuntimeException {
        try {
            return entityManager.unwrap(Session.class)
                    .createQuery("FROM ElectronicSignature e WHERE e.signatureMeaning = :meaning "
                            + "ORDER BY e.signedAt DESC", ElectronicSignature.class)
                    .setParameter("meaning", meaning.name()).list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getSignaturesByMeaning()", e);
        }
    }

    // ========================
    // First-Use Certification
    // ========================

    @Override
    @Transactional(readOnly = true)
    public boolean isUserCertified(Long userId) throws LIMSRuntimeException {
        try {
            String sql = "SELECT COUNT(c) FROM EsigFirstUseCertification c WHERE c.userId = :userId";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("userId", userId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in isUserCertified()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EsigFirstUseCertification getCertificationByUserId(Long userId) throws LIMSRuntimeException {
        try {
            String sql = "FROM EsigFirstUseCertification c WHERE c.userId = :userId";
            Query<EsigFirstUseCertification> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EsigFirstUseCertification.class);
            query.setParameter("userId", userId);
            return query.uniqueResult();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCertificationByUserId()", e);
        }
    }

    @Override
    @Transactional
    public Long insertCertification(EsigFirstUseCertification certification) throws LIMSRuntimeException {
        try {
            entityManager.persist(certification);
            entityManager.flush();
            return certification.getId();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in insertCertification()", e);
        }
    }

    @Override
    @Transactional
    public void deleteCertification(EsigFirstUseCertification certification) throws LIMSRuntimeException {
        try {
            EsigFirstUseCertification managed = entityManager.merge(certification);
            entityManager.remove(managed);
            entityManager.flush();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in deleteCertification()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EsigFirstUseCertification> getAllCertifications() throws LIMSRuntimeException {
        try {
            String sql = "FROM EsigFirstUseCertification c ORDER BY c.certifiedAt DESC";
            Query<EsigFirstUseCertification> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EsigFirstUseCertification.class);
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getAllCertifications()", e);
        }
    }
}
