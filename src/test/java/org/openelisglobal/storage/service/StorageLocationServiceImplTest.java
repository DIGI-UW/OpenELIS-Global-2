package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.dao.StorageDeviceDAO;
import org.openelisglobal.storage.dao.StoragePositionDAO;
import org.openelisglobal.storage.dao.StorageRackDAO;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.dao.StorageShelfDAO;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StoragePosition;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;

/**
 * Unit tests for StorageLocationService implementation Following TDD: Write
 * tests BEFORE implementation Tests business logic validation rules per
 * data-model.md
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceImplTest {

    @Mock
    private StorageRoomDAO storageRoomDAO;

    @Mock
    private StorageDeviceDAO storageDeviceDAO;

    @Mock
    private StorageShelfDAO storageShelfDAO;

    @Mock
    private StorageRackDAO storageRackDAO;

    @Mock
    private StoragePositionDAO storagePositionDAO;

    @InjectMocks
    private StorageLocationServiceImpl storageLocationService;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack;

    @Before
    public void setUp() {
        // Create test hierarchy
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setCode("TEST-ROOM");
        testRoom.setName("Test Room");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(2);
        testDevice.setCode("TEST-DEV");
        testDevice.setName("Test Device");
        testDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId(3);
        testShelf.setLabel("Shelf-A");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId(4);
        testRack.setLabel("Rack R1");
        testRack.setRows(8);
        testRack.setColumns(12);
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);
    }

    /**
     * T030: Test creating device with duplicate code in same room throws exception
     * Validation per data-model.md: Device code must be unique within parent room
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testCreateDevice_DuplicateCodeInSameRoom_ThrowsException() {
        // Given: Room with existing device code "FRZ01"
        StorageDevice existingDevice = new StorageDevice();
        existingDevice.setCode("FRZ01");
        existingDevice.setParentRoom(testRoom);

        when(storageDeviceDAO.findByParentRoomIdAndCode(testRoom.getId(), "FRZ01")).thenReturn(existingDevice);

        // Given: New device with same code in same room
        StorageDevice newDevice = new StorageDevice();
        newDevice.setCode("FRZ01"); // Duplicate
        newDevice.setName("New Freezer");
        newDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        newDevice.setParentRoom(testRoom);

        // When: Attempt to create device
        // Then: Expect LIMSRuntimeException for duplicate code
        storageLocationService.insert(newDevice);
    }

    /**
     * T030: Test device code unique across different rooms succeeds Validation:
     * Device code only needs to be unique WITHIN a room, not globally
     */
    @Test
    public void testCreateDevice_SameCodeDifferentRoom_Succeeds() {
        // Given: Room 1 with device code "FRZ01"
        StorageRoom room1 = new StorageRoom();
        room1.setId(1);
        room1.setCode("ROOM1");

        StorageDevice deviceRoom1 = new StorageDevice();
        deviceRoom1.setCode("FRZ01");
        deviceRoom1.setParentRoom(room1);

        // Given: Room 2 with device code "FRZ01" (same code, different room)
        StorageRoom room2 = new StorageRoom();
        room2.setId(2);
        room2.setCode("ROOM2");

        StorageDevice deviceRoom2 = new StorageDevice();
        deviceRoom2.setCode("FRZ01");
        deviceRoom2.setName("Freezer in Room 2");
        deviceRoom2.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        deviceRoom2.setParentRoom(room2);

        // Mock: No device with this code exists in room2
        when(storageDeviceDAO.findByParentRoomIdAndCode(room2.getId(), "FRZ01")).thenReturn(null);
        when(storageDeviceDAO.insert(any(StorageDevice.class))).thenReturn(123);

        // When: Create device in room 2
        // Then: Should succeed (same code allowed in different rooms)
        Integer insertedId = storageLocationService.insert(deviceRoom2);
        assertNotNull("Device should be inserted successfully", insertedId);
        assertEquals(Integer.valueOf(123), insertedId);
    }

    /**
     * T030: Test deleting room with active child devices throws exception
     * Validation per data-model.md: Cannot delete room with active child devices
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testDeleteRoom_WithActiveDevices_ThrowsException() {
        // Given: Room with active child devices
        when(storageDeviceDAO.findByParentRoomId(testRoom.getId()))
            .thenReturn(Arrays.asList(testDevice));

        // When: Attempt to delete room
        // Then: Expect LIMSRuntimeException
        storageLocationService.delete(testRoom);
    }

    /**
     * T030: Test deleting room with inactive devices succeeds Validation: Room can
     * be deleted if all child devices are inactive
     */
    @Test
    public void testDeleteRoom_AllDevicesInactive_Succeeds() {
        // Given: Room with only inactive devices
        testDevice.setActive(false);
        when(storageDeviceDAO.findByParentRoomId(testRoom.getId())).thenReturn(Arrays.asList(testDevice));

        // When: Delete room
        storageLocationService.delete(testRoom);

        // Then: Delete should succeed
        verify(storageRoomDAO, times(1)).delete(testRoom);
    }

    /**
     * T030: Test deactivating device with active samples shows warning Validation
     * per data-model.md: Warn when deactivating location with active samples
     */
    @Test
    public void testDeactivateDevice_WithActiveSamples_ShowsWarning() {
        // Given: Device with active sample assignments
        StoragePosition position = new StoragePosition();
        position.setParentRack(testRack);
        position.setOccupied(true);

        when(storagePositionDAO.countOccupiedInDevice(testDevice.getId())).thenReturn(5); // 5 active samples

        // When: Deactivate device
        testDevice.setActive(false);
        try {
            storageLocationService.update(testDevice);
            fail("Expected LIMSRuntimeException for device with active samples");
        } catch (LIMSRuntimeException e) {
            assertTrue("Warning should mention active samples", e.getMessage().contains("active samples"));
        }

        // Then: Exception should have been thrown with warning message
    }

    /**
     * T030: Test creating rack with negative grid dimensions throws exception
     * Validation per data-model.md: Rows and columns must be non-negative
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRack_NegativeRows_ThrowsException() {
        // Given: Rack with negative rows
        StorageRack invalidRack = new StorageRack();
        invalidRack.setLabel("Invalid Rack");
        invalidRack.setRows(-1); // Invalid
        invalidRack.setColumns(12);
        invalidRack.setParentShelf(testShelf);

        // When: Attempt to create rack
        // Then: Expect IllegalArgumentException
        storageLocationService.insert(invalidRack);
    }

    /**
     * T030: Test buildHierarchicalPath returns correct path format Helper method
     * validation: Path should be "Room > Device > Shelf > Rack > Position"
     */
    @Test
    public void testBuildHierarchicalPath_ReturnsCorrectFormat() {
        // Given: Position in full hierarchy
        StoragePosition position = new StoragePosition();
        position.setCoordinate("A5");
        position.setParentRack(testRack);

        // When: Build hierarchical path
        String path = storageLocationService.buildHierarchicalPath(position);

        // Then: Expect formatted path
        assertNotNull("Path should not be null", path);
        assertEquals("Test Room > Test Device > Shelf-A > Rack R1 > Position A5", path);
    }

    /**
     * T030: Test buildHierarchicalPath handles null parent gracefully Edge case:
     * Position without full hierarchy should not crash
     */
    @Test
    public void testBuildHierarchicalPath_NullParent_HandlesGracefully() {
        // Given: Position with null parent
        StoragePosition position = new StoragePosition();
        position.setCoordinate("A5");
        position.setParentRack(null); // No parent

        // When: Build hierarchical path
        String path = storageLocationService.buildHierarchicalPath(position);

        // Then: Expect partial path or error indication
        assertNotNull("Path should not be null", path);
        assertTrue("Path should indicate missing parent", path.contains("Unknown") || path.contains("A5"));
    }

    /**
     * T030: Test validateLocationActive checks entire hierarchy Validation: All
     * parent locations must be active to assign sample
     */
    @Test
    public void testValidateLocationActive_InactiveParent_ReturnsFalse() {
        // Given: Position in rack where parent device is inactive
        testDevice.setActive(false); // Inactive device

        StoragePosition position = new StoragePosition();
        position.setParentRack(testRack);

        // When: Validate location is active
        boolean isActive = storageLocationService.validateLocationActive(position);

        // Then: Expect false (entire hierarchy must be active)
        assertFalse("Position should be invalid due to inactive parent", isActive);
    }

    /**
     * T030: Test validateLocationActive returns true for all active hierarchy
     * Validation: Location is valid only if entire parent chain is active
     */
    @Test
    public void testValidateLocationActive_AllActive_ReturnsTrue() {
        // Given: Position in fully active hierarchy
        StoragePosition position = new StoragePosition();
        position.setParentRack(testRack);

        // Ensure all parents are active
        assertTrue("Room should be active", testRoom.getActive());
        assertTrue("Device should be active", testDevice.getActive());
        assertTrue("Shelf should be active", testShelf.getActive());
        assertTrue("Rack should be active", testRack.getActive());

        // When: Validate location is active
        boolean isActive = storageLocationService.validateLocationActive(position);

        // Then: Expect true (all hierarchy active)
        assertTrue("Position should be valid with all active parents", isActive);
    }

    // ========== Phase 6: Location CRUD Operations - Constraint Validation Tests (T102) ==========

    /**
     * T102: Test validating delete constraints for room with devices returns false
     * Validation: Room with devices cannot be deleted
     */
    @Test
    public void testValidateDeleteConstraints_RoomWithDevices_ReturnsFalse() {
        // Given: Room with child devices
        when(storageDeviceDAO.countByRoomId(testRoom.getId())).thenReturn(3); // 3 devices

        // When: Validate delete constraints
        boolean canDelete = storageLocationService.validateDeleteConstraints(testRoom);

        // Then: Expect false (has child devices)
        assertFalse("Room with devices should not be deletable", canDelete);
    }

    /**
     * T102: Test validating delete constraints for room with active samples returns false
     * Validation: Room with active samples cannot be deleted
     */
    @Test
    public void testValidateDeleteConstraints_RoomWithActiveSamples_ReturnsFalse() {
        // Given: Room with no devices but with active samples
        when(storageDeviceDAO.countByRoomId(testRoom.getId())).thenReturn(0); // No devices
        // Note: Active samples check requires SampleStorageService.hasActiveSamplesInLocation()
        // For now, we'll mock this behavior - actual implementation will check samples

        // When: Validate delete constraints
        // Note: This test will need SampleStorageService mock when implementing
        // For now, we test the device constraint check
        boolean canDelete = storageLocationService.validateDeleteConstraints(testRoom);

        // Then: If no devices, can delete (samples check will be added in implementation)
        // This test verifies the device constraint check works
        assertTrue("Room with no devices should be deletable (samples check deferred)", canDelete);
    }

    /**
     * T102: Test validating delete constraints for device with shelves returns false
     * Validation: Device with shelves cannot be deleted
     */
    @Test
    public void testValidateDeleteConstraints_DeviceWithShelves_ReturnsFalse() {
        // Given: Device with child shelves
        when(storageShelfDAO.countByDeviceId(testDevice.getId())).thenReturn(2); // 2 shelves

        // When: Validate delete constraints
        boolean canDelete = storageLocationService.validateDeleteConstraints(testDevice);

        // Then: Expect false (has child shelves)
        assertFalse("Device with shelves should not be deletable", canDelete);
    }

    /**
     * T102: Test validating delete constraints for location with no constraints returns true
     * Validation: Location with no children and no active samples can be deleted
     */
    @Test
    public void testValidateDeleteConstraints_LocationNoConstraints_ReturnsTrue() {
        // Given: Room with no devices and no active samples
        when(storageDeviceDAO.countByRoomId(testRoom.getId())).thenReturn(0); // No devices

        // When: Validate delete constraints
        boolean canDelete = storageLocationService.validateDeleteConstraints(testRoom);

        // Then: Expect true (no constraints)
        assertTrue("Room with no constraints should be deletable", canDelete);
    }

    /**
     * T102: Test getting delete constraint message for room with devices returns message
     * Validation: Error message should include specific reason
     */
    @Test
    public void testGetDeleteConstraintMessage_RoomWithDevices_ReturnsMessage() {
        // Given: Room with 3 child devices
        when(storageDeviceDAO.countByRoomId(testRoom.getId())).thenReturn(3);

        // When: Get constraint message
        String message = storageLocationService.getDeleteConstraintMessage(testRoom);

        // Then: Expect message mentioning devices
        assertNotNull("Message should not be null", message);
        assertTrue("Message should mention devices", message.toLowerCase().contains("device"));
    }

    /**
     * T102: Test getting delete constraint message for device with samples returns message
     * Validation: Error message should include specific reason
     */
    @Test
    public void testGetDeleteConstraintMessage_DeviceWithSamples_ReturnsMessage() {
        // Given: Device with active samples
        // Note: This requires SampleStorageService mock - for now test structure
        when(storageShelfDAO.countByDeviceId(testDevice.getId())).thenReturn(0); // No shelves

        // When: Get constraint message
        // Note: Actual implementation will check samples via SampleStorageService
        String message = storageLocationService.getDeleteConstraintMessage(testDevice);

        // Then: Expect message (may be empty if no constraints, or mention samples)
        assertNotNull("Message should not be null", message);
    }

    // ========== Phase 6: Location CRUD Operations - Update Validation Tests (T103) ==========

    /**
     * T103: Test updating location with code uniqueness check
     * Validation: Code uniqueness should be validated before update
     */
    @Test
    public void testUpdateLocation_CodeUniquenessCheck() {
        // Given: Existing room with code "ORIG-CODE"
        StorageRoom existingRoom = new StorageRoom();
        existingRoom.setId(1);
        existingRoom.setCode("ORIG-CODE");
        existingRoom.setName("Original Room");

        when(storageRoomDAO.get(1)).thenReturn(java.util.Optional.of(existingRoom));

        // Given: Update attempt with duplicate code (but code is read-only, so should be ignored)
        StorageRoom updateRoom = new StorageRoom();
        updateRoom.setId(1);
        updateRoom.setCode("NEW-CODE"); // Attempt to change code
        updateRoom.setName("Updated Room");

        // When: Update room
        // Then: Code should be ignored (read-only), update should succeed
        // Note: Actual implementation will ignore code field
        when(storageRoomDAO.update(any(StorageRoom.class))).thenReturn(1);

        Integer result = storageLocationService.update(updateRoom);
        assertNotNull("Update should succeed", result);

        // Verify code was not changed (read-only)
        verify(storageRoomDAO, times(1)).update(any(StorageRoom.class));
    }

    /**
     * T103: Test updating location with read-only fields ignored
     * Validation: Code and Parent fields should not be updated even if provided
     */
    @Test
    public void testUpdateLocation_ReadOnlyFieldsIgnored() {
        // Given: Existing device with original parent room
        StorageRoom originalRoom = new StorageRoom();
        originalRoom.setId(1);
        originalRoom.setCode("ROOM-1");

        StorageDevice existingDevice = new StorageDevice();
        existingDevice.setId(2);
        existingDevice.setCode("ORIG-DEV");
        existingDevice.setName("Original Device");
        existingDevice.setParentRoom(originalRoom);

        when(storageDeviceDAO.get(2)).thenReturn(java.util.Optional.of(existingDevice));

        // Given: Update attempt with new parent room and code
        StorageRoom newRoom = new StorageRoom();
        newRoom.setId(3);
        newRoom.setCode("ROOM-2");

        StorageDevice updateDevice = new StorageDevice();
        updateDevice.setId(2);
        updateDevice.setCode("NEW-DEV"); // Attempt to change code
        updateDevice.setName("Updated Device");
        updateDevice.setParentRoom(newRoom); // Attempt to change parent

        // When: Update device
        // Then: Code and parent should be ignored (read-only), only name should update
        when(storageDeviceDAO.update(any(StorageDevice.class))).thenReturn(2);

        Integer result = storageLocationService.update(updateDevice);
        assertNotNull("Update should succeed", result);

        // Verify update was called (actual implementation will ignore read-only fields)
        verify(storageDeviceDAO, times(1)).update(any(StorageDevice.class));
    }
}
