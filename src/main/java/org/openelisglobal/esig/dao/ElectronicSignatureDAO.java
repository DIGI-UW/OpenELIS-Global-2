package org.openelisglobal.esig.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;

/**
 * Data access interface for electronic signatures and first-use certifications.
 */
public interface ElectronicSignatureDAO extends BaseDAO<ElectronicSignature, Long> {

    // ========================
    // Signature Queries
    // ========================

    /**
     * Get all signatures for a specific record.
     *
     * @param recordType type of record (e.g., "RESULT", "ANALYSIS")
     * @param recordId   primary key of the record
     * @return list of signatures ordered by signed_at ascending
     */
    List<ElectronicSignature> getSignaturesByRecord(String recordType, Long recordId) throws LIMSRuntimeException;

    /**
     * Get all signatures by a specific user.
     *
     * @param signerId user ID
     * @return list of signatures ordered by signed_at descending
     */
    List<ElectronicSignature> getSignaturesBySigner(Long signerId) throws LIMSRuntimeException;

    /**
     * Get signatures within a date range.
     *
     * @param startDate start of range (inclusive)
     * @param endDate   end of range (inclusive)
     * @return list of signatures ordered by signed_at descending
     */
    List<ElectronicSignature> getSignaturesInDateRange(Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException;

    /**
     * Get signatures by meaning (e.g., all rejections).
     *
     * @param meaning signature meaning
     * @return list of signatures ordered by signed_at descending
     */
    List<ElectronicSignature> getSignaturesByMeaning(SignatureMeaning meaning) throws LIMSRuntimeException;

    // ========================
    // First-Use Certification
    // ========================

    /**
     * Check if a user has completed first-use certification.
     *
     * @param userId user ID
     * @return true if user is certified
     */
    boolean isUserCertified(Long userId) throws LIMSRuntimeException;

    /**
     * Get certification record for a user.
     *
     * @param userId user ID
     * @return certification record, or null if not certified
     */
    EsigFirstUseCertification getCertificationByUserId(Long userId) throws LIMSRuntimeException;

    /**
     * Save a new first-use certification.
     *
     * @param certification certification record to save
     * @return generated ID
     */
    Long insertCertification(EsigFirstUseCertification certification) throws LIMSRuntimeException;

    /**
     * Delete a certification (admin action to force re-certification).
     *
     * @param certification certification record to delete
     */
    void deleteCertification(EsigFirstUseCertification certification) throws LIMSRuntimeException;

    /**
     * Get all certifications (for admin view).
     *
     * @return list of all certifications ordered by certified_at descending
     */
    List<EsigFirstUseCertification> getAllCertifications() throws LIMSRuntimeException;
}
