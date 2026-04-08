package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageRackDAOTest extends BaseWebContextSensitiveTest {
    @Autowired
    private StorageRackDAO storageRackDAO;
    @Autowired
    private StorageShelfDAO storageShelfDAO;
    @Autowired
    private StorageDeviceDAO storageDeviceDAO;
    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private StorageRoom testRoom;
    private StorageDevice testDevice;
    private StorageShelf testShelf;
    private StorageRack testRack1;
    private StorageRack testRack2;

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

        testShelf = new StorageShelf();
        testShelf.setLabel("Test Shelf");
        testShelf.setCode("SH" + uniqueCode);
        testShelf.setParentDevice(testDevice);
        testShelf.setActive(true);
        testShelf.setSysUserId("1");
        Integer testShelfId = storageShelfDAO.insert(testShelf);
        testShelf = storageShelfDAO.get(testShelfId).orElse(null);

        testRack1 = new StorageRack();
        testRack1.setLabel("Rack 1");
        testRack1.setCode("R1" + uniqueCode);
        testRack1.setParentShelf(testShelf);
        testRack1.setActive(true);
        testRack1.setSysUserId("1");
        Integer testRack1Id = storageRackDAO.insert(testRack1);
        testRack1 = storageRackDAO.get(testRack1Id).orElse(null);

        testRack2 = new StorageRack();
        testRack2.setLabel("Rack 2");
        testRack2.setCode("R2" + uniqueCode);
        testRack2.setParentShelf(testShelf);
        testRack2.setActive(true);
        testRack2.setSysUserId("1");
        Integer testRack2Id = storageRackDAO.insert(testRack2);
        testRack2 = storageRackDAO.get(testRack2Id).orElse(null);
    }

    @Test
    public void testFindByLabel_WithValidLabel_ReturnsRack() {
        StorageRack result = storageRackDAO.findByLabel("Rack 1");
        assertNotNull("Rack should be found", result);
        assertEquals("Label should match", "Rack 1", result.getLabel());
    }

    @Test
    public void testFindByLabel_WithInvalidLabel_ReturnsNull() {
        StorageRack result = storageRackDAO.findByLabel("NONEXISTENT");
        assertNull("Rack should not be found", result);
    }

    @Test
    public void testFindByParentShelfId_WithValidShelf_ReturnsRacks() {
        List<StorageRack> results = storageRackDAO.findByParentShelfId(testShelf.getId());
        assertEquals("Should return two racks", 2, results.size());
        assertTrue("Should contain rack 1", results.stream().anyMatch(r -> r.getLabel().equals("Rack 1")));
        assertTrue("Should contain rack 2", results.stream().anyMatch(r -> r.getLabel().equals("Rack 2")));
    }

    @Test
    public void testFindByLabelAndParentShelf_WithValidParams_ReturnsRack() {
        StorageRack result = storageRackDAO.findByLabelAndParentShelf("Rack 1", testShelf);
        assertNotNull("Rack should be found", result);
        assertEquals("Label should match", "Rack 1", result.getLabel());
    }

    @Test
    public void testCountByShelfId_WithValidShelf_ReturnsCount() {
        int count = storageRackDAO.countByShelfId(testShelf.getId());
        assertEquals("Should return count of 2", 2, count);
    }

    @Test
    public void testGetAll_ReturnsAllRacks() {
        List<StorageRack> results = storageRackDAO.getAll();
        assertTrue("Should return at least 2 racks", results.size() >= 2);
        assertTrue("Should contain rack 1", results.stream().anyMatch(r -> r.getLabel().equals("Rack 1")));
        assertTrue("Should contain rack 2", results.stream().anyMatch(r -> r.getLabel().equals("Rack 2")));
    }
}
