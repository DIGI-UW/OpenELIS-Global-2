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
     * Count signatures within a date range (QA Overview counter — avoids loading
     * rows).
     *
     * @param startDate start of range (inclusive)
     * @param endDate   end of range (inclusive)
     * @return number of signatures executed in the range
     */
    long countSignaturesInDateRange(Timestamp startDate, Timestamp endDate) throws LIMSRuntimeException;

    /**
     * Get signatures by meaning (e.g., all rejections).
     *
     * @param meaning signature meaning
     * @return list of signatures ordered by signed_at descending
     */
    List<ElectronicSignature> getSignaturesByMeaning(SignatureMeaning meaning) throws LIMSRuntimeException;

    /**
     * Search signatures with combined optional filters, paginated (E-Sig Log).
     *
     * @param startDate  start of range (inclusive, required)
     * @param endDate    end of range (inclusive, required)
     * @param signerId   optional signer filter
     * @param meaning    optional meaning filter
     * @param recordType optional record type filter
     * @param page       0-based page index
     * @param pageSize   rows per page
     * @return matching page of signatures ordered by signed_at descending
     */
    List<ElectronicSignature> searchSignatures(Timestamp startDate, Timestamp endDate, Long signerId,
            SignatureMeaning meaning, String recordType, int page, int pageSize) throws LIMSRuntimeException;

    /**
     * Count signatures matching the same filters as
     * {@link #searchSignatures(Timestamp, Timestamp, Long, SignatureMeaning, String, int, int)}.
     *
     * @return total matching rows across all pages
     */
    long countSearchSignatures(Timestamp startDate, Timestamp endDate, Long signerId, SignatureMeaning meaning,
            String recordType) throws LIMSRuntimeException;

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
