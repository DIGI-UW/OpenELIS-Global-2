package org.openelisglobal.esig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.esig.service.ElectronicSignatureService;
import org.openelisglobal.esig.valueholder.AuthMethod;
import org.openelisglobal.esig.valueholder.ElectronicSignature;
import org.openelisglobal.esig.valueholder.EsigFirstUseCertification;
import org.openelisglobal.esig.valueholder.SignatureMeaning;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for ElectronicSignatureService.
 *
 * Tests verify 21 CFR Part 11 compliance requirements: - First-use
 * certification (§11.100(c)) - Signature execution with credential verification
 * - Session-based signing (§11.200(a)(1)(i)) - Signature manifestation (§11.50)
 */
public class ElectronicSignatureServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ElectronicSignatureService electronicSignatureService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private LoginUserService loginUserService;

    private SystemUser testUser;
    private String testUsername;
    private static final String TEST_PASSWORD = "testPassword123!";
    private static final String TEST_CERTIFICATION_TEXT = "I certify that my electronic signature is the legally binding equivalent of my handwritten signature.";

    @Before
    public void setUp() {
        // Enable e-signatures for testing
        enableEsig(true);

        // Setup test user with login credentials
        testUser = createOrGetTestUser("esig_test_user", "Test", "User");
        testUsername = testUser.getLoginName();
        createOrGetTestLoginUser(testUsername, TEST_PASSWORD);

        // Clear any existing signing sessions
        electronicSignatureService.clearSigningSession(testUsername);

        // Ensure user is not certified at start of each test
        electronicSignatureService.revokeCertification(testUsername);
    }

    // ========================================================================
    // FIRST-USE CERTIFICATION TESTS (§11.100(c))
    // ========================================================================

    @Test
    public void testCertifyUser_Success() {
        // Arrange
        assertFalse("User should not be certified initially", electronicSignatureService.isUserCertified(testUsername));

        // Act
        EsigFirstUseCertification certification = electronicSignatureService.certifyUser(testUsername, TEST_PASSWORD,
                TEST_CERTIFICATION_TEXT, "192.168.1.100", "Mozilla/5.0 Test Browser");

        // Assert - Deep state verification
        assertNotNull("Certification should be created", certification);
        assertNotNull("Certification ID should be generated", certification.getId());
        assertEquals("User ID should match", Long.valueOf(testUser.getId()), certification.getUserId());
        assertEquals("Certification text should match", TEST_CERTIFICATION_TEXT, certification.getCertificationText());
        assertEquals("Client IP should match", "192.168.1.100", certification.getClientIp());
        assertEquals("User agent should match", "Mozilla/5.0 Test Browser", certification.getUserAgent());
        assertNotNull("Certified at timestamp should be set", certification.getCertifiedAt());

        // Verify timestamp is recent (within last minute)
        long timestampDiff = Instant.now().toEpochMilli() - certification.getCertifiedAt().getTime();
        assertTrue("Certification timestamp should be recent", timestampDiff < 60000);

        // Verify user is now certified
        assertTrue("User should be certified after certification",
                electronicSignatureService.isUserCertified(testUsername));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCertifyUser_AlreadyCertified_ThrowsException() {
        // Arrange - Certify user first
        electronicSignatureService.certifyUser(testUsername, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);

        assertTrue("User should be certified", electronicSignatureService.isUserCertified(testUsername));

        // Act - Try to certify again (should throw)
        electronicSignatureService.certifyUser(testUsername, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCertifyUser_InvalidPassword_ThrowsException() {
        // Act - Try to certify with wrong password (should throw)
        electronicSignatureService.certifyUser(testUsername, "wrongPassword", TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);
    }

    @Test
    public void testRevokeCertification_Success() {
        // Arrange - Certify user first
        electronicSignatureService.certifyUser(testUsername, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);
        assertTrue("User should be certified", electronicSignatureService.isUserCertified(testUsername));

        // Act
        electronicSignatureService.revokeCertification(testUsername);

        // Assert
        assertFalse("User should not be certified after revocation",
                electronicSignatureService.isUserCertified(testUsername));
    }

    @Test
    public void testRevokeCertification_NonExistent_NoError() {
        // Arrange - Ensure user is not certified
        assertFalse("User should not be certified", electronicSignatureService.isUserCertified(testUsername));

        // Act - Should not throw
        electronicSignatureService.revokeCertification(testUsername);

        // Assert - Still not certified
        assertFalse("User should still not be certified", electronicSignatureService.isUserCertified(testUsername));
    }

    @Test
    public void testGetAllCertifications_ReturnsList() {
        // Arrange - Create certifications for multiple users
        SystemUser user1 = createOrGetTestUser("esig_cert_user1", "Cert", "User1");
        SystemUser user2 = createOrGetTestUser("esig_cert_user2", "Cert", "User2");
        String user1Name = user1.getLoginName();
        String user2Name = user2.getLoginName();
        createOrGetTestLoginUser(user1Name, TEST_PASSWORD);
        createOrGetTestLoginUser(user2Name, TEST_PASSWORD);

        // Clean up any existing certifications
        electronicSignatureService.revokeCertification(user1Name);
        electronicSignatureService.revokeCertification(user2Name);

        electronicSignatureService.certifyUser(user1Name, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, null, null);
        electronicSignatureService.certifyUser(user2Name, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, null, null);

        // Act
        List<EsigFirstUseCertification> certifications = electronicSignatureService.getAllCertifications();

        // Assert
        assertNotNull("Should return a list", certifications);
        assertTrue("Should contain at least 2 certifications", certifications.size() >= 2);

        // Verify the certifications contain our test users (certifications store userId
        // as Long)
        Long user1Id = Long.valueOf(user1.getId());
        Long user2Id = Long.valueOf(user2.getId());
        boolean foundUser1 = certifications.stream().anyMatch(c -> c.getUserId().equals(user1Id));
        boolean foundUser2 = certifications.stream().anyMatch(c -> c.getUserId().equals(user2Id));
        assertTrue("Should contain user1's certification", foundUser1);
        assertTrue("Should contain user2's certification", foundUser2);
    }

    // ========================================================================
    // SIGNATURE EXECUTION TESTS
    // ========================================================================

    @Test
    public void testExecuteSignature_Authored_Success() {
        // Arrange - Certify user first
        certifyTestUser();

        // Act
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100",
                "Mozilla/5.0 Test Browser");

        // Assert - Deep state verification per §11.50
        assertNotNull("Signature should be created", signature);
        assertNotNull("Signature ID should be generated", signature.getId());

        // Signer identification (signerId is the numeric user ID stored in DB)
        assertEquals("Signer ID should match", Long.valueOf(testUser.getId()), signature.getSignerId());
        assertEquals("Printed name should match", "Test User", signature.getSignerNamePrinted());

        // Signature meaning
        assertEquals("Signature meaning should be AUTHORED", SignatureMeaning.AUTHORED,
                signature.getSignatureMeaning());

        // Timestamp
        assertNotNull("Signed at timestamp should be set", signature.getSignedAt());
        long timestampDiff = Instant.now().toEpochMilli() - signature.getSignedAt().getTime();
        assertTrue("Signature timestamp should be recent", timestampDiff < 60000);

        // Record reference
        assertEquals("Record type should match", "RESULT", signature.getRecordType());
        assertEquals("Record ID should match", Long.valueOf(123L), signature.getRecordId());

        // Rejection reason should be null for AUTHORED
        assertNull("Rejection reason should be null for AUTHORED", signature.getRejectionReason());

        // Session tracking
        assertEquals("Session sequence should be 1 for first signature", Integer.valueOf(1),
                Integer.valueOf(signature.getSessionSigningSequence()));

        // Authentication method
        assertEquals("Auth method should be LOCAL", AuthMethod.LOCAL, signature.getAuthMethod());

        // Client metadata
        assertEquals("Client IP should match", "192.168.1.100", signature.getClientIp());
        assertEquals("User agent should match", "Mozilla/5.0 Test Browser", signature.getUserAgent());
    }

    @Test
    public void testExecuteSignature_ValidatedAndReleased_Success() {
        // Arrange
        certifyTestUser();

        // Act
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(456L), null, "192.168.1.100", null);

        // Assert
        assertNotNull("Signature should be created", signature);
        assertEquals("Signature meaning should be VALIDATED_AND_RELEASED", SignatureMeaning.VALIDATED_AND_RELEASED,
                signature.getSignatureMeaning());
        assertEquals("Record ID should match", Long.valueOf(456L), signature.getRecordId());
        assertNull("Rejection reason should be null", signature.getRejectionReason());
    }

    @Test
    public void testExecuteSignature_Rejected_WithReason_Success() {
        // Arrange
        certifyTestUser();
        String rejectionReason = "Sample contaminated - requires recollection";

        // Act
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.REJECTED, "RESULT", Long.valueOf(789L), rejectionReason, "192.168.1.100", null);

        // Assert
        assertNotNull("Signature should be created", signature);
        assertEquals("Signature meaning should be REJECTED", SignatureMeaning.REJECTED,
                signature.getSignatureMeaning());
        assertEquals("Rejection reason should match", rejectionReason, signature.getRejectionReason());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteSignature_Rejected_WithoutReason_ThrowsException() {
        // Arrange
        certifyTestUser();

        // Act - Try to reject without reason (should throw)
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(789L), null, "192.168.1.100", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteSignature_Rejected_WithEmptyReason_ThrowsException() {
        // Arrange
        certifyTestUser();

        // Act - Try to reject with empty reason (should throw)
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(789L), "   ", "192.168.1.100", null);
    }

    @Test(expected = IllegalStateException.class)
    public void testExecuteSignature_EsigDisabled_ThrowsException() {
        // Arrange
        certifyTestUser();
        enableEsig(false);

        // Act - Try to sign when e-signatures disabled (should throw)
        try {
            electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);
        } finally {
            // Re-enable for cleanup
            enableEsig(true);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteSignature_NotCertified_ThrowsException() {
        // Arrange - User is NOT certified (setUp revokes certification)
        assertFalse("User should not be certified", electronicSignatureService.isUserCertified(testUsername));

        // Act - Try to sign without certification (should throw)
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(123L), null, "192.168.1.100", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecuteSignature_InvalidPassword_ThrowsException() {
        // Arrange
        certifyTestUser();

        // Act - Try to sign with wrong password (should throw)
        electronicSignatureService.executeSignature(testUsername, "wrongPassword", SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(123L), null, "192.168.1.100", null);
    }

    @Test
    public void testExecuteSignature_TruncatesLongUserAgent() {
        // Arrange
        certifyTestUser();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append("X");
        }
        String longUserAgent = sb.toString();

        // Act
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100", longUserAgent);

        // Assert - User agent should be truncated to 500 characters
        assertNotNull("Signature should be created", signature);
        assertNotNull("User agent should be set", signature.getUserAgent());
        assertEquals("User agent should be truncated to 500 chars", 500, signature.getUserAgent().length());
    }

    // ========================================================================
    // SESSION-BASED SIGNING TESTS (§11.200(a)(1)(i))
    // ========================================================================

    @Test
    public void testSessionSigning_SequenceIncrementsCorrectly() {
        // Arrange
        certifyTestUser();

        // Act - Sign multiple records in the same session
        ElectronicSignature sig1 = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(1L), null, null, null);

        ElectronicSignature sig2 = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(2L), null, null, null);

        ElectronicSignature sig3 = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(3L), null, null, null);

        // Assert - Session sequence should increment
        assertEquals("First signature sequence should be 1", Integer.valueOf(1),
                Integer.valueOf(sig1.getSessionSigningSequence()));
        assertEquals("Second signature sequence should be 2", Integer.valueOf(2),
                Integer.valueOf(sig2.getSessionSigningSequence()));
        assertEquals("Third signature sequence should be 3", Integer.valueOf(3),
                Integer.valueOf(sig3.getSessionSigningSequence()));
    }

    @Test
    public void testHasActiveSigningSession_TrueAfterSigning() {
        // Arrange
        certifyTestUser();
        assertFalse("Should not have active session before signing",
                electronicSignatureService.hasActiveSigningSession(testUsername));

        // Act
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);

        // Assert
        assertTrue("Should have active session after signing",
                electronicSignatureService.hasActiveSigningSession(testUsername));
    }

    @Test
    public void testGetSessionSigningCount_ReturnsCorrectCount() {
        // Arrange
        certifyTestUser();
        assertEquals("Should have 0 count before signing", 0,
                electronicSignatureService.getSessionSigningCount(testUsername));

        // Act - Sign 3 times
        for (int i = 1; i <= 3; i++) {
            electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(i), null, null, null);
        }

        // Assert
        assertEquals("Should have 3 signatures in session", 3,
                electronicSignatureService.getSessionSigningCount(testUsername));
    }

    @Test
    public void testClearSigningSession_ResetsSession() {
        // Arrange
        certifyTestUser();
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);
        assertTrue("Should have active session", electronicSignatureService.hasActiveSigningSession(testUsername));

        // Act
        electronicSignatureService.clearSigningSession(testUsername);

        // Assert
        assertFalse("Should not have active session after clearing",
                electronicSignatureService.hasActiveSigningSession(testUsername));
        assertEquals("Signing count should be 0 after clearing", 0,
                electronicSignatureService.getSessionSigningCount(testUsername));
    }

    @Test
    public void testClearSigningSession_NewSessionStartsAtOne() {
        // Arrange - Create session and clear it
        certifyTestUser();
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(2L), null, null, null);
        electronicSignatureService.clearSigningSession(testUsername);

        // Act - Sign again (new session)
        ElectronicSignature newSig = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(3L), null, null, null);

        // Assert - New session should start at 1
        assertEquals("New session should start at sequence 1", Integer.valueOf(1),
                Integer.valueOf(newSig.getSessionSigningSequence()));
    }

    // ========================================================================
    // SIGNATURE QUERY TESTS
    // ========================================================================

    @Test
    public void testGetSignaturesForRecord_ReturnsCorrectSignatures() {
        // Arrange
        certifyTestUser();
        Long targetRecordId = Long.valueOf(System.currentTimeMillis());

        // Sign target record twice
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                targetRecordId, null, null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", targetRecordId, null, null, null);

        // Sign different record
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(targetRecordId + 1), null, null, null);

        // Act
        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesForRecord("RESULT",
                targetRecordId);

        // Assert
        assertEquals("Should find exactly 2 signatures for target record", 2, signatures.size());
        assertTrue("All signatures should be for target record",
                signatures.stream().allMatch(s -> s.getRecordId().equals(targetRecordId)));
        assertTrue("All signatures should be for RESULT type",
                signatures.stream().allMatch(s -> "RESULT".equals(s.getRecordType())));

        // Verify we have both AUTHORED and VALIDATED_AND_RELEASED
        assertTrue("Should have AUTHORED signature",
                signatures.stream().anyMatch(s -> s.getSignatureMeaning() == SignatureMeaning.AUTHORED));
        assertTrue("Should have VALIDATED_AND_RELEASED signature",
                signatures.stream().anyMatch(s -> s.getSignatureMeaning() == SignatureMeaning.VALIDATED_AND_RELEASED));
    }

    @Test
    public void testGetSignaturesForRecord_EmptyForNonExistent() {
        // Act
        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesForRecord("RESULT",
                Long.valueOf(999999999L));

        // Assert
        assertNotNull("Should return empty list, not null", signatures);
        assertTrue("Should be empty for non-existent record", signatures.isEmpty());
    }

    @Test
    public void testGetSignaturesByUser_ReturnsCorrectSignatures() {
        // Arrange
        certifyTestUser();
        Long recordBase = Long.valueOf(System.currentTimeMillis());

        // Create 3 signatures for test user
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                recordBase, null, null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(recordBase + 1), null, null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(recordBase + 2), null, null, null);

        // Act
        List<ElectronicSignature> signatures = electronicSignatureService
                .getSignaturesByUser(Long.valueOf(testUser.getId()));

        // Assert
        assertTrue("Should find at least 3 signatures", signatures.size() >= 3);
        assertTrue("All signatures should be by test user",
                signatures.stream().allMatch(s -> s.getSignerId().equals(Long.valueOf(testUser.getId()))));
    }

    @Test
    public void testGetSignaturesByMeaning_ReturnsCorrectSignatures() {
        // Arrange
        certifyTestUser();
        Long recordBase = Long.valueOf(System.currentTimeMillis());

        // Create signatures with different meanings
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                recordBase, null, null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(recordBase + 1), "Quality issue", null, null);
        electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(recordBase + 2), "Sample damaged", null, null);

        // Act
        List<ElectronicSignature> rejections = electronicSignatureService
                .getSignaturesByMeaning(SignatureMeaning.REJECTED);

        // Assert
        assertTrue("Should find at least 2 rejected signatures", rejections.size() >= 2);
        assertTrue("All signatures should be REJECTED",
                rejections.stream().allMatch(s -> s.getSignatureMeaning() == SignatureMeaning.REJECTED));
        assertTrue("All REJECTED signatures should have rejection reason",
                rejections.stream().allMatch(s -> s.getRejectionReason() != null && !s.getRejectionReason().isEmpty()));
    }

    // ========================================================================
    // FEATURE TOGGLE TESTS
    // ========================================================================

    @Test
    public void testIsEsigEnabled_ReturnsTrueWhenEnabled() {
        // Arrange
        enableEsig(true);

        // Act & Assert
        assertTrue("Should return true when enabled", electronicSignatureService.isEsigEnabled());
    }

    @Test
    public void testIsEsigEnabled_ReturnsFalseWhenDisabled() {
        // Arrange
        enableEsig(false);

        // Act & Assert
        assertFalse("Should return false when disabled", electronicSignatureService.isEsigEnabled());

        // Cleanup
        enableEsig(true);
    }

    // ========================================================================
    // SIGNATURE MANIFESTATION TESTS (§11.50)
    // ========================================================================

    @Test
    public void testSignatureManifestation_ContainsRequiredFields() {
        // Arrange - Certify and sign
        certifyTestUser();
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);

        // Assert - Per §11.50, signature manifestation must include:
        // 1. Printed name of the signer
        assertNotNull("Printed name is required per §11.50", signature.getSignerNamePrinted());
        assertFalse("Printed name should not be empty", signature.getSignerNamePrinted().isEmpty());

        // 2. Date and time of signing
        assertNotNull("Date/time is required per §11.50", signature.getSignedAt());

        // 3. Meaning of the signature (e.g., review, approval, responsibility)
        assertNotNull("Signature meaning is required per §11.50", signature.getSignatureMeaning());

        // Verify full manifestation data is persisted and retrievable
        ElectronicSignature retrieved = electronicSignatureService.get(signature.getId());
        assertEquals("Retrieved printed name should match", signature.getSignerNamePrinted(),
                retrieved.getSignerNamePrinted());
        assertEquals("Retrieved timestamp should match", signature.getSignedAt(), retrieved.getSignedAt());
        assertEquals("Retrieved meaning should match", signature.getSignatureMeaning(),
                retrieved.getSignatureMeaning());
    }

    @Test
    public void testSignerNamePrinted_FormattedCorrectly() {
        // Arrange - User has both first and last name
        certifyTestUser();

        // Act
        ElectronicSignature signature = electronicSignatureService.executeSignature(testUsername, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, null, null);

        // Assert - Name should be "FirstName LastName"
        assertEquals("Signer name should be 'Test User'", "Test User", signature.getSignerNamePrinted());
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void certifyTestUser() {
        if (!electronicSignatureService.isUserCertified(testUsername)) {
            electronicSignatureService.certifyUser(testUsername, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, null, null);
        }
    }

    private void enableEsig(boolean enabled) {
        ConfigurationProperties.getInstance().setPropertyValue(
                ConfigurationProperties.Property.ELECTRONIC_SIGNATURE_ENABLED, enabled ? "true" : "false");
    }

    private SystemUser createOrGetTestUser(String loginName, String firstName, String lastName) {
        // Try to find existing user
        List<SystemUser> users = systemUserService.getAll();
        for (SystemUser user : users) {
            if (loginName.equals(user.getLoginName())) {
                return user;
            }
        }

        // Create new user
        SystemUser user = new SystemUser();
        user.setLoginName(loginName);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setIsActive("Y");
        user.setIsEmployee("Y");
        user.setSysUserId("1");
        return systemUserService.save(user);
    }

    private LoginUser createOrGetTestLoginUser(String loginName, String password) {
        // Try to find existing login user
        try {
            LoginUser existing = loginUserService.getUserProfile(loginName);
            if (existing != null) {
                return existing;
            }
        } catch (Exception e) {
            // User doesn't exist, will create
        }

        // Create new login user
        LoginUser loginUser = new LoginUser();
        loginUser.setLoginName(loginName);
        // Hash the password before storing
        loginUserService.hashPassword(loginUser, password);
        loginUser.setAccountDisabled("N");
        loginUser.setAccountLocked("N");
        loginUser.setIsAdmin("N");
        loginUser.setUserTimeOut("30"); // 30 minute timeout
        loginUser.setPasswordExpiredDate(Date.valueOf(LocalDate.of(2099, 12, 31)));
        loginUser.setSysUserId("1");
        return loginUserService.save(loginUser);
    }
}
