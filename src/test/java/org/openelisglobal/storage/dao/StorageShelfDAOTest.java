package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageShelfDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageShelfDAO storageShelfDAO;

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf1;
    private StorageShelf testShelf2;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        cleanRowsInCurrentConnection(
                new String[] { "storage_rack", "storage_shelf", "storage_device", "storage_room" });
        String uniqueCode = java.util.UUID.randomUUID().toString().substring(0, 6);

        testRoom = new StorageRoom();
        testRoom.setName("Test Room");
        testRoom.setCode("RM" + uniqueCode);
        testRoom.setActive(true);
        testRoom.setSysUserId("1");
        Integer testRoomId = storageRoomDAO.insert(testRoom);
        testRoom = storageRoomDAO.get(testRoomId).orElse(null);

        testDevice = new StorageDevice();
        testDevice.setName("Test Device");
        testDevice.setCode("DV" + uniqueCode);
        testDevice.setType("freezer");
        testDevice.setParentRoom(testRoom);
        testDevice.setActive(true);
        testDevice.setSysUserId("1");
        Integer testDeviceId = storageDeviceDAO.insert(testDevice);
        testDevice = storageDeviceDAO.get(testDeviceId).orElse(null);

        testShelf1 = new StorageShelf();
        testShelf1.setLabel("Shelf A");
        testShelf1.setCode("S1" + uniqueCode);
        testShelf1.setParentDevice(testDevice);
        testShelf1.setActive(true);
        testShelf1.setSysUserId("1");
        Integer testShelf1Id = storageShelfDAO.insert(testShelf1);
        testShelf1 = storageShelfDAO.get(testShelf1Id).orElse(null);

        testShelf2 = new StorageShelf();
        testShelf2.setLabel("Shelf B");
        testShelf2.setCode("S2" + uniqueCode);
        testShelf2.setParentDevice(testDevice);
        testShelf2.setActive(true);
        testShelf2.setSysUserId("1");
        Integer testShelf2Id = storageShelfDAO.insert(testShelf2);
        testShelf2 = storageShelfDAO.get(testShelf2Id).orElse(null);
    }

    @Test
    public void testFindByLabel_WithValidLabel_ReturnsShelf() {
        StorageShelf result = storageShelfDAO.findByLabel("Shelf A");
        assertNotNull("Shelf should be found", result);
        assertEquals("Label should match", "Shelf A", result.getLabel());
        assertEquals("Code should match", testShelf1.getCode(), result.getCode());
    }

    @Test
    public void testFindByLabel_WithInvalidLabel_ReturnsNull() {
        StorageShelf result = storageShelfDAO.findByLabel("NONEXISTENT");
        assertNull("Shelf should not be found", result);
    }

    @Test
    public void testFindByParentDeviceId_WithValidDevice_ReturnsShelves() {
        List<StorageShelf> results = storageShelfDAO.findByParentDeviceId(testDevice.getId());
        assertEquals("Should return two shelves", 2, results.size());
        assertTrue("Should contain shelf 1", results.stream().anyMatch(s -> s.getLabel().equals("Shelf A")));
        assertTrue("Should contain shelf 2", results.stream().anyMatch(s -> s.getLabel().equals("Shelf B")));
    }

    @Test
    public void testFindByLabelAndParentDevice_WithValidParams_ReturnsShelf() {
        StorageShelf result = storageShelfDAO.findByLabelAndParentDevice("Shelf A", testDevice);
        assertNotNull("Shelf should be found", result);
        assertEquals("Label should match", "Shelf A", result.getLabel());
    }

    @Test
    public void testCountByDeviceId_WithValidDevice_ReturnsCount() {
        int count = storageShelfDAO.countByDeviceId(testDevice.getId());
        assertEquals("Should return count of 2", 2, count);
    }

    @Test
    public void testGetAll_ReturnsAllShelves() {
        List<StorageShelf> results = storageShelfDAO.getAll();
        assertTrue("Should return at least 2 shelves", results.size() >= 2);
        assertTrue("Should contain shelf 1", results.stream().anyMatch(s -> s.getLabel().equals("Shelf A")));
        assertTrue("Should contain shelf 2", results.stream().anyMatch(s -> s.getLabel().equals("Shelf B")));
    }
}
