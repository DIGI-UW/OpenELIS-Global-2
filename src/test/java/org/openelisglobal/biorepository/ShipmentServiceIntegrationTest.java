package org.openelisglobal.biorepository;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.DocumentationStatus;
import org.openelisglobal.biorepository.valueholder.Shipment.PackagingCondition;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for ShipmentService. Tests the complete flow of shipment
 * operations in the biorepository module.
 */
public class ShipmentServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private SystemUserService systemUserService;

    private SystemUser testUser;

    @Before
    public void setUp() {
        // Get existing admin user for testing
        testUser = systemUserService.get("1");
        if (testUser == null) {
            // Create a test user if none exists
            testUser = new SystemUser();
            testUser.setLoginName("test_biorepository_user");
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setIsActive("Y");
            testUser.setSysUserId("1");
            systemUserService.save(testUser);
        }
    }

    @Test
    public void testReceiveShipment_Success() {
        // Arrange
        Shipment shipment = createTestShipment("TEST-REF-001");

        // Act
        Shipment received = shipmentService.receiveShipment(shipment);

        // Assert
        assertNotNull("Shipment should be saved", received.getId());
        assertEquals("Status should be RECEIVED", ShipmentStatus.RECEIVED, received.getStatus());
        assertNotNull("Reception timestamp should be set", received.getReceptionTimestamp());
        assertEquals("Delivery reference should match", "TEST-REF-001", received.getDeliveryReference());
    }

    @Test
    public void testReceiveShipment_WithAllFields() {
        // Arrange
        Shipment shipment = createTestShipment("TEST-REF-002");
        shipment.setTransportTemperature(new BigDecimal("-20.5"));
        shipment.setExpectedSampleCount(10);
        shipment.setPackagingCondition(PackagingCondition.DAMAGED);
        shipment.setPackagingConditionNotes("Minor dent on corner");

        // Act
        Shipment received = shipmentService.receiveShipment(shipment);

        // Assert
        assertNotNull(received.getId());
        assertEquals(new BigDecimal("-20.5"), received.getTransportTemperature());
        assertEquals(Integer.valueOf(10), received.getExpectedSampleCount());
        assertEquals(PackagingCondition.DAMAGED, received.getPackagingCondition());
        assertEquals("Minor dent on corner", received.getPackagingConditionNotes());
    }

    @Test(expected = LIMSDuplicateRecordException.class)
    public void testReceiveShipment_DuplicateReference_ThrowsException() {
        // Arrange
        Shipment shipment1 = createTestShipment("DUPLICATE-REF");
        shipmentService.receiveShipment(shipment1);

        Shipment shipment2 = createTestShipment("DUPLICATE-REF");

        // Act - should throw exception
        shipmentService.receiveShipment(shipment2);
    }

    @Test
    public void testGetByDeliveryReference() {
        // Arrange
        Shipment shipment = createTestShipment("SEARCH-REF-001");
        shipmentService.receiveShipment(shipment);

        // Act
        Shipment found = shipmentService.getByDeliveryReference("SEARCH-REF-001");

        // Assert
        assertNotNull("Shipment should be found", found);
        assertEquals("SEARCH-REF-001", found.getDeliveryReference());
    }

    @Test
    public void testGetByDeliveryReference_NotFound() {
        // Act
        Shipment found = shipmentService.getByDeliveryReference("NON-EXISTENT-REF");

        // Assert
        assertNull("Should return null for non-existent reference", found);
    }

    @Test
    public void testCompleteShipment() {
        // Arrange
        Shipment shipment = createTestShipment("COMPLETE-REF-001");
        Shipment received = shipmentService.receiveShipment(shipment);

        // Act
        Shipment completed = shipmentService.completeShipment(received.getId());

        // Assert
        assertEquals(ShipmentStatus.COMPLETED, completed.getStatus());
    }

    @Test(expected = Exception.class)
    public void testCompleteShipment_NotFound_ThrowsException() {
        // Act - should throw exception (ObjectNotFoundException or
        // IllegalArgumentException)
        shipmentService.completeShipment(999999);
    }

    @Test
    public void testCountByStatus() {
        // Arrange
        Shipment shipment1 = createTestShipment("COUNT-REF-001");
        Shipment shipment2 = createTestShipment("COUNT-REF-002");
        shipmentService.receiveShipment(shipment1);
        Shipment received2 = shipmentService.receiveShipment(shipment2);
        shipmentService.completeShipment(received2.getId());

        // Act
        long receivedCount = shipmentService.countByStatus(ShipmentStatus.RECEIVED);
        long completedCount = shipmentService.countByStatus(ShipmentStatus.COMPLETED);

        // Assert
        assertTrue("Should have at least 1 received shipment", receivedCount >= 1);
        assertTrue("Should have at least 1 completed shipment", completedCount >= 1);
    }

    @Test
    public void testSearch() {
        // Arrange
        Shipment shipment = createTestShipment("SEARCHABLE-REF");
        shipment.setSenderName("Unique Sender Name XYZ");
        shipmentService.receiveShipment(shipment);

        // Act
        var results = shipmentService.search("Unique Sender", 10);

        // Assert
        assertFalse("Should find at least one result", results.isEmpty());
        assertTrue("Results should contain the shipment",
                results.stream().anyMatch(s -> "SEARCHABLE-REF".equals(s.getDeliveryReference())));
    }

    // ========== DOCUMENTATION STATUS TESTS ==========

    @Test
    public void testUpdateDocumentationStatus_ToVerified() {
        // Arrange
        Shipment shipment = createTestShipment("DOC-STATUS-VERIFY-" + System.currentTimeMillis());
        Shipment received = shipmentService.receiveShipment(shipment);

        // Initial status should be PENDING
        assertEquals("Initial documentation status should be PENDING", DocumentationStatus.PENDING,
                received.getDocumentationStatus());

        // Act
        Shipment updated = shipmentService.updateDocumentationStatus(received.getId(), DocumentationStatus.VERIFIED);

        // Assert
        assertEquals("Documentation status should be VERIFIED", DocumentationStatus.VERIFIED,
                updated.getDocumentationStatus());

        // Verify persistence
        Shipment fetched = shipmentService.get(received.getId());
        assertEquals("Persisted documentation status should be VERIFIED", DocumentationStatus.VERIFIED,
                fetched.getDocumentationStatus());
    }

    @Test
    public void testUpdateDocumentationStatus_ToQuarantine() {
        // Arrange
        Shipment shipment = createTestShipment("DOC-STATUS-QUAR-" + System.currentTimeMillis());
        Shipment received = shipmentService.receiveShipment(shipment);

        // Act
        Shipment updated = shipmentService.updateDocumentationStatus(received.getId(), DocumentationStatus.QUARANTINE);

        // Assert
        assertEquals("Documentation status should be QUARANTINE", DocumentationStatus.QUARANTINE,
                updated.getDocumentationStatus());
    }

    @Test
    public void testUpdateDocumentationStatus_BackToPending() {
        // Arrange - Create and set to VERIFIED
        Shipment shipment = createTestShipment("DOC-STATUS-PENDING-" + System.currentTimeMillis());
        Shipment received = shipmentService.receiveShipment(shipment);
        shipmentService.updateDocumentationStatus(received.getId(), DocumentationStatus.VERIFIED);

        // Act - Set back to PENDING
        Shipment updated = shipmentService.updateDocumentationStatus(received.getId(), DocumentationStatus.PENDING);

        // Assert
        assertEquals("Documentation status should be PENDING", DocumentationStatus.PENDING,
                updated.getDocumentationStatus());
    }

    @Test(expected = Exception.class)
    public void testUpdateDocumentationStatus_NotFound_ThrowsException() {
        // Act - should throw exception (ObjectNotFoundException or
        // IllegalArgumentException)
        shipmentService.updateDocumentationStatus(999999, DocumentationStatus.VERIFIED);
    }

    @Test
    public void testReceiveShipment_DefaultDocumentationStatus() {
        // Arrange
        Shipment shipment = createTestShipment("DOC-STATUS-DEFAULT-" + System.currentTimeMillis());

        // Act
        Shipment received = shipmentService.receiveShipment(shipment);

        // Assert - Default documentation status should be PENDING
        assertEquals("Default documentation status should be PENDING", DocumentationStatus.PENDING,
                received.getDocumentationStatus());
    }

    /**
     * Helper method to create a test shipment with required fields.
     */
    private Shipment createTestShipment(String deliveryReference) {
        Shipment shipment = new Shipment();
        shipment.setDeliveryReference(deliveryReference);
        shipment.setSenderName("Test Sender");
        shipment.setSenderOrganization("Test Organization");
        shipment.setReceiver(testUser);
        shipment.setPackagingCondition(PackagingCondition.INTACT);
        shipment.setReceptionTimestamp(new Timestamp(System.currentTimeMillis()));
        shipment.setSysUserId(testUser.getId().toString());
        return shipment;
    }
}
