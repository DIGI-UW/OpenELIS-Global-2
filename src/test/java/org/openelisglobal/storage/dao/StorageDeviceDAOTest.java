package org.openelisglobal.storage.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageDeviceDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageDeviceDAO storageDeviceDAO;

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    private StorageRoom testRoom;
    private StorageDevice testDevice1;
    private StorageDevice testDevice2;

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

        testDevice1 = new StorageDevice();
        testDevice1.setName("Test Device 1");
        testDevice1.setCode("D1" + uniqueCode);
        testDevice1.setType("freezer");
        testDevice1.setParentRoom(testRoom);
        testDevice1.setActive(true);
        testDevice1.setSysUserId("1");
        Integer testDevice1Id = storageDeviceDAO.insert(testDevice1);
        testDevice1 = storageDeviceDAO.get(testDevice1Id).orElse(null);

        testDevice2 = new StorageDevice();
        testDevice2.setName("Test Device 2");
        testDevice2.setCode("D2" + uniqueCode);
        testDevice2.setType("refrigerator");
        testDevice2.setParentRoom(testRoom);
        testDevice2.setActive(true);
        testDevice2.setSysUserId("1");
        Integer testDevice2Id = storageDeviceDAO.insert(testDevice2);
        testDevice2 = storageDeviceDAO.get(testDevice2Id).orElse(null);
    }

    @Test
    public void testFindByCode_WithValidCode_ReturnsDevice() {
        StorageDevice result = storageDeviceDAO.findByCode(testDevice1.getCode());
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", testDevice1.getCode(), result.getCode());
        assertEquals("Name should match", "Test Device 1", result.getName());
    }

    @Test
    public void testFindByCode_WithInvalidCode_ReturnsNull() {
        StorageDevice result = storageDeviceDAO.findByCode("NONEXISTENT");
        assertNull("Device should not be found", result);
    }

    @Test
    public void testFindByParentRoomId_WithValidRoom_ReturnsDevices() {
        List<StorageDevice> results = storageDeviceDAO.findByParentRoomId(testRoom.getId());
        assertEquals("Should return two devices", 2, results.size());
        assertTrue("Should contain device 1",
                results.stream().anyMatch(d -> d.getCode().equals(testDevice1.getCode())));
        assertTrue("Should contain device 2",
                results.stream().anyMatch(d -> d.getCode().equals(testDevice2.getCode())));
    }

    @Test
    public void testFindByParentRoomIdAndCode_WithValidParams_ReturnsDevice() {
        StorageDevice result = storageDeviceDAO.findByParentRoomIdAndCode(testRoom.getId(), testDevice1.getCode());
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", testDevice1.getCode(), result.getCode());
    }

    @Test
    public void testFindByCodeAndParentRoom_WithValidParams_ReturnsDevice() {
        StorageDevice result = storageDeviceDAO.findByCodeAndParentRoom(testDevice1.getCode(), testRoom);
        assertNotNull("Device should be found", result);
        assertEquals("Code should match", testDevice1.getCode(), result.getCode());
    }

    @Test
    public void testCountByRoomId_WithValidRoom_ReturnsCount() {
        int count = storageDeviceDAO.countByRoomId(testRoom.getId());
        assertEquals("Should return count of 2", 2, count);
    }

    @Test
    public void testGetAll_ReturnsAllDevices() {
        List<StorageDevice> results = storageDeviceDAO.getAll();
        assertTrue("Should return at least 2 devices", results.size() >= 2);
        assertTrue("Should contain device 1",
                results.stream().anyMatch(d -> d.getCode().equals(testDevice1.getCode())));
        assertTrue("Should contain device 2",
                results.stream().anyMatch(d -> d.getCode().equals(testDevice2.getCode())));
    }
}
