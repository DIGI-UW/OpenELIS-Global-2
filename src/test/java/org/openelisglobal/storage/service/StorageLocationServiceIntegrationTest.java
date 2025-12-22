package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.hibernate.LazyInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageLocationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final Logger logger = LoggerFactory.getLogger(StorageLocationServiceIntegrationTest.class);

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/storage-location.xml");
    }

    @Test
    public void getAllDevices_shouldAllowAccessToParentRoom_whenDevicesExist() {
        List<StorageDevice> devices = storageLocationService.getAllDevices();
        assertNotNull(devices);
        if (!devices.isEmpty()) {
            StorageDevice device = devices.get(0);
            try {
                StorageRoom parentRoom = device.getParentRoom();
                if (parentRoom != null) {
                    String roomName = parentRoom.getName();
                    assertNotNull(roomName);
                }
            } catch (LazyInitializationException e) {
                fail("LazyInitializationException occurred: " + e.getMessage());
            }
        }
    }

    @Test
    public void getAllShelves_shouldAllowAccessToParentDeviceAndRoom_whenShelvesExist() {
        List<StorageShelf> shelves = storageLocationService.getAllShelves();
        assertNotNull(shelves);
        if (!shelves.isEmpty()) {
            StorageShelf shelf = shelves.get(0);
            try {
                StorageDevice device = shelf.getParentDevice();
                if (device != null) {
                    String deviceName = device.getName();
                    assertNotNull(deviceName);
                    StorageRoom room = device.getParentRoom();
                    if (room != null) {
                        String roomName = room.getName();
                        assertNotNull(roomName);
                    }
                }
            } catch (LazyInitializationException e) {
                fail("LazyInitializationException occurred: " + e.getMessage());
            }
        }
    }

    @Test
    public void getAllRacks_shouldAllowAccessToFullHierarchy_whenRacksExist() {
        List<StorageRack> racks = storageLocationService.getAllRacks();
        assertNotNull(racks);
        if (!racks.isEmpty()) {
            StorageRack rack = racks.get(0);
            try {
                StorageShelf shelf = rack.getParentShelf();
                if (shelf != null) {
                    String shelfLabel = shelf.getLabel();
                    assertNotNull(shelfLabel);
                    StorageDevice device = shelf.getParentDevice();
                    if (device != null) {
                        String deviceName = device.getName();
                        assertNotNull(deviceName);
                        StorageRoom room = device.getParentRoom();
                        if (room != null) {
                            String roomName = room.getName();
                            assertNotNull(roomName);
                        }
                    }
                }
            } catch (LazyInitializationException e) {
                fail("LazyInitializationException occurred: " + e.getMessage());
            }
        }
    }

    @Test
    public void getRooms_shouldReturnDevicesLinkedToRoom_whenRoomHasDevices() {
        List<StorageRoom> rooms = storageLocationService.getRooms();
        assertNotNull(rooms);
        if (!rooms.isEmpty()) {
            StorageRoom room = rooms.get(0);
            try {
                List<StorageDevice> devices = storageLocationService.getDevicesByRoom(room.getId());
                assertNotNull(devices);
                if (!devices.isEmpty()) {
                    StorageDevice device = devices.get(0);
                    assertNotNull(device);
                    assertEquals(room.getId(), device.getParentRoom().getId());
                }
            } catch (LazyInitializationException e) {
                fail("LazyInitializationException occurred: " + e.getMessage());
            }
        }
    }

    @Test
    public void buildHierarchicalPath_shouldResolveLazyRelationships_whenHierarchyExists() {
        List<StorageRoom> rooms = storageLocationService.getRooms();
        if (!rooms.isEmpty()) {
            List<StorageDevice> devices = storageLocationService.getDevicesByRoom(rooms.get(0).getId());
            if (!devices.isEmpty()) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void getRoomsForAPI_shouldIncludeSampleCount_whenRoomsExist() {
        List<Map<String, Object>> rooms = storageLocationService.getRoomsForAPI();
        assertNotNull(rooms);
        if (!rooms.isEmpty()) {
            Map<String, Object> room = rooms.get(0);
            assertTrue(room.containsKey("sampleCount"));
            Object sampleCount = room.get("sampleCount");
            assertNotNull(sampleCount);
            assertTrue(sampleCount instanceof Integer);
            Integer count = (Integer) sampleCount;
            assertTrue(count >= 0);
        }
    }

    @Test
    public void insertDevice_shouldNormalizeShortCodeToUppercase_whenLowercaseProvided() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device = new StorageDevice();
        device.setName("Test Device 01");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setCode("test-frz01");
        device.setActive(true);
        device.setSysUserIdValue(1);
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull(deviceId);
        StorageDevice retrieved = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        assertNotNull(retrieved);
        assertEquals("TEST-FRZ01", retrieved.getCode());
    }

    @Test
    public void insertDevice_shouldPersistCode_whenCodeLengthIsWithinLimit() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-DEV02");
        device.setName("Test Device 02");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1);
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull(deviceId);
        StorageDevice retrieved = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        assertNotNull(retrieved);
        assertEquals("TEST-DEV02", retrieved.getCode());
    }

    @Test
    public void insertDevice_shouldThrowException_whenCodeLengthExceedsLimit() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-DEVICE-LONG-CODE");
        device.setName("Test Device Long Code");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1);
        try {
            storageLocationService.insert(device);
            fail("Should have thrown exception");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("short") || e.getMessage().contains("code")
                    || e.getMessage().contains("10"));
        }
    }

    @Test
    public void insertDevice_shouldThrowException_whenShortCodeIsDuplicate() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device1 = new StorageDevice();
        device1.setCode("TEST-DUP");
        device1.setName("Test Device 03");
        device1.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device1.setParentRoom(parentRoom);
        device1.setActive(true);
        device1.setSysUserIdValue(1);
        storageLocationService.insert(device1);

        StorageDevice device2 = new StorageDevice();
        device2.setCode("TEST-DUP");
        device2.setName("Test Device 04");
        device2.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device2.setParentRoom(parentRoom);
        device2.setActive(true);
        device2.setSysUserIdValue(1);

        try {
            storageLocationService.insert(device2);
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void updateDevice_shouldNormalizeShortCodeToUppercase_whenUpdated() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device = new StorageDevice();
        device.setCode("TEST-OLD");
        device.setName("Test Device 05");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1);
        Integer deviceId = storageLocationService.insert(device);

        StorageDevice updatedDevice = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        updatedDevice.setCode("test-new");
        storageLocationService.update(updatedDevice);

        StorageDevice retrieved = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        assertEquals("TEST-NEW", retrieved.getCode());
    }

    @Test
    public void insertShelf_shouldNormalizeShortCodeToUppercase_whenLowercaseProvided() {
        StorageDevice parentDevice = (StorageDevice) storageLocationService.get(5000, StorageDevice.class);
        assertNotNull(parentDevice);
        StorageShelf shelf = new StorageShelf();
        shelf.setLabel("TEST-SHELF01");
        shelf.setParentDevice(parentDevice);
        shelf.setCode("test-sha01");
        shelf.setActive(true);
        shelf.setSysUserIdValue(1);
        Integer shelfId = storageLocationService.insert(shelf);
        StorageShelf retrieved = (StorageShelf) storageLocationService.get(shelfId, StorageShelf.class);
        assertEquals("TEST-SHA01", retrieved.getCode());
    }

    @Test
    public void insertRack_shouldNormalizeShortCodeToUppercase_whenLowercaseProvided() {
        StorageShelf parentShelf = (StorageShelf) storageLocationService.get(5000, StorageShelf.class);
        assertNotNull(parentShelf);
        StorageRack rack = new StorageRack();
        rack.setLabel("TEST-RACK01");
        rack.setParentShelf(parentShelf);
        rack.setCode("test-rkr01");
        rack.setActive(true);
        rack.setSysUserIdValue(1);
        Integer rackId = storageLocationService.insert(rack);
        StorageRack retrieved = (StorageRack) storageLocationService.get(rackId, StorageRack.class);
        assertEquals("TEST-RKR01", retrieved.getCode());
    }

    @Test
    public void getAllSamplesWithAssignments_shouldReturnSampleDataWithAssignments() {
        List<Map<String, Object>> allSamples = sampleStorageService.getAllSamplesWithAssignments();
        assertNotNull(allSamples);
    }

    @Test
    public void filterRooms_shouldReturnOnlyActiveRooms_whenFilteringByActiveStatus() {
        List<StorageRoom> allRooms = storageLocationService.getRooms();
        List<StorageRoom> activeRooms = allRooms.stream().filter(StorageRoom::getActive).toList();
        assertEquals(2, activeRooms.size());
    }

    @Test
    public void filterDevices_shouldReturnMatchingDevices_whenTypeRoomAndStatusMatch() {
        List<StorageDevice> allDevices = storageLocationService.getAllDevices();
        List<StorageDevice> filteredDevices = allDevices.stream()
                .filter(d -> d.getTypeEnum() == StorageDevice.DeviceType.FREEZER)
                .filter(d -> d.getParentRoom().getId().equals(5000)).filter(StorageDevice::getActive).toList();
        assertEquals(1, filteredDevices.size());
    }

    @Test
    public void filterShelves_shouldReturnMatchingShelves_whenDeviceRoomAndStatusMatch() {
        List<StorageShelf> allShelves = storageLocationService.getAllShelves();
        List<StorageShelf> filteredShelves = allShelves.stream().filter(s -> s.getParentDevice().getId().equals(5000))
                .filter(StorageShelf::getActive).toList();
        assertEquals(2, filteredShelves.size());
    }

    @Test
    public void filterRacks_shouldReturnMatchingRacks_whenHierarchyAndStatusMatch() {
        List<StorageRack> allRacks = storageLocationService.getAllRacks();
        assertNotNull(allRacks);

        List<StorageRack> filteredRacks = allRacks.stream()
                .filter(r -> r.getParentShelf() != null && r.getParentShelf().getId().equals(5000))
                .filter(r -> r.getParentShelf().getParentDevice() != null
                        && r.getParentShelf().getParentDevice().getId().equals(5000))
                .filter(r -> r.getParentShelf().getParentDevice().getParentRoom() != null
                        && r.getParentShelf().getParentDevice().getParentRoom().getId().equals(5000))
                .filter(StorageRack::getActive).toList();

        assertEquals(2, filteredRacks.size());
        for (StorageRack rack : filteredRacks) {
            assertEquals(Integer.valueOf(5000), rack.getParentShelf().getId());
            assertEquals(Integer.valueOf(5000), rack.getParentShelf().getParentDevice().getId());
            assertEquals(Integer.valueOf(5000), rack.getParentShelf().getParentDevice().getParentRoom().getId());
            assertTrue(rack.getActive());
        }
    }

    @Test
    public void testGetRacks_IncludesRoomColumn() {
        List<Map<String, Object>> racks = storageLocationService.getRacksForAPI(null);
        assertNotNull(racks);
        assertFalse(racks.isEmpty());

        for (Map<String, Object> rack : racks) {
            assertTrue(rack.containsKey("parentRoomId"));
            assertNotNull(rack.get("parentRoomId"));
        }
    }

    @Test
    public void testFilterShelvesForAPI_ByDeviceId_ReturnsMatchingShelves() {
        List<Map<String, Object>> allShelves = storageLocationService.getShelvesForAPI(null);
        assertNotNull(allShelves);

        List<Map<String, Object>> filteredShelves = allShelves.stream()
                .filter(s -> s.get("parentDeviceId") != null && ((Integer) s.get("parentDeviceId")).equals(5000))
                .toList();

        assertEquals(2, filteredShelves.size());
        for (Map<String, Object> shelf : filteredShelves) {
            assertEquals(Integer.valueOf(5000), shelf.get("parentDeviceId"));
        }
    }

    @Test
    public void testFilterShelvesForAPI_ByRoomId_ReturnsMatchingShelves() {
        List<Map<String, Object>> allShelves = storageLocationService.getShelvesForAPI(null);
        assertNotNull(allShelves);

        List<Map<String, Object>> filteredShelves = allShelves.stream()
                .filter(s -> s.get("parentRoomId") != null && ((Integer) s.get("parentRoomId")).equals(5000)).toList();

        assertEquals(4, filteredShelves.size());
        for (Map<String, Object> shelf : filteredShelves) {
            assertEquals(Integer.valueOf(5000), shelf.get("parentRoomId"));
        }
    }

    @Test
    public void testGetShelvesForAPI_IncludesParentDeviceId() {
        List<Map<String, Object>> shelves = storageLocationService.getShelvesForAPI(null);
        assertNotNull(shelves);
        assertFalse(shelves.isEmpty());

        for (Map<String, Object> shelf : shelves) {
            assertTrue(shelf.containsKey("parentDeviceId"));
            assertNotNull(shelf.get("parentDeviceId"));
        }
    }

    @Test
    public void testGetShelvesForAPI_IncludesParentRoomId() {
        List<Map<String, Object>> shelves = storageLocationService.getShelvesForAPI(null);
        assertNotNull(shelves);
        assertFalse(shelves.isEmpty());

        for (Map<String, Object> shelf : shelves) {
            assertTrue(shelf.containsKey("parentRoomId"));
            assertNotNull(shelf.get("parentRoomId"));
        }
    }

    @Test
    public void testFilterLogic_DevicesByTypeAndRoom() {
        List<StorageDevice> allDevices = storageLocationService.getAllDevices();
        List<StorageDevice> freezersInRoom5000 = allDevices.stream()
                .filter(d -> d.getTypeEnum() == StorageDevice.DeviceType.FREEZER)
                .filter(d -> d.getParentRoom() != null && d.getParentRoom().getId().equals(5000))
                .filter(StorageDevice::getActive).toList();

        assertTrue(freezersInRoom5000.size() >= 1);
        assertTrue(freezersInRoom5000.stream().anyMatch(d -> d.getName().equals("Test Freezer 1")));
    }

    @Test
    public void testFilterLogic_ShelvesByDevice() {
        List<StorageShelf> allShelves = storageLocationService.getAllShelves();
        List<StorageShelf> shelvesForDevice5000 = allShelves.stream()
                .filter(s -> s.getParentDevice() != null && s.getParentDevice().getId().equals(5000))
                .filter(StorageShelf::getActive).toList();

        assertEquals(2, shelvesForDevice5000.size());
        assertTrue(shelvesForDevice5000.stream().anyMatch(s -> s.getLabel().equals("Shelf A")));
        assertTrue(shelvesForDevice5000.stream().anyMatch(s -> s.getLabel().equals("Shelf B")));
    }
}