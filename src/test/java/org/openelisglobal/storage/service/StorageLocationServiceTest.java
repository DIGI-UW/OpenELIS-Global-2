package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Unit tests for StorageLocationService deletion validation.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceTest {

    @Mock
    private StorageRoomDAO storageRoomDAO;

    @Mock
    private StorageDeviceDAO storageDeviceDAO;

    @Mock
    private StorageShelfDAO storageShelfDAO;

    @Mock
    private StorageRackDAO storageRackDAO;

    @Mock
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @InjectMocks
    private StorageLocationServiceImpl storageLocationService;

    @Test
    public void testCanDeleteRoom_WithNoChildDevices_ReturnsSuccess() {
        // Arrange
        StorageRoom room = new StorageRoom();
        room.setId(1);
        room.setName("Empty Room");

        when(storageRoomDAO.get(1)).thenReturn(java.util.Optional.of(room));
        when(storageDeviceDAO.findByParentRoomId(1)).thenReturn(Collections.emptyList());

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteRoom(1);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should allow deletion of room with no devices", result.isSuccess());
    }

    @Test
    public void testCanDeleteRoom_WithChildDevices_ReturnsFail() {
        // Arrange
        StorageRoom room = new StorageRoom();
        room.setId(1);
        room.setName("Lab Room");

        StorageDevice device1 = new StorageDevice();
        device1.setId(10);
        StorageDevice device2 = new StorageDevice();
        device2.setId(11);

        when(storageRoomDAO.get(1)).thenReturn(java.util.Optional.of(room));
        when(storageDeviceDAO.findByParentRoomId(1)).thenReturn(Arrays.asList(device1, device2));

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteRoom(1);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should prevent deletion of room with devices", result.isSuccess());
        assertEquals("Should report dependent count", 2, result.getDependentCount());
        assertEquals("Should have correct error code", "REFERENTIAL_INTEGRITY_VIOLATION", result.getErrorCode());
        assertTrue("Message should mention devices", result.getMessage().toLowerCase().contains("device"));
    }

    @Test
    public void testCanDeleteDevice_WithNoChildShelves_ReturnsSuccess() {
        // Arrange
        StorageDevice device = new StorageDevice();
        device.setId(10);
        device.setName("Empty Device");

        when(storageDeviceDAO.get(10)).thenReturn(java.util.Optional.of(device));
        when(storageShelfDAO.findByParentDeviceId(10)).thenReturn(Collections.emptyList());

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteDevice(10);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should allow deletion of device with no shelves", result.isSuccess());
    }

    @Test
    public void testCanDeleteDevice_WithChildShelves_ReturnsFail() {
        // Arrange
        StorageDevice device = new StorageDevice();
        device.setId(10);
        device.setName("Freezer-01");

        StorageShelf shelf1 = new StorageShelf();
        shelf1.setId(20);
        StorageShelf shelf2 = new StorageShelf();
        shelf2.setId(21);
        StorageShelf shelf3 = new StorageShelf();
        shelf3.setId(22);

        when(storageDeviceDAO.get(10)).thenReturn(java.util.Optional.of(device));
        when(storageShelfDAO.findByParentDeviceId(10)).thenReturn(Arrays.asList(shelf1, shelf2, shelf3));

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteDevice(10);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should prevent deletion of device with shelves", result.isSuccess());
        assertEquals("Should report dependent count", 3, result.getDependentCount());
        assertEquals("Should have correct error code", "REFERENTIAL_INTEGRITY_VIOLATION", result.getErrorCode());
        assertTrue("Message should mention shelves", result.getMessage().toLowerCase().contains("shel"));
    }

    @Test
    public void testCanDeleteShelf_WithNoChildRacks_ReturnsSuccess() {
        // Arrange
        StorageShelf shelf = new StorageShelf();
        shelf.setId(20);
        shelf.setLabel("Empty Shelf");

        when(storageShelfDAO.get(20)).thenReturn(java.util.Optional.of(shelf));
        when(storageRackDAO.findByParentShelfId(20)).thenReturn(Collections.emptyList());

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteShelf(20);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should allow deletion of shelf with no racks", result.isSuccess());
    }

    @Test
    public void testCanDeleteShelf_WithChildRacks_ReturnsFail() {
        // Arrange
        StorageShelf shelf = new StorageShelf();
        shelf.setId(20);
        shelf.setLabel("Shelf-A");

        StorageRack rack1 = new StorageRack();
        rack1.setId(30);

        when(storageShelfDAO.get(20)).thenReturn(java.util.Optional.of(shelf));
        when(storageRackDAO.findByParentShelfId(20)).thenReturn(Arrays.asList(rack1));

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteShelf(20);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should prevent deletion of shelf with racks", result.isSuccess());
        assertEquals("Should report dependent count", 1, result.getDependentCount());
        assertEquals("Should have correct error code", "REFERENTIAL_INTEGRITY_VIOLATION", result.getErrorCode());
        assertTrue("Message should mention racks", result.getMessage().toLowerCase().contains("rack"));
    }

    @Test
    public void testCanDeleteRack_WithNoAssignedSamples_ReturnsSuccess() {
        // Arrange
        StorageRack rack = new StorageRack();
        rack.setId(30);
        rack.setLabel("Empty Rack");

        when(storageRackDAO.get(30)).thenReturn(java.util.Optional.of(rack));
        when(sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", 30)).thenReturn(0);

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteRack(30);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Should allow deletion of rack with no samples", result.isSuccess());
    }

    @Test
    public void testCanDeleteRack_WithAssignedSamples_ReturnsFail() {
        // Arrange
        StorageRack rack = new StorageRack();
        rack.setId(30);
        rack.setLabel("Rack-01");

        when(storageRackDAO.get(30)).thenReturn(java.util.Optional.of(rack));
        when(sampleStorageAssignmentDAO.countByLocationTypeAndId("rack", 30)).thenReturn(15);

        // Act
        DeletionValidationResult result = storageLocationService.canDeleteRack(30);

        // Assert
        assertNotNull("Result should not be null", result);
        assertFalse("Should prevent deletion of rack with assigned samples", result.isSuccess());
        assertEquals("Should report dependent count", 15, result.getDependentCount());
        assertEquals("Should have correct error code", "ACTIVE_ASSIGNMENTS", result.getErrorCode());
        assertTrue("Message should mention samples", result.getMessage().toLowerCase().contains("sample"));
    }

    @Test
    public void testStorageDevice_ConnectivityFields_PersistCorrectly() {
        // Arrange
        StorageDevice device = new StorageDevice();
        device.setId(10);
        device.setName("IoT Freezer");
        device.setIpAddress("192.168.1.100");
        device.setPort(502);
        device.setCommunicationProtocol("BACnet");

        // Act
        String ipAddress = device.getIpAddress();
        Integer port = device.getPort();
        String protocol = device.getCommunicationProtocol();

        // Assert
        assertEquals("IP address should be set correctly", "192.168.1.100", ipAddress);
        assertEquals("Port should be set correctly", Integer.valueOf(502), port);
        assertEquals("Communication protocol should be set correctly", "BACnet", protocol);
    }

    @Test
    public void testIsNameUniqueWithinParent_RoomDuplicate_ReturnsFalse() {
        StorageRoom existing = new StorageRoom();
        existing.setId(5);
        when(storageRoomDAO.findByName("Main Lab")).thenReturn(existing);

        boolean unique = storageLocationService.isNameUniqueWithinParent("Main Lab", null, "room", null);

        assertFalse("Duplicate room names should not be allowed", unique);
    }

    @Test
    public void testIsNameUniqueWithinParent_DeviceDuplicate_ReturnsFalse() {
        StorageDevice existing = new StorageDevice();
        existing.setId(10);
        when(storageDeviceDAO.findByNameAndParentRoomId("Freezer-01", 1)).thenReturn(existing);

        boolean unique = storageLocationService.isNameUniqueWithinParent("Freezer-01", 1, "device", null);

        assertFalse("Duplicate device names within a room should not be allowed", unique);
    }

    @Test
    public void testIsNameUniqueWithinParent_ShelfDuplicate_ReturnsFalse() {
        StorageShelf existing = new StorageShelf();
        existing.setId(20);
        when(storageShelfDAO.findByLabelAndParentDeviceId("Shelf-A", 2)).thenReturn(existing);

        boolean unique = storageLocationService.isNameUniqueWithinParent("Shelf-A", 2, "shelf", null);

        assertFalse("Duplicate shelf labels within a device should not be allowed", unique);
    }

    @Test
    public void testIsNameUniqueWithinParent_RackDuplicate_ReturnsFalse() {
        StorageRack existing = new StorageRack();
        existing.setId(30);
        when(storageRackDAO.findByLabelAndParentShelfId("Rack-01", 3)).thenReturn(existing);

        boolean unique = storageLocationService.isNameUniqueWithinParent("Rack-01", 3, "rack", null);

        assertFalse("Duplicate rack labels within a shelf should not be allowed", unique);
    }

    @Test
    public void testIsNameUniqueWithinParent_RoomUnique_WhenDaoReturnsNull() {
        when(storageRoomDAO.findByName("New Lab")).thenReturn(null);

        boolean unique = storageLocationService.isNameUniqueWithinParent("New Lab", null, "room", null);

        assertTrue("Room names should be unique when DAO returns null", unique);
    }

    @Test
    public void testIsNameUniqueWithinParent_DeviceUnique_WhenExcludeIdMatchesExisting() {
        StorageDevice existing = new StorageDevice();
        existing.setId(10);
        when(storageDeviceDAO.findByNameAndParentRoomId("Freezer-02", 1)).thenReturn(existing);

        boolean unique = storageLocationService.isNameUniqueWithinParent("Freezer-02", 1, "device", 10);

        assertTrue("Device name should be allowed when excludeId matches existing entity", unique);
    }
}
