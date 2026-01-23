package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.DocumentationVerificationService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.VerificationItemStatus;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.DocumentationStatus;
import org.openelisglobal.biorepository.valueholder.Shipment.PackagingCondition;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for DocumentationVerificationService.
 *
 * DocumentationVerification is now at shipment level (one verification per
 * shipment). Tests verify the 7-point verification checklist workflow: - Points
 * 1-2: Auto-verified (system validates against shipment/project data) - Points
 * 3-5: Manual verification required - Points 6-7: Conditional (N/A allowed with
 * justification)
 */
public class DocumentationVerificationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DocumentationVerificationService verificationService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser testUser;

    @Before
    public void setUp() {
        // Setup test user
        testUser = systemUserService.get("1");
        if (testUser == null) {
            testUser = new SystemUser();
            testUser.setLoginName("test_verification_user");
            testUser.setFirstName("Test");
            testUser.setLastName("Verification User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }
    }

    // ========== CREATE FOR SHIPMENT TESTS ==========

    @Test
    public void testCreateForShipment_Success() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-CREATE-" + System.currentTimeMillis());

        // Act
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Assert
        assertNotNull("Verification ID should be generated", verification.getId());
        assertNotNull("Shipment link should be set", verification.getShipment());
        assertEquals("Shipment ID should match", shipment.getId(), verification.getShipment().getId());
        assertEquals("Overall status should be PENDING", OverallStatus.PENDING, verification.getOverallStatus());

        // All items should start as PENDING
        assertEquals("Sample identifiers status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusSampleIdentifiers());
        assertEquals("Project linkage status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusProjectLinkage());
        assertEquals("Ethics approval status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusEthicsApproval());
        assertEquals("Biosafety match status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusBiosafetyMatch());
        assertEquals("Packaging integrity status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusPackagingIntegrity());
        assertEquals("Consent record status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusConsentRecord());
        assertEquals("MTA documented status should be PENDING", VerificationItemStatus.PENDING,
                verification.getStatusMtaDocumented());
    }

    @Test
    public void testCreateForShipment_ExistingVerification_ReturnsExisting() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-EXISTING-" + System.currentTimeMillis());
        DocumentationVerification first = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act - Try to create another verification for same shipment
        DocumentationVerification second = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Assert - Should return the existing verification
        assertEquals("Should return existing verification ID", first.getId(), second.getId());
    }

    @Test(expected = Exception.class)
    public void testCreateForShipment_NonExistentShipment_ThrowsException() {
        // Act - should throw (ObjectNotFoundException or IllegalArgumentException)
        verificationService.createForShipment(999999, testUser.getId().toString());
    }

    // ========== GET BY SHIPMENT TESTS ==========

    @Test
    public void testGetByShipmentId_Found() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-GET-" + System.currentTimeMillis());
        DocumentationVerification created = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act
        DocumentationVerification found = verificationService.getByShipmentId(shipment.getId());

        // Assert
        assertNotNull("Verification should be found", found);
        assertEquals("ID should match", created.getId(), found.getId());
    }

    @Test
    public void testGetByShipmentId_NotFound() {
        // Act
        DocumentationVerification found = verificationService.getByShipmentId(999999);

        // Assert
        assertNull("Should return null for non-existent shipment", found);
    }

    @Test
    public void testExistsByShipmentId() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-EXISTS-" + System.currentTimeMillis());

        // Assert - before creating verification
        assertFalse("Should not exist before creation", verificationService.existsByShipmentId(shipment.getId()));

        // Create verification
        verificationService.createForShipment(shipment.getId(), testUser.getId().toString());

        // Assert - after creating verification
        assertTrue("Should exist after creation", verificationService.existsByShipmentId(shipment.getId()));
    }

    // ========== UPDATE VERIFICATION ITEM TESTS ==========

    @Test
    public void testUpdateVerificationItem_SetVerified() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-ITEM-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act - Verify sample identifiers
        DocumentationVerification updated = verificationService.updateVerificationItem(verification.getId(),
                "sampleIdentifiers", true, false, null);

        // Assert
        assertTrue("Sample identifiers check should be true", updated.isCheckSampleIdentifiers());
        assertEquals("Sample identifiers status should be VERIFIED", VerificationItemStatus.VERIFIED,
                updated.getStatusSampleIdentifiers());
        assertEquals("Completed count should be 1", 1, updated.getCompletedCount());
    }

    @Test
    public void testUpdateVerificationItem_SetNotApplicable_WithJustification() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-NA-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act - Mark consent as N/A with justification
        DocumentationVerification updated = verificationService.updateVerificationItem(verification.getId(),
                "consentRecord", false, true, "Anonymous samples - no consent required");

        // Assert
        assertFalse("Consent check should be false", updated.isCheckConsentRecord());
        assertEquals("Consent status should be N_A", VerificationItemStatus.N_A, updated.getStatusConsentRecord());
        assertEquals("Consent NA justification should be set", "Anonymous samples - no consent required",
                updated.getNaJustificationConsent());
    }

    @Test
    public void testUpdateVerificationItem_AllItems() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-ALL-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act - Verify all mandatory items
        verificationService.updateVerificationItem(verification.getId(), "sampleIdentifiers", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "projectLinkage", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "ethicsApproval", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "biosafetyMatch", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "packagingIntegrity", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "consentRecord", true, false, null);
        DocumentationVerification updated = verificationService.updateVerificationItem(verification.getId(),
                "mtaDocumented", true, false, null);

        // Assert
        assertEquals("Completed count should be 7", 7, updated.getCompletedCount());
        assertTrue("Verification should be complete", updated.isComplete());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateVerificationItem_UnknownItem_ThrowsException() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-UNKNOWN-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Act - should throw
        verificationService.updateVerificationItem(verification.getId(), "unknownItem", true, false, null);
    }

    // ========== COMPLETE VERIFICATION TESTS ==========

    @Test
    public void testCompleteVerification_Success() {
        // Arrange - Create and fully verify
        Shipment shipment = createTestShipment("VERIFY-COMPLETE-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Verify all items
        verificationService.updateVerificationItem(verification.getId(), "sampleIdentifiers", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "projectLinkage", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "ethicsApproval", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "biosafetyMatch", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "packagingIntegrity", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "consentRecord", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "mtaDocumented", true, false, null);

        // Act
        DocumentationVerification completed = verificationService.completeVerification(verification.getId(),
                Integer.valueOf(testUser.getId()));

        // Assert
        assertEquals("Overall status should be VERIFIED", OverallStatus.VERIFIED, completed.getOverallStatus());
        assertNotNull("Verified timestamp should be set", completed.getVerifiedTimestamp());
        assertNotNull("Verified by user should be set", completed.getVerifiedByUser());
        assertEquals("Verifier ID should match", testUser.getId(), completed.getVerifiedByUser().getId());

        // Check shipment documentation status was updated
        Shipment updatedShipment = shipmentService.get(shipment.getId());
        assertEquals("Shipment documentation status should be VERIFIED", DocumentationStatus.VERIFIED,
                updatedShipment.getDocumentationStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testCompleteVerification_Incomplete_ThrowsException() {
        // Arrange - Create but don't verify all items
        Shipment shipment = createTestShipment("VERIFY-INCOMPLETE-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Only verify some items
        verificationService.updateVerificationItem(verification.getId(), "sampleIdentifiers", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "projectLinkage", true, false, null);

        // Act - should throw because not all items verified
        verificationService.completeVerification(verification.getId(), Integer.valueOf(testUser.getId()));
    }

    // ========== QUARANTINE TESTS ==========

    @Test
    public void testQuarantineShipment_Success() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-QUARANTINE-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        String quarantineReason = "Missing ethics approval documentation";

        // Act
        DocumentationVerification quarantined = verificationService.quarantineShipment(verification.getId(),
                Integer.valueOf(testUser.getId()), quarantineReason);

        // Assert
        assertEquals("Overall status should be QUARANTINE", OverallStatus.QUARANTINE, quarantined.getOverallStatus());
        assertNotNull("Verified timestamp should be set", quarantined.getVerifiedTimestamp());
        assertEquals("Verification notes should contain quarantine reason", quarantineReason,
                quarantined.getVerificationNotes());

        // Check shipment documentation status was updated
        Shipment updatedShipment = shipmentService.get(shipment.getId());
        assertEquals("Shipment documentation status should be QUARANTINE", DocumentationStatus.QUARANTINE,
                updatedShipment.getDocumentationStatus());
    }

    // ========== QUERY BY STATUS TESTS ==========

    @Test
    public void testGetByOverallStatus() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-STATUS-" + System.currentTimeMillis());
        verificationService.createForShipment(shipment.getId(), testUser.getId().toString());

        // Act
        List<DocumentationVerification> pending = verificationService.getByOverallStatus(OverallStatus.PENDING);

        // Assert
        assertFalse("Should find at least one pending verification", pending.isEmpty());
        assertTrue("All results should be PENDING",
                pending.stream().allMatch(v -> OverallStatus.PENDING.equals(v.getOverallStatus())));
    }

    @Test
    public void testGetPendingVerifications() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-PENDING-" + System.currentTimeMillis());
        verificationService.createForShipment(shipment.getId(), testUser.getId().toString());

        // Act
        List<DocumentationVerification> pending = verificationService.getPendingVerifications(50);

        // Assert
        assertFalse("Should find at least one pending verification", pending.isEmpty());
    }

    @Test
    public void testCountByOverallStatus() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-COUNT-" + System.currentTimeMillis());
        verificationService.createForShipment(shipment.getId(), testUser.getId().toString());

        // Act
        long pendingCount = verificationService.countByOverallStatus(OverallStatus.PENDING);

        // Assert
        assertTrue("Should have at least 1 pending verification", pendingCount >= 1);
    }

    // ========== VERIFICATION PROGRESS TESTS ==========

    @Test
    public void testVerificationProgress_CompletedCount() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-PROGRESS-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Assert initial state
        assertEquals("Initial completed count should be 0", 0, verification.getCompletedCount());
        assertEquals("Total count should be 7", 7, verification.getTotalCount());
        assertFalse("Should not be complete initially", verification.isComplete());

        // Verify 3 items
        verificationService.updateVerificationItem(verification.getId(), "sampleIdentifiers", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "projectLinkage", true, false, null);
        DocumentationVerification updated = verificationService.updateVerificationItem(verification.getId(),
                "ethicsApproval", true, false, null);

        // Assert progress
        assertEquals("Completed count should be 3", 3, updated.getCompletedCount());
        assertFalse("Should not be complete with 3/7", updated.isComplete());
    }

    @Test
    public void testVerificationProgress_NAWithJustificationCountsAsComplete() {
        // Arrange
        Shipment shipment = createTestShipment("VERIFY-NA-COMPLETE-" + System.currentTimeMillis());
        DocumentationVerification verification = verificationService.createForShipment(shipment.getId(),
                testUser.getId().toString());

        // Verify mandatory items
        verificationService.updateVerificationItem(verification.getId(), "sampleIdentifiers", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "projectLinkage", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "ethicsApproval", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "biosafetyMatch", true, false, null);
        verificationService.updateVerificationItem(verification.getId(), "packagingIntegrity", true, false, null);

        // Mark conditional items as N/A with justification
        verificationService.updateVerificationItem(verification.getId(), "consentRecord", false, true,
                "Anonymous samples");
        DocumentationVerification updated = verificationService.updateVerificationItem(verification.getId(),
                "mtaDocumented", false, true, "Internal samples - no MTA needed");

        // Assert - N/A items with justification should count as complete
        assertEquals("Completed count should be 7", 7, updated.getCompletedCount());
        assertTrue("Should be complete with all items verified or N/A", updated.isComplete());
    }

    // ========== HELPER METHODS ==========

    private Shipment createTestShipment(String deliveryReference) {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference(deliveryReference);
        shipment.setSenderName("Test Sender");
        shipment.setSenderOrganization("Test Organization");
        shipment.setReceiver(testUser);
        shipment.setPackagingCondition(PackagingCondition.INTACT);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setSysUserId(testUser.getId().toString());
        return shipmentService.receiveShipment(shipment);
    }
}
