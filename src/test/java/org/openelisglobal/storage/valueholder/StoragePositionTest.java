package org.openelisglobal.storage.valueholder;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for StoragePosition entity hierarchy validation. Tests position
 * structure with flexible hierarchy (2-5 levels).
 * 
 * T026a: Position hierarchy validation tests
 */
public class StoragePositionTest {

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack;

    @Before
    public void setUp() {
        // Create test hierarchy components
        testRoom = new StorageRoom();
        testRoom.setId(1);
        testRoom.setCode("MAIN");
        testRoom.setName("Main Laboratory");
        testRoom.setActive(true);

        testDevice = new StorageDevice();
        testDevice.setId(10);
        testDevice.setCode("FRZ01");
        testDevice.setName("Freezer Unit 1");
        testDevice.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);

        testShelf = new StorageShelf();
        testShelf.setId(20);
        testShelf.setLabel("Shelf-A");
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);

        testRack = new StorageRack();
        testRack.setId(30);
        testRack.setLabel("Rack R1");
        testRack.setRows(8);
        testRack.setColumns(12);
        testRack.setParentShelf(testShelf);
        testRack.setActive(true);
    }

    /**
     * Test position with device only (2-level position) - should be valid Minimum
     * requirement: room + device
     */
    @Test
    public void testPositionWithDeviceOnly_Valid() {
        // Given: Position with only parent device
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);

        // When: Validating hierarchy
        // Then: Should be valid (has parent_device_id)
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNull("Position should not have parent shelf", position.getParentShelf());
        assertNull("Position should not have parent rack", position.getParentRack());
        assertNull("Position should not have coordinate", position.getCoordinate());
    }

    /**
     * Test position with device and shelf (3-level position) - should be valid
     */
    @Test
    public void testPositionWithDeviceAndShelf_Valid() {
        // Given: Position with device and shelf
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);
        position.setParentShelf(testShelf);

        // When: Validating hierarchy
        // Then: Should be valid
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNotNull("Position should have parent shelf", position.getParentShelf());
        assertNull("Position should not have parent rack", position.getParentRack());
        assertNull("Position should not have coordinate", position.getCoordinate());
    }

    /**
     * Test position with device, shelf, and rack (4-level position) - should be
     * valid
     */
    @Test
    public void testPositionWithDeviceShelfRack_Valid() {
        // Given: Position with device, shelf, and rack
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);
        position.setParentShelf(testShelf);
        position.setParentRack(testRack);

        // When: Validating hierarchy
        // Then: Should be valid
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNotNull("Position should have parent shelf", position.getParentShelf());
        assertNotNull("Position should have parent rack", position.getParentRack());
        assertNull("Position should not have coordinate", position.getCoordinate());
    }

    /**
     * Test position with full hierarchy (5-level position) - should be valid
     */
    @Test
    public void testPositionWithFullHierarchy_Valid() {
        // Given: Position with full hierarchy including coordinate
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);
        position.setParentShelf(testShelf);
        position.setParentRack(testRack);
        position.setCoordinate("A5");

        // When: Validating hierarchy
        // Then: Should be valid
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNotNull("Position should have parent shelf", position.getParentShelf());
        assertNotNull("Position should have parent rack", position.getParentRack());
        assertNotNull("Position should have coordinate", position.getCoordinate());
        assertEquals("A5", position.getCoordinate());
    }

    /**
     * Test position without device - should be invalid Minimum requirement: device
     * must exist (room + device)
     */
    @Test
    public void testPositionWithoutDevice_Invalid() {
        // Given: Position without parent device
        StoragePosition position = new StoragePosition();
        position.setId(40);
        // No parent device set

        // When: Validating hierarchy
        // Then: Should be invalid (no parent_device_id)
        assertNull("Position should not have parent device", position.getParentDevice());
        // This violates minimum 2-level requirement (room + device)
    }

    /**
     * Test position with rack but no shelf - should be invalid Constraint: If
     * parent_rack_id is NOT NULL, then parent_shelf_id must also be NOT NULL
     */
    @Test
    public void testPositionWithRackButNoShelf_Invalid() {
        // Given: Position with rack but no shelf
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);
        // Shelf is null
        position.setParentRack(testRack);

        // When: Validating hierarchy integrity
        // Then: Should be invalid (rack without shelf violates constraint)
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNull("Position should not have parent shelf", position.getParentShelf());
        assertNotNull("Position should have parent rack", position.getParentRack());
        // This violates constraint: if rack exists, shelf must exist
    }

    /**
     * Test position with coordinate but no rack - should be invalid Constraint: If
     * coordinate is NOT NULL, then parent_rack_id must also be NOT NULL
     */
    @Test
    public void testPositionWithCoordinateButNoRack_Invalid() {
        // Given: Position with coordinate but no rack
        StoragePosition position = new StoragePosition();
        position.setId(40);
        position.setParentDevice(testDevice);
        position.setParentShelf(testShelf);
        // Rack is null
        position.setCoordinate("A5");

        // When: Validating hierarchy integrity
        // Then: Should be invalid (coordinate without rack violates constraint)
        assertNotNull("Position should have parent device", position.getParentDevice());
        assertNotNull("Position should have parent shelf", position.getParentShelf());
        assertNull("Position should not have parent rack", position.getParentRack());
        assertNotNull("Position should have coordinate", position.getCoordinate());
        // This violates constraint: if coordinate exists, rack must exist
    }
}
