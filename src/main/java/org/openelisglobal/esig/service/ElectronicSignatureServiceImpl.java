package org.openelisglobal.esig.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    @Override
    @Transactional
    public ElectronicSignature executeSignature(String username, String password, SignatureMeaning meaning,
            String recordType, Long recordId, String rejectionReason, String clientIp, String userAgent) {

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

        // 7. Determine session signing sequence
        int sessionSequence = getOrCreateSigningSession(username);

        // 7. Create signature record
        ElectronicSignature signature = new ElectronicSignature();
        signature.setSignerId(userId);
        signature.setSignerNamePrinted(signerName);
        signature.setSignatureMeaning(meaning);
        signature.setSignedAt(Timestamp.from(Instant.now()));
        signature.setRecordType(recordType);
        signature.setRecordId(recordId);
        signature.setRejectionReason(rejectionReason);
        signature.setSessionSigningSequence(sessionSequence);
        signature.setAuthMethod(authMethod);
        signature.setClientIp(clientIp);
        signature.setUserAgent(truncateUserAgent(userAgent));
        signature.setSysUserId(userId.toString());

        // 8. Save and return
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

        // 3. Verify credentials
        verifyCredentials(username, password);

        // 4. Create certification record
        EsigFirstUseCertification certification = new EsigFirstUseCertification();
        certification.setUserId(userId);
        certification.setCertifiedAt(Timestamp.from(Instant.now()));
        certification.setCertificationText(certificationText);
        certification.setClientIp(clientIp);
        certification.setUserAgent(truncateUserAgent(userAgent));
        certification.setSysUserId(userId.toString());

        // 5. Save and return
        Long id = electronicSignatureDAO.insertCertification(certification);
        return electronicSignatureDAO.getCertificationByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeCertification(String username) {
        Long userId = getUserIdByUsername(username);
        if (userId == null) {
            return;
        }
        EsigFirstUseCertification certification = electronicSignatureDAO.getCertificationByUserId(userId);
        if (certification != null) {
            electronicSignatureDAO.deleteCertification(certification);
        }
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
        return session != null && !session.isExpired();
    }

    @Override
    public int getSessionSigningCount(String username) {
        SigningSessionInfo session = activeSessions.get(username);
        if (session != null && !session.isExpired()) {
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
        // Check site configuration for e-signature feature toggle
        // Default to false if not configured
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
     * Get or create a signing session, returning the sequence number.
     */
    private int getOrCreateSigningSession(String username) {
        SigningSessionInfo session = activeSessions.compute(username, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                // New session
                return new SigningSessionInfo(username);
            } else {
                // Existing session - increment count
                existing.incrementSigningCount();
                return existing;
            }
        });
        return session.getSigningCount();
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
     * Tracks signing session state for a user.
     */
    private static class SigningSessionInfo {
        private final Instant startedAt;
        private int signingCount;
        private String username;

        // Session expires after 8 hours of inactivity (aligns with typical session
        // timeout)
        private static final long SESSION_TIMEOUT_HOURS = 8;

        public SigningSessionInfo(String username) {
            this.startedAt = Instant.now();
            this.signingCount = 1;
            this.username = username;
        }

        public int getSigningCount() {
            return signingCount;
        }

        public String getUsername() {
            return username;
        }

        public void incrementSigningCount() {
            this.signingCount++;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(startedAt.plusSeconds(SESSION_TIMEOUT_HOURS * 3600));
        }
    }
}
