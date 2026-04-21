package org.openelisglobal.esig.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;

/**
 * Service interface for electronic signatures per 21 CFR Part 11.
 *
 * <p>
 * Handles:
 * <ul>
 * <li>Signature execution (with credential verification)</li>
 * <li>First-use certification management</li>
 * <li>Signing session tracking</li>
 * <li>Signature queries</li>
 * </ul>
 */
public interface ElectronicSignatureService extends BaseObjectService<ElectronicSignature, Long> {

    // ========================
    // Signature Execution
    // ========================

    /**
     * Execute an electronic signature after verifying credentials.
     *
     * @param username        username of user attempting to sign
     * @param password        user's password for verification
     * @param meaning         signature meaning (AUTHORED, VALIDATED_AND_RELEASED,
     *                        REJECTED)
     * @param recordType      type of record being signed (e.g., "RESULT")
     * @param recordId        ID of the record being signed
     * @param rejectionReason required if meaning is REJECTED, otherwise null
     * @param clientIp        client IP address
     * @param userAgent       browser user agent
     * @return the created signature record
     * @throws IllegalArgumentException if user not certified or credentials invalid
     * @throws IllegalStateException    if e-signatures are disabled
     */
    ElectronicSignature executeSignature(String username, String password, SignatureMeaning meaning, String recordType,
            Long recordId, String rejectionReason, String clientIp, String userAgent);

    // ========================
    // Signature Queries
    // ========================

    /**
     * Get all signatures for a specific record.
     *
     * @param recordType type of record
     * @param recordId   ID of the record
     * @return list of signatures ordered chronologically
     */
    List<ElectronicSignature> getSignaturesForRecord(String recordType, Long recordId);

    /**
     * Get all signatures by a specific user.
     *
     * @param userId user ID
     * @return list of signatures ordered by most recent first
     */
    List<ElectronicSignature> getSignaturesByUser(Long userId);

    /**
     * Get all signatures with a specific meaning (e.g., all rejections).
     *
     * @param meaning signature meaning to filter by
     * @return list of signatures ordered by most recent first
     */
    List<ElectronicSignature> getSignaturesByMeaning(SignatureMeaning meaning);

    // ========================
    // First-Use Certification
    // ========================

    /**
     * Check if a user has completed first-use certification.
     *
     * @param username username
     * @return true if certified
     */
    boolean isUserCertified(String username);

    /**
     * Complete first-use certification for a user.
     *
     * @param username          username
     * @param password          user's password for verification
     * @param certificationText the legal text being acknowledged
     * @param clientIp          client IP address
     * @param userAgent         browser user agent
     * @return the created certification record
     * @throws IllegalArgumentException if credentials invalid or already certified
     */
    EsigFirstUseCertification certifyUser(String username, String password, String certificationText, String clientIp,
            String userAgent);

    /**
     * Revoke a user's certification (admin action). User will need to re-certify
     * before signing.
     *
     * @param username username whose certification to revoke
     */
    void revokeCertification(String username);

    /**
     * Get all certifications (for admin view).
     *
     * @return list of all certifications
     */
    List<EsigFirstUseCertification> getAllCertifications();

    // ========================
    // Signing Session
    // ========================

    /**
     * Check if the current user has an active signing session.
     *
     * @param username username
     * @return true if session is active (user has signed at least once)
     */
    boolean hasActiveSigningSession(String username);

    /**
     * Get the number of signatures executed in the current session.
     *
     * @param username username
     * @return count of signatures in session, or 0 if no active session
     */
    int getSessionSigningCount(String username);

    /**
     * Clear the signing session for a user (called on logout/timeout).
     *
     * @param username username
     */
    void clearSigningSession(String username);

    // ========================
    // Feature Toggle
    // ========================

    /**
     * Check if electronic signatures are enabled for this site.
     *
     * @return true if e-signatures are enabled
     */
    boolean isEsigEnabled();
}
