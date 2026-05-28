package org.openelisglobal.esig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
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
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for ElectronicSignatureService.
 *
 * Uses the pre-existing "admin" user (system_user id=1, login_user
 * login_name="admin") that ships with the base test database — no custom user
 * creation needed. This avoids ConstraintViolation issues with the login_user
 * table's PRIMARY KEY(login_name) when running in the full test suite.
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

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD = "adminADMIN!";
    private static final String TEST_CERTIFICATION_TEXT = "I certify that my electronic signature is the legally binding equivalent of my handwritten signature.";

    private SystemUser adminUser;

    @Before
    public void setUp() throws Exception {
        // Load admin login_user credentials (password: adminADMIN!) via direct JDBC.
        // Uses REFRESH so it's idempotent even if another test modified login_user.
        executeDataSetWithStateManagement("testdata/esig-test-data.xml");

        enableEsig(true);

        adminUser = systemUserService.get("1");
        assertNotNull("admin system_user (id=1) must exist in base test DB", adminUser);

        electronicSignatureService.clearSigningSession(TEST_USERNAME);
        electronicSignatureService.revokeCertification(TEST_USERNAME);
    }

    // ========================================================================
    // FIRST-USE CERTIFICATION TESTS (§11.100(c))
    // ========================================================================

    @Test
    public void testCertifyUser_Success() {
        assertFalse("User should not be certified initially",
                electronicSignatureService.isUserCertified(TEST_USERNAME));

        EsigFirstUseCertification certification = electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD,
                TEST_CERTIFICATION_TEXT, "192.168.1.100", "Mozilla/5.0 Test Browser");

        // Verify returned object
        assertNotNull("Certification should be created", certification);
        assertNotNull("Certification ID should be generated", certification.getId());
        assertEquals("User ID should match", Long.valueOf(adminUser.getId()), certification.getUserId());
        assertEquals("Certification text should match", TEST_CERTIFICATION_TEXT, certification.getCertificationText());
        assertEquals("Client IP should match", "192.168.1.100", certification.getClientIp());
        assertEquals("User agent should match", "Mozilla/5.0 Test Browser", certification.getUserAgent());
        assertNotNull("Certified at timestamp should be set", certification.getCertifiedAt());

        long timestampDiff = Instant.now().toEpochMilli() - certification.getCertifiedAt().getTime();
        assertTrue("Certification timestamp should be recent", timestampDiff < 60000);

        // Cross-verify: state change is queryable
        assertTrue("User should be certified after certification",
                electronicSignatureService.isUserCertified(TEST_USERNAME));
    }

    @Test
    public void testCertifyUser_AlreadyCertified_ThrowsException() {
        // Arrange — certify first, then verify state BEFORE the real test
        electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);
        assertTrue("Precondition: user must be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));

        // Act — the ONLY call that should throw
        try {
            electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD, TEST_CERTIFICATION_TEXT,
                    "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for double certification");
        } catch (IllegalArgumentException e) {
            // Expected — verify it's about certification, not credentials
            assertTrue("Error should mention certification", e.getMessage().toLowerCase().contains("certif"));
        }
    }

    @Test
    public void testCertifyUser_InvalidPassword_ThrowsException() {
        try {
            electronicSignatureService.certifyUser(TEST_USERNAME, "wrongPassword", TEST_CERTIFICATION_TEXT,
                    "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for wrong password");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // Cross-verify: user should NOT be certified after failed attempt
        assertFalse("Failed certification must not change state",
                electronicSignatureService.isUserCertified(TEST_USERNAME));
    }

    @Test
    public void testRevokeCertification_Success() {
        electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, "192.168.1.100",
                null);
        assertTrue("Precondition: user must be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));

        electronicSignatureService.revokeCertification(TEST_USERNAME);

        assertFalse("User should not be certified after revocation",
                electronicSignatureService.isUserCertified(TEST_USERNAME));
    }

    @Test
    public void testRevokeCertification_NonExistent_NoError() {
        assertFalse("Precondition: user should not be certified",
                electronicSignatureService.isUserCertified(TEST_USERNAME));

        // Should not throw
        electronicSignatureService.revokeCertification(TEST_USERNAME);

        assertFalse("User should still not be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));
    }

    @Test
    public void testGetAllCertifications_ReturnsList() {
        electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, null, null);

        List<EsigFirstUseCertification> certifications = electronicSignatureService.getAllCertifications();

        assertNotNull("Should return a list", certifications);

        // Find our SPECIFIC certification by admin user ID — not just "any"
        // certification
        Long adminId = Long.valueOf(adminUser.getId());
        EsigFirstUseCertification ours = certifications.stream().filter(c -> c.getUserId().equals(adminId)).findFirst()
                .orElse(null);
        assertNotNull("Should contain admin's certification", ours);
        assertEquals("Certification text should match", TEST_CERTIFICATION_TEXT, ours.getCertificationText());
    }

    // ========================================================================
    // SIGNATURE EXECUTION TESTS
    // ========================================================================

    @Test
    public void testExecuteSignature_Authored_Success() {
        certifyTestUser();

        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100",
                "Mozilla/5.0 Test Browser");

        // Verify returned object
        assertNotNull("Signature should be created", signature);
        assertNotNull("Signature ID should be generated", signature.getId());
        assertEquals("Signer ID should match", Long.valueOf(adminUser.getId()), signature.getSignerId());
        assertNotNull("Printed name should be set", signature.getSignerNamePrinted());
        assertFalse("Printed name should not be empty", signature.getSignerNamePrinted().isEmpty());
        assertEquals("Signature meaning should be AUTHORED", SignatureMeaning.AUTHORED,
                signature.getSignatureMeaning());
        assertNotNull("Signed at timestamp should be set", signature.getSignedAt());
        assertEquals("Record type should match", "RESULT", signature.getRecordType());
        assertEquals("Record ID should match", Long.valueOf(123L), signature.getRecordId());
        assertNull("Rejection reason should be null for AUTHORED", signature.getRejectionReason());
        assertEquals("Session sequence should be 1", Integer.valueOf(1),
                Integer.valueOf(signature.getSessionSigningSequence()));
        assertEquals("Auth method should be LOCAL", AuthMethod.LOCAL, signature.getAuthMethod());
        assertEquals("Client IP should match", "192.168.1.100", signature.getClientIp());
        assertEquals("User agent should match", "Mozilla/5.0 Test Browser", signature.getUserAgent());

        long timestampDiff = Instant.now().toEpochMilli() - signature.getSignedAt().getTime();
        assertTrue("Signature timestamp should be recent", timestampDiff < 60000);

        // Cross-verify: read back from DB
        ElectronicSignature persisted = electronicSignatureService.get(signature.getId());
        assertNotNull("Signature must be persisted", persisted);
        assertEquals("Persisted meaning must match", SignatureMeaning.AUTHORED, persisted.getSignatureMeaning());
        assertEquals("Persisted record ID must match", Long.valueOf(123L), persisted.getRecordId());
    }

    @Test
    public void testExecuteSignature_ValidatedAndReleased_Success() {
        certifyTestUser();

        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(456L), null, "192.168.1.100", null);

        assertNotNull("Signature should be created", signature);
        assertEquals("Signature meaning should be VALIDATED_AND_RELEASED", SignatureMeaning.VALIDATED_AND_RELEASED,
                signature.getSignatureMeaning());
        assertEquals("Record ID should match", Long.valueOf(456L), signature.getRecordId());
        assertNull("Rejection reason should be null", signature.getRejectionReason());

        // Cross-verify persistence
        ElectronicSignature persisted = electronicSignatureService.get(signature.getId());
        assertNotNull("Signature must be persisted", persisted);
        assertEquals("Persisted meaning must match", SignatureMeaning.VALIDATED_AND_RELEASED,
                persisted.getSignatureMeaning());
    }

    @Test
    public void testExecuteSignature_Rejected_WithReason_Success() {
        certifyTestUser();
        String rejectionReason = "Sample contaminated - requires recollection";

        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.REJECTED, "RESULT", Long.valueOf(789L), rejectionReason, "192.168.1.100", null);

        assertNotNull("Signature should be created", signature);
        assertEquals("Signature meaning should be REJECTED", SignatureMeaning.REJECTED,
                signature.getSignatureMeaning());
        assertEquals("Rejection reason should match", rejectionReason, signature.getRejectionReason());

        // Cross-verify: rejection reason persisted
        ElectronicSignature persisted = electronicSignatureService.get(signature.getId());
        assertEquals("Persisted rejection reason must match", rejectionReason, persisted.getRejectionReason());
    }

    @Test
    public void testExecuteSignature_Rejected_WithoutReason_ThrowsException() {
        certifyTestUser();
        assertTrue("Precondition: user must be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));

        try {
            electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.REJECTED,
                    "RESULT", Long.valueOf(789L), null, "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for REJECTED without reason");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testExecuteSignature_Rejected_WithEmptyReason_ThrowsException() {
        certifyTestUser();
        assertTrue("Precondition: user must be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));

        try {
            electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.REJECTED,
                    "RESULT", Long.valueOf(789L), "   ", "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for REJECTED with blank reason");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testExecuteSignature_EsigDisabled_ThrowsException() {
        certifyTestUser();
        enableEsig(false);

        try {
            electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);
            fail("Should have thrown IllegalStateException when e-sig disabled");
        } catch (IllegalStateException e) {
            // Expected
        } finally {
            enableEsig(true);
        }
    }

    @Test
    public void testExecuteSignature_NotCertified_ThrowsException() {
        assertFalse("Precondition: user should not be certified",
                electronicSignatureService.isUserCertified(TEST_USERNAME));

        try {
            electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for uncertified user");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testExecuteSignature_InvalidPassword_ThrowsException() {
        certifyTestUser();
        assertTrue("Precondition: user must be certified", electronicSignatureService.isUserCertified(TEST_USERNAME));

        try {
            electronicSignatureService.executeSignature(TEST_USERNAME, "wrongPassword", SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);
            fail("Should have thrown IllegalArgumentException for wrong password");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testExecuteSignature_TruncatesLongUserAgent() {
        certifyTestUser();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append("X");
        }
        String longUserAgent = sb.toString();

        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100", longUserAgent);

        assertNotNull("Signature should be created", signature);
        assertNotNull("User agent should be set", signature.getUserAgent());
        assertEquals("User agent should be truncated to 500 chars", 500, signature.getUserAgent().length());
    }

    // ========================================================================
    // SESSION-BASED SIGNING TESTS (§11.200(a)(1)(i))
    // ========================================================================

    @Test
    public void testSessionSigning_SequenceIncrementsCorrectly() {
        certifyTestUser();

        ElectronicSignature sig1 = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(1L), null, null, null);
        ElectronicSignature sig2 = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(2L), null, null, null);
        ElectronicSignature sig3 = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(3L), null, null, null);

        assertEquals("First signature sequence should be 1", 1L, (long) sig1.getSessionSigningSequence());
        assertEquals("Second signature sequence should be 2", 2L, (long) sig2.getSessionSigningSequence());
        assertEquals("Third signature sequence should be 3", 3L, (long) sig3.getSessionSigningSequence());
    }

    @Test
    public void testHasActiveSigningSession_TrueAfterSigning() {
        certifyTestUser();
        assertFalse("Should not have active session before signing",
                electronicSignatureService.hasActiveSigningSession(TEST_USERNAME));

        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);

        assertTrue("Should have active session after signing",
                electronicSignatureService.hasActiveSigningSession(TEST_USERNAME));
    }

    @Test
    public void testGetSessionSigningCount_ReturnsCorrectCount() {
        certifyTestUser();
        assertEquals("Should have 0 count before signing", 0,
                electronicSignatureService.getSessionSigningCount(TEST_USERNAME));

        for (int i = 1; i <= 3; i++) {
            electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED,
                    "RESULT", Long.valueOf(i), null, null, null);
        }

        assertEquals("Should have exactly 3 signatures in session", 3,
                electronicSignatureService.getSessionSigningCount(TEST_USERNAME));
    }

    @Test
    public void testClearSigningSession_ResetsSession() {
        certifyTestUser();
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);
        assertTrue("Precondition: should have active session",
                electronicSignatureService.hasActiveSigningSession(TEST_USERNAME));

        electronicSignatureService.clearSigningSession(TEST_USERNAME);

        assertFalse("Should not have active session after clearing",
                electronicSignatureService.hasActiveSigningSession(TEST_USERNAME));
        assertEquals("Signing count should be 0 after clearing", 0,
                electronicSignatureService.getSessionSigningCount(TEST_USERNAME));
    }

    @Test
    public void testClearSigningSession_NewSessionStartsAtOne() {
        certifyTestUser();
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(1L), null, null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(2L), null, null, null);
        electronicSignatureService.clearSigningSession(TEST_USERNAME);

        ElectronicSignature newSig = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(3L), null, null, null);

        assertEquals("New session should start at sequence 1", 1L, (long) newSig.getSessionSigningSequence());
    }

    // ========================================================================
    // SIGNATURE QUERY TESTS
    // ========================================================================

    @Test
    public void testGetSignaturesForRecord_ReturnsCorrectSignatures() {
        certifyTestUser();
        // Use unique record ID to avoid stale data from other tests
        Long targetRecordId = Long.valueOf(System.currentTimeMillis());

        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                targetRecordId, null, null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", targetRecordId, null, null, null);

        // Sign a DIFFERENT record to verify isolation
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(targetRecordId + 1), null, null, null);

        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesForRecord("RESULT",
                targetRecordId);

        // Exact count — not >=
        assertEquals("Should find exactly 2 signatures for target record", 2, signatures.size());
        assertTrue("All signatures should be for target record",
                signatures.stream().allMatch(s -> s.getRecordId().equals(targetRecordId)));
        assertTrue("All signatures should be for RESULT type",
                signatures.stream().allMatch(s -> "RESULT".equals(s.getRecordType())));
        assertTrue("Should have AUTHORED signature",
                signatures.stream().anyMatch(s -> s.getSignatureMeaning() == SignatureMeaning.AUTHORED));
        assertTrue("Should have VALIDATED_AND_RELEASED signature",
                signatures.stream().anyMatch(s -> s.getSignatureMeaning() == SignatureMeaning.VALIDATED_AND_RELEASED));
    }

    @Test
    public void testGetSignaturesForRecord_EmptyForNonExistent() {
        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesForRecord("RESULT",
                Long.valueOf(999999999L));

        assertNotNull("Should return empty list, not null", signatures);
        assertTrue("Should be empty for non-existent record", signatures.isEmpty());
    }

    @Test
    public void testGetSignaturesByUser_ReturnsCorrectSignatures() {
        certifyTestUser();
        // Use unique record IDs and count signatures BEFORE and AFTER
        Long recordBase = Long.valueOf(System.currentTimeMillis());
        Long adminId = Long.valueOf(adminUser.getId());

        int countBefore = electronicSignatureService.getSignaturesByUser(adminId).size();

        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                recordBase, null, null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                Long.valueOf(recordBase + 1), null, null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.VALIDATED_AND_RELEASED, "RESULT", Long.valueOf(recordBase + 2), null, null, null);

        List<ElectronicSignature> signatures = electronicSignatureService.getSignaturesByUser(adminId);

        // Exact delta — not "at least"
        assertEquals("Should have exactly 3 more signatures than before", countBefore + 3, signatures.size());
        assertTrue("All signatures should be by admin user",
                signatures.stream().allMatch(s -> s.getSignerId().equals(adminId)));
    }

    @Test
    public void testGetSignaturesByMeaning_ReturnsCorrectSignatures() {
        certifyTestUser();
        Long recordBase = Long.valueOf(System.currentTimeMillis());

        int rejectionsBefore = electronicSignatureService.getSignaturesByMeaning(SignatureMeaning.REJECTED).size();

        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.AUTHORED, "RESULT",
                recordBase, null, null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(recordBase + 1), "Quality issue", null, null);
        electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD, SignatureMeaning.REJECTED, "RESULT",
                Long.valueOf(recordBase + 2), "Sample damaged", null, null);

        List<ElectronicSignature> rejections = electronicSignatureService
                .getSignaturesByMeaning(SignatureMeaning.REJECTED);

        // Exact delta
        assertEquals("Should have exactly 2 more rejections", rejectionsBefore + 2, rejections.size());
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
        enableEsig(true);
        assertTrue("Should return true when enabled", electronicSignatureService.isEsigEnabled());
    }

    @Test
    public void testIsEsigEnabled_ReturnsFalseWhenDisabled() {
        enableEsig(false);
        assertFalse("Should return false when disabled", electronicSignatureService.isEsigEnabled());
        enableEsig(true);
    }

    // ========================================================================
    // SIGNATURE MANIFESTATION TESTS (§11.50)
    // ========================================================================

    @Test
    public void testSignatureManifestation_ContainsRequiredFields() {
        certifyTestUser();
        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, "192.168.1.100", null);

        // Per §11.50: printed name, date/time, meaning
        assertNotNull("Printed name is required per §11.50", signature.getSignerNamePrinted());
        assertFalse("Printed name should not be empty", signature.getSignerNamePrinted().isEmpty());
        assertNotNull("Date/time is required per §11.50", signature.getSignedAt());
        assertNotNull("Signature meaning is required per §11.50", signature.getSignatureMeaning());

        // Cross-verify: read back from DB to confirm persistence
        ElectronicSignature retrieved = electronicSignatureService.get(signature.getId());
        assertEquals("Retrieved printed name should match", signature.getSignerNamePrinted(),
                retrieved.getSignerNamePrinted());
        assertEquals("Retrieved timestamp should match", signature.getSignedAt(), retrieved.getSignedAt());
        assertEquals("Retrieved meaning should match", signature.getSignatureMeaning(),
                retrieved.getSignatureMeaning());
    }

    @Test
    public void testSignerNamePrinted_FormattedCorrectly() {
        certifyTestUser();

        ElectronicSignature signature = electronicSignatureService.executeSignature(TEST_USERNAME, TEST_PASSWORD,
                SignatureMeaning.AUTHORED, "RESULT", Long.valueOf(123L), null, null, null);

        String expectedName = adminUser.getFirstName() + " " + adminUser.getLastName();
        assertEquals("Signer name should match admin user's full name", expectedName, signature.getSignerNamePrinted());
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void certifyTestUser() {
        if (!electronicSignatureService.isUserCertified(TEST_USERNAME)) {
            electronicSignatureService.certifyUser(TEST_USERNAME, TEST_PASSWORD, TEST_CERTIFICATION_TEXT, null, null);
        }
    }

    private void enableEsig(boolean enabled) {
        ConfigurationProperties.getInstance().setPropertyValue(
                ConfigurationProperties.Property.ELECTRONIC_SIGNATURE_ENABLED, enabled ? "true" : "false");
    }
}
