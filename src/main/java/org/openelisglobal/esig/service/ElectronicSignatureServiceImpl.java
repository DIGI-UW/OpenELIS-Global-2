package org.openelisglobal.esig.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.esig.dao.ElectronicSignatureDAO;
import org.openelisglobal.esig.valueholder.AuthMethod;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Service implementation for electronic signatures per 21 CFR Part 11.
 */
@Service
public class ElectronicSignatureServiceImpl extends AuditableBaseObjectServiceImpl<ElectronicSignature, Long>
        implements ElectronicSignatureService {

    @Autowired
    private ElectronicSignatureDAO electronicSignatureDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private CredentialVerificationService credentialVerificationService;

    /**
     * In-memory session tracking. Key: username, Value: session signing info. Note:
     * For distributed deployments, this should be replaced with Redis or similar.
     */
    private final Map<String, SigningSessionInfo> activeSessions = new ConcurrentHashMap<>();

    public ElectronicSignatureServiceImpl() {
        super(ElectronicSignature.class);
        this.auditTrailLog = true; // Enable audit trail for signatures
    }

    @Override
    protected ElectronicSignatureDAO getBaseObjectDAO() {
        return electronicSignatureDAO;
    }

    // ========================
    // Signature Execution
    // ========================

    private static final java.util.Set<String> VALID_RECORD_TYPES = java.util.Set.of("RESULT", "RESULT_BATCH",
            "ANALYSIS", "VALIDATION_BATCH", "QC_RESULT", "REPORT");

    @Override
    @Transactional
    public ElectronicSignature executeSignature(String username, String password, SignatureMeaning meaning,
            String recordType, Long recordId, String rejectionReason, String clientIp, String userAgent) {

        // 0. Validate required parameters
        if (meaning == null) {
            throw new IllegalArgumentException("Signature meaning is required");
        }
        if (recordId == null) {
            throw new IllegalArgumentException("Record ID is required");
        }
        if (recordType == null || !VALID_RECORD_TYPES.contains(recordType)) {
            throw new IllegalArgumentException("Invalid record type: must be one of " + VALID_RECORD_TYPES);
        }

        // 1. Check if e-signatures are enabled
        if (!isEsigEnabled()) {
            throw new IllegalStateException("Electronic signatures are not enabled for this site");
        }

        // 2. Look up user by username
        SystemUser user = systemUserService.getDataForLoginUser(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        Long userId = Long.parseLong(user.getId());

        // 3. Check if user is certified
        if (!isUserCertified(username)) {
            throw new IllegalArgumentException("User must complete first-use certification before signing");
        }

        // 4. Verify credentials
        AuthMethod authMethod = verifyCredentials(username, password);

        // 5. Validate rejection reason if meaning is REJECTED
        if (meaning == SignatureMeaning.REJECTED) {
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting");
            }
        }

        // 6. Get user's full name for signature manifestation
        String signerName = buildSignerName(user);

        // 7. Atomically advance the session counter. If the DB insert rolls back,
        // the TransactionSynchronization callback will decrement it.
        int sessionSequence = advanceSessionSequence(username);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    rollbackSessionSequence(username);
                }
            }
        });

        // 8. Create signature record
        ElectronicSignature signature = new ElectronicSignature();
        signature.setSignerId(userId);
        signature.setSignerNamePrinted(signerName);
        signature.setSignatureMeaning(meaning);
        signature.setSignedAt(Timestamp.from(Instant.now().truncatedTo(ChronoUnit.MICROS)));
        signature.setRecordType(recordType);
        signature.setRecordId(recordId);
        signature.setRejectionReason(rejectionReason);
        signature.setSessionSigningSequence(sessionSequence);
        signature.setAuthMethod(authMethod);
        signature.setClientIp(clientIp);
        signature.setUserAgent(truncateUserAgent(userAgent));
        signature.setSysUserId(userId.toString());

        // 9. Save
        Long id = insert(signature);
        return get(id);
    }

    // ========================
    // Signature Queries
    // ========================

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesForRecord(String recordType, Long recordId) {
        return electronicSignatureDAO.getSignaturesByRecord(recordType, recordId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesByUser(Long userId) {
        return electronicSignatureDAO.getSignaturesBySigner(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ElectronicSignature> getSignaturesByMeaning(SignatureMeaning meaning) {
        return electronicSignatureDAO.getSignaturesByMeaning(meaning);
    }

    // ========================
    // First-Use Certification
    // ========================

    @Override
    @Transactional(readOnly = true)
    public boolean isUserCertified(String username) {
        Long userId = getUserIdByUsername(username);
        if (userId == null) {
            return false;
        }
        return electronicSignatureDAO.isUserCertified(userId);
    }

    @Override
    @Transactional
    public EsigFirstUseCertification certifyUser(String username, String password, String certificationText,
            String clientIp, String userAgent) {

        // 1. Look up user by username
        SystemUser user = systemUserService.getDataForLoginUser(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        Long userId = Long.parseLong(user.getId());

        // 2. Check if already certified
        if (electronicSignatureDAO.isUserCertified(userId)) {
            throw new IllegalArgumentException("User is already certified");
        }

        // 3. Validate certification text is non-empty
        if (certificationText == null || certificationText.trim().isEmpty()) {
            throw new IllegalArgumentException("Certification text is required");
        }

        // 4. Verify credentials
        verifyCredentials(username, password);

        // 5. Create certification record
        EsigFirstUseCertification certification = new EsigFirstUseCertification();
        certification.setUserId(userId);
        certification.setCertifiedAt(Timestamp.from(Instant.now()));
        certification.setCertificationText(certificationText);
        certification.setClientIp(clientIp);
        certification.setUserAgent(truncateUserAgent(userAgent));
        certification.setSysUserId(userId.toString());

        // 5. Save and return (entity is already managed after persist+flush)
        electronicSignatureDAO.insertCertification(certification);
        return certification;
    }

    @Override
    @Transactional
    public void revokeCertification(String username) {
        Long userId = getUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        EsigFirstUseCertification certification = electronicSignatureDAO.getCertificationByUserId(userId);
        if (certification == null) {
            return; // Already not certified — idempotent
        }
        electronicSignatureDAO.deleteCertification(certification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EsigFirstUseCertification> getAllCertifications() {
        return electronicSignatureDAO.getAllCertifications();
    }

    // ========================
    // Signing Session
    // ========================

    @Override
    public boolean hasActiveSigningSession(String username) {
        SigningSessionInfo session = activeSessions.get(username);
        return session != null && !session.isExpired(getSessionTimeoutMinutes());
    }

    @Override
    public int getSessionSigningCount(String username) {
        SigningSessionInfo session = activeSessions.get(username);
        if (session != null && !session.isExpired(getSessionTimeoutMinutes())) {
            return session.getSigningCount();
        }
        return 0;
    }

    @Override
    public void clearSigningSession(String username) {
        activeSessions.remove(username);
    }

    // ========================
    // Feature Toggle
    // ========================

    @Override
    public boolean isEsigEnabled() {
        String enabled = ConfigurationProperties.getInstance().getPropertyValue(Property.ELECTRONIC_SIGNATURE_ENABLED);
        return "true".equalsIgnoreCase(enabled);
    }

    // ========================
    // Private Helper Methods
    // ========================

    /**
     * Verify user credentials against the authentication provider.
     *
     * @return the authentication method used
     * @throws IllegalArgumentException if credentials are invalid
     */
    private AuthMethod verifyCredentials(String username, String password) {
        return credentialVerificationService.verifyCredentialsByLoginName(username, password);
    }

    /**
     * Look up user ID by username.
     *
     * @return user ID or null if not found
     */
    private Long getUserIdByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        SystemUser user = systemUserService.getDataForLoginUser(username);
        if (user == null) {
            return null;
        }
        return Long.parseLong(user.getId());
    }

    /**
     * Build the printed name for signature manifestation.
     */
    private String buildSignerName(SystemUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (lastName != null) {
            return lastName;
        } else if (firstName != null) {
            return firstName;
        } else {
            return user.getLoginName();
        }
    }

    /**
     * Atomically advance the session counter and return the new sequence number.
     * Uses ConcurrentHashMap.compute() so concurrent calls for the same username
     * are serialized — no two threads can get the same sequence number.
     */
    private int advanceSessionSequence(String username) {
        SigningSessionInfo session = activeSessions.compute(username, (key, existing) -> {
            if (existing == null || existing.isExpired(getSessionTimeoutMinutes())) {
                return new SigningSessionInfo();
            } else {
                existing.incrementSigningCount();
                return existing;
            }
        });
        return session.getSigningCount();
    }

    /**
     * Roll back the session counter on transaction failure so the sequence stays
     * accurate.
     */
    private void rollbackSessionSequence(String username) {
        activeSessions.computeIfPresent(username, (key, existing) -> {
            int count = existing.decrementSigningCount();
            return count <= 0 ? null : existing;
        });
    }

    /**
     * Truncate user agent to fit database column.
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent;
    }

    // ========================
    // Inner Classes
    // ========================

    /**
     * Get the configured session timeout in minutes. Defaults to 30 minutes per
     * industry standard for 21 CFR Part 11 compliance.
     */
    private long getSessionTimeoutMinutes() {
        String value = ConfigurationProperties.getInstance().getPropertyValue(Property.ESIG_SESSION_TIMEOUT_MINUTES);
        if (value != null && !value.isEmpty()) {
            try {
                long minutes = Long.parseLong(value);
                if (minutes > 0) {
                    return minutes;
                }
            } catch (NumberFormatException e) {
                LogEvent.logWarn(getClass().getSimpleName(), "getSessionTimeoutMinutes",
                        "Invalid ESIG_SESSION_TIMEOUT_MINUTES value: " + value + ". Using default "
                                + DEFAULT_SESSION_TIMEOUT_MINUTES + " minutes.");
            }
        }
        return DEFAULT_SESSION_TIMEOUT_MINUTES;
    }

    private static final long DEFAULT_SESSION_TIMEOUT_MINUTES = 30;

    /**
     * Tracks signing session state for a user.
     */
    private static class SigningSessionInfo {
        private volatile Instant lastActivityAt;
        private final AtomicInteger signingCount;

        public SigningSessionInfo() {
            this.lastActivityAt = Instant.now();
            this.signingCount = new AtomicInteger(1);
        }

        public int getSigningCount() {
            return signingCount.get();
        }

        public void incrementSigningCount() {
            this.signingCount.incrementAndGet();
            this.lastActivityAt = Instant.now();
        }

        public int decrementSigningCount() {
            return this.signingCount.decrementAndGet();
        }

        public boolean isExpired(long timeoutMinutes) {
            return Instant.now().isAfter(lastActivityAt.plusSeconds(timeoutMinutes * 60));
        }
    }
}
