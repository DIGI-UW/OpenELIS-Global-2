package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;

/**
 * Integration tests for StorageLocationService buildHierarchicalPath method
 * with flexible hierarchy (2-5 levels).
 * 
 * T026b: Tests buildHierarchicalPath with optional parents
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceHierarchyTest {

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
    private StoragePosition testPosition;

    @Before
    public void setUp() {
        // Create test hierarchy components
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setFhirUuid(UUID.randomUUID());
        testRoom.setCode("MAIN");
        testRoom.setName("Main Laboratory");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(10);
        testDevice.setFhirUuid(UUID.randomUUID());
        testDevice.setCode("FRZ01");
        testDevice.setName("Freezer Unit 1");
        testDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId(20);
        testShelf.setFhirUuid(UUID.randomUUID());
        testShelf.setLabel("Shelf-A");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId(30);
        testRack.setFhirUuid(UUID.randomUUID());
        testRack.setLabel("Rack R1");
        testRack.setRows(8);
        testRack.setColumns(12);
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);
    }

    /**
     * Test building path for device-level position (2 levels: Room > Device)
     */
    @Test
    public void testBuildPath_DeviceLevel_ReturnsRoomAndDevice() {
        // Given: Position at device level (only device, no shelf/rack/coordinate)
        testPosition = new StoragePosition();
        testPosition.setId(40);
        testPosition.setFhirUuid(UUID.randomUUID());
        testPosition.setParentDevice(testDevice);
        // No parent shelf, rack, or coordinate

        // When: Building hierarchical path
        String path = storageLocationService.buildHierarchicalPath(testPosition);

        // Then: Should return "Room > Device"
        assertNotNull("Path should not be null", path);
        assertEquals("Main Laboratory > Freezer Unit 1", path);
    }

    /**
     * Test building path for shelf-level position (3 levels: Room > Device > Shelf)
     */
    @Test
    public void testBuildPath_ShelfLevel_ReturnsRoomDeviceShelf() {
        // Given: Position at shelf level (device + shelf, no rack/coordinate)
        testPosition = new StoragePosition();
        testPosition.setId(40);
        testPosition.setFhirUuid(UUID.randomUUID());
        testPosition.setParentDevice(testDevice);
        testPosition.setParentShelf(testShelf);
        // No parent rack or coordinate

        // When: Building hierarchical path
        String path = storageLocationService.buildHierarchicalPath(testPosition);

        // Then: Should return "Room > Device > Shelf"
        assertNotNull("Path should not be null", path);
        assertEquals("Main Laboratory > Freezer Unit 1 > Shelf-A", path);
    }

    /**
     * Test building path for rack-level position (4 levels: Room > Device > Shelf >
     * Rack)
     */
    @Test
    public void testBuildPath_RackLevel_ReturnsRoomDeviceShelfRack() {
        // Given: Position at rack level (device + shelf + rack, no coordinate)
        testPosition = new StoragePosition();
        testPosition.setId(40);
        testPosition.setFhirUuid(UUID.randomUUID());
        testPosition.setParentDevice(testDevice);
        testPosition.setParentShelf(testShelf);
        testPosition.setParentRack(testRack);
        // No coordinate

        // When: Building hierarchical path
        String path = storageLocationService.buildHierarchicalPath(testPosition);

        // Then: Should return "Room > Device > Shelf > Rack"
        assertNotNull("Path should not be null", path);
        assertEquals("Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1", path);
    }

    /**
     * Test building path for position-level position (5 levels: Room > Device >
     * Shelf > Rack > Position)
     */
    @Test
    public void testBuildPath_PositionLevel_ReturnsFullHierarchy() {
        // Given: Position at position level (full hierarchy with coordinate)
        testPosition = new StoragePosition();
        testPosition.setId(40);
        testPosition.setFhirUuid(UUID.randomUUID());
        testPosition.setParentDevice(testDevice);
        testPosition.setParentShelf(testShelf);
        testPosition.setParentRack(testRack);
        testPosition.setCoordinate("A5");

        // When: Building hierarchical path
        String path = storageLocationService.buildHierarchicalPath(testPosition);

        // Then: Should return "Room > Device > Shelf > Rack > Position {coordinate}"
        assertNotNull("Path should not be null", path);
        assertEquals("Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5", path);
    }

    /**
     * Test building path for null position - should return "Unknown Location"
     */
    @Test
    public void testBuildPath_NullPosition_ReturnsUnknown() {
        // Given: Null position
        // When: Building hierarchical path
        String path = storageLocationService.buildHierarchicalPath(null);

        // Then: Should return "Unknown Location"
        assertNotNull("Path should not be null", path);
        assertEquals("Unknown Location", path);
    }
}
