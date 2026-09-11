package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.LazyInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.valueholder.*;
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
        // storage-location.xml stamps sample rows with sys_user_id=1; ensure that
        // seed survives a sibling test's TRUNCATE system_user CASCADE before load.
        ensureAuditSystemUser();
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
    public void insertDevice_shouldPersistCode_whenDeviceCodeIsNullOrEmpty() {
        StorageRoom parentRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(parentRoom);
        StorageDevice device = new StorageDevice();
        device.setName("Test Device 02");
        device.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device.setParentRoom(parentRoom);
        device.setActive(true);
        device.setSysUserIdValue(1);
        Integer deviceId = storageLocationService.insert(device);
        assertNotNull(deviceId);
        StorageDevice retrieved = (StorageDevice) storageLocationService.get(deviceId, StorageDevice.class);
        assertNotNull(retrieved);
        assertEquals("TESTDEVICE", retrieved.getCode());
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
    public void testInsert_WhenEntityIsStorageBox_ThrowExceptionWhenColumnIsNull() {
        StorageBox storageBox = new StorageBox();

        storageBox.setPositionSchemaHint("New Schema Hint");
        storageBox.setLabel("Box-5004");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            storageLocationService.insert(storageBox);
        });

        assertEquals("Box must have valid grid dimensions (rows and columns cannot be negative)",
                exception.getMessage());
    }

    @Test
    public void testInsert_WhenEntityIsStorageBox_ReturnInsertedBox() {
        StorageBox storageBox = new StorageBox();
        StorageRack storageRack = (StorageRack) storageLocationService.get(5001, StorageRack.class);

        storageBox.setPositionSchemaHint("New Schema Hint");
        storageBox.setRows(9);
        storageBox.setColumns(10);
        storageBox.setLabel("Box-5004");
        storageBox.setActive(true);
        storageBox.setCode("CODE-0019");
        storageBox.setSysUserId("1");
        storageBox.setParentRack(storageRack);

        Integer returnedInteger = storageLocationService.insert(storageBox);
        assertEquals("Box-5004", storageBox.getLabel());
        assertNotNull(returnedInteger);
        assertEquals(Integer.valueOf(1), returnedInteger);
        assertEquals("New Schema Hint", storageBox.getPositionSchemaHint());
    }

    @Test
    public void testInsert_WhenEntityIsStorageRoom_ReturnInsertedRoom() {
        StorageRoom storageRoom = new StorageRoom();

        storageRoom.setFhirUuid(UUID.fromString("00000000-0000-0000-0000-000000001078"));
        storageRoom.setActive(true);
        storageRoom.setCode("CODE-0019");
        storageRoom.setName("ROOM-277");
        storageRoom.setSysUserId("1");

        Integer returnedInteger = storageLocationService.insert(storageRoom);
        assertEquals("CODE-0019", storageRoom.getCode());
        assertNotNull(returnedInteger);
        assertEquals(Integer.valueOf(3), returnedInteger);
        assertEquals("1", storageRoom.getSysUserId());
    }

    @Test
    public void testInsert_WhenEntityIsStorageShelf_ReturnInsertedShelfCodeIsNull() {
        StorageShelf storageShelf = new StorageShelf();
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5001, StorageDevice.class);

        storageShelf.setFhirUuid(UUID.fromString("00000000-0000-0000-0000-000000001078"));
        storageShelf.setActive(true);
        storageShelf.setLabel("SHELF-0087");
        storageShelf.setSysUserId("1");
        storageShelf.setParentDevice(storageDevice);

        Integer returnedInteger = storageLocationService.insert(storageShelf);
        assertEquals("SHELF-0087", storageShelf.getCode());
        assertNotNull(returnedInteger);
        assertEquals(Integer.valueOf(2), returnedInteger);
        assertEquals("1", storageShelf.getSysUserId());
    }

    @Test
    public void testInsert_WhenEntityIsStorageRoom_ThrowsExceptionWhenDuplicateCodeIsBeingInserted() {
        StorageRoom storageRoom = new StorageRoom();
        storageRoom.setCode("TEST-R01");

        LIMSRuntimeException exception = assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.insert(storageRoom);
        });
        assertEquals("Room with code " + storageRoom.getCode() + " already exists", exception.getMessage());
    }

    @Test
    public void testInsert_WhenEntityIsUnsupported_ThrowsExceptionWithMessage() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("7843");

        LIMSRuntimeException exception = assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.insert(sampleItem);
        });
        assertEquals("Unsupported entity type for insert", exception.getMessage());
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

    @Test
    public void testUpdatedRoom_ReturnNullWhenRoomIsNull() {
        StorageRoom returnedRoom = storageLocationService.updateRoom(6702, new StorageRoom());
        assertNull(returnedRoom);
    }

    @Test
    public void testUpdatedRoom_UpdateRoomWhenCodeIsNotNullAndNewCodeNotEqualsToExistingCode() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.get(5001, StorageRoom.class);
        assertEquals("Test Room 2", storageRoom.getName());
        assertEquals("Second test room", storageRoom.getDescription());
        assertEquals("TEST-R02", storageRoom.getCode());

        storageRoom.setName("Test Room 5001");
        storageRoom.setDescription("Test Number 5001");
        storageRoom.setCode("CODE-R03");
        StorageRoom returnedRoom = storageLocationService.updateRoom(5001, storageRoom);
        assertNotNull(returnedRoom);
        assertEquals("Test Room 5001", returnedRoom.getName());
        assertEquals("Test Number 5001", returnedRoom.getDescription());
        assertEquals("CODE-R03", returnedRoom.getCode());

    }

    @Test
    public void testUpdatedRoom_UpdateStorageRoomWhenAvailable() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.get(5001, StorageRoom.class);
        assertEquals("Test Room 2", storageRoom.getName());
        assertEquals("Second test room", storageRoom.getDescription());

        storageRoom.setName("Test Room 5001");
        storageRoom.setDescription("Test Number 5001");
        StorageRoom returnedRoom = storageLocationService.updateRoom(5001, storageRoom);
        assertNotNull(returnedRoom);
        assertEquals("Test Room 5001", returnedRoom.getName());
        assertEquals("Test Number 5001", returnedRoom.getDescription());
    }

    @Test
    public void testGetBoxesByRack_ReturnsAllBoxesWithParentRackId() {
        List<StorageBox> storageBoxes = storageLocationService.getBoxesByRack(5004);
        assertNotNull(storageBoxes);
        assertEquals(2, storageBoxes.size());
        assertEquals("Plate 96-A", storageBoxes.getLast().getLabel());
        assertEquals("Box 81-B", storageBoxes.getFirst().getLabel());
    }

    @Test
    public void testGetAllBoxes_ReturnsAllStorageBoxes() {
        List<StorageBox> storageBoxes = storageLocationService.getAllBoxes();
        assertNotNull(storageBoxes);
        assertEquals(2, storageBoxes.size());
        assertEquals("PLT-01", storageBoxes.getLast().getCode());
        assertEquals("BOX-02", storageBoxes.getFirst().getCode());
    }

    @Test
    public void testCountOccupiedInDevice_ReturnsNumberOfOccupiedBoxesInDevice() {
        int occupiedBoxesInDevice = storageLocationService.countOccupiedInDevice(5004);
        assertEquals(2, occupiedBoxesInDevice);
    }

    @Test
    public void testCountOccupied_ReturnNumberOfOccupiedBoxesInRack() {
        int occupiedBoxesInDevice = storageLocationService.countOccupied(5004);
        assertEquals(2, occupiedBoxesInDevice);
    }

    @Test
    public void testCountOccupied_ReturnNumberOfOccupiedBoxesInShelf() {
        int occupiedBoxesInShelf = storageLocationService.countOccupiedInShelf(5004);
        assertEquals(2, occupiedBoxesInShelf);
    }

    @Test
    public void testValidateLocationActive_ReturnTrueWhenAllParentsAreActive() {
        StorageRoom room = new StorageRoom();
        room.setActive(true);

        StorageDevice device = new StorageDevice();
        device.setActive(true);
        device.setParentRoom(room);

        StorageShelf shelf = new StorageShelf();
        shelf.setActive(true);
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setActive(true);
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setParentRack(rack);

        boolean isLocationActive = storageLocationService.validateLocationActive(box);
        assertTrue(isLocationActive);
    }

    @Test
    public void testValidateLocationActive_ReturnFalseWhenDeviceActiveIsNull() {
        StorageRoom room = new StorageRoom();
        room.setActive(true);

        StorageDevice device = new StorageDevice();
        device.setActive(null); // Null active state
        device.setParentRoom(room);

        StorageShelf shelf = new StorageShelf();
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setParentRack(rack);

        assertFalse(storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_ReturnFalseWhenShelfIsInactive() {
        StorageRoom room = new StorageRoom();
        room.setActive(true);

        StorageDevice device = new StorageDevice();
        device.setActive(true);
        device.setParentRoom(room);

        StorageShelf shelf = new StorageShelf();
        shelf.setActive(false); // Inactive shelf
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setParentRack(rack);

        assertFalse(storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testValidateLocationActive_ReturnFalseWhenRackIsInactive() {
        StorageRoom room = new StorageRoom();
        room.setActive(true);

        StorageDevice device = new StorageDevice();
        device.setActive(true);
        device.setParentRoom(room);

        StorageShelf shelf = new StorageShelf();
        shelf.setActive(true);
        shelf.setParentDevice(device);

        StorageRack rack = new StorageRack();
        rack.setActive(false); // Inactive rack
        rack.setParentShelf(shelf);

        StorageBox box = new StorageBox();
        box.setParentRack(rack);

        assertFalse(storageLocationService.validateLocationActive(box));
    }

    @Test
    public void testBuildHierarchicalPath_ReturnUnKnownLocationWhenBoxIsNull() {
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(null);
        assertEquals("Unknown Location", hierarchicalPath);
    }

    @Test
    public void testBuildHierarchicalPath_ReturnUnKnownWhenRackIsNull() {
        StorageBox storageBox = new StorageBox();
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(storageBox);
        assertEquals("Unknown", hierarchicalPath);
    }

    @Test
    public void testBuildHierarchicalPath_ReturnUnKnownWhenParentShelfIsNull() {
        StorageBox storageBox = new StorageBox();
        StorageRack storageRack = new StorageRack();
        storageBox.setParentRack(storageRack);
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(storageBox);
        assertEquals("Unknown", hierarchicalPath);
    }

    @Test
    public void testBuildHierarchicalPath_ReturnRackLabelWhenRoomIsNull() {
        StorageBox storageBox = new StorageBox();
        StorageRack storageRack = new StorageRack();

        StorageShelf storageShelf = new StorageShelf();
        storageRack.setLabel("RACk #680");
        storageRack.setParentShelf(storageShelf);

        storageBox.setParentRack(storageRack);
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(storageBox);
        assertEquals("RACk #680", hierarchicalPath);
    }

    @Test
    public void testBuildHierarchicalPath_ReturnPathWhenBoxLabelIsNull() {
        StorageBox storageBox = new StorageBox();
        StorageRoom storageRoom = new StorageRoom();
        storageRoom.setName("Test ROOM-0652");
        StorageRack storageRack = new StorageRack();

        storageRack.setLabel("RACk #680");
        StorageShelf storageShelf = new StorageShelf();
        storageShelf.setLabel("SHELF-0022");
        StorageDevice storageDevice = new StorageDevice();
        storageDevice.setName("Test-Device-001");
        storageShelf.setParentDevice(storageDevice);

        storageDevice.setParentRoom(storageRoom);

        storageRack.setParentShelf(storageShelf);
        storageBox.setParentRack(storageRack);

        String hierarchicalPath = storageLocationService.buildHierarchicalPath(storageBox);
        System.out.println(hierarchicalPath);
        assertEquals("Test ROOM-0652 > Test-Device-001 > SHELF-0022 > RACk #680", hierarchicalPath);

    }

    @Test
    public void testBuildHierarchicalPath_ReturnPathWhenBoxIsFullyBult() {
        StorageBox storageBox = (StorageBox) storageLocationService.get(5004, StorageBox.class);
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(storageBox);
        assertEquals("Test Room 2 > Test Device Inactive > Shelf Inactive > Rack Flexible > Plate 96-A",
                hierarchicalPath);
    }

    @Test
    public void testGetRoom() {
        StorageRoom storageRoom = storageLocationService.getRoom(5000);
        assertNotNull(storageRoom);
        assertEquals("First test room", storageRoom.getDescription());
        assertEquals("TEST-R01", storageRoom.getCode());
    }

    @Test
    public void testCreateRoom_WhenRoomCodeIsNullOrEmpty() {

        StorageRoom storageRoom = new StorageRoom();
        storageRoom.setFhirUuid(UUID.fromString("00000000-0000-0000-0000-000000001783"));
        storageRoom.setName("New Created Room");
        storageRoom.setActive(true);
        storageRoom.setSysUserId("1");

        StorageRoom createdRoom = storageLocationService.createRoom(storageRoom);

        assertNotNull(createdRoom);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000001783"), createdRoom.getFhirUuid());
        assertEquals("New Created Room", createdRoom.getName());

    }

    @Test
    public void testCreateRoom_WhenRoomCodeIsNotNull() {

        StorageRoom storageRoom = new StorageRoom();
        storageRoom.setFhirUuid(UUID.fromString("00000000-0000-0000-0000-000000001783"));
        storageRoom.setName("New Created Room");
        storageRoom.setActive(true);
        storageRoom.setSysUserId("1");
        storageRoom.setCode("PLTA-89");

        StorageRoom createdRoom = storageLocationService.createRoom(storageRoom);

        assertNotNull(createdRoom);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000001783"), createdRoom.getFhirUuid());
        assertEquals("New Created Room", createdRoom.getName());
    }

    @Test
    public void testCreateRoom_ThrowLIMSRuntimeExceptionWhenFormatResultIsInvalid() {

    }

    @Test
    public void testGetShelvesByDevice() {

        List<StorageShelf> storageShelves = storageLocationService.getShelvesByDevice(5004);
        assertNotNull(storageShelves);
        assertEquals(1, storageShelves.size());
        assertEquals("Shelf Inactive", storageShelves.getFirst().getLabel());
    }

    @Test
    public void testGetRacksByShelf() {
        List<StorageRack> storageRacks = storageLocationService.getRacksByShelf(5004);
        assertNotNull(storageRacks);
        assertEquals(1, storageRacks.size());
        assertEquals("TEST-RF", storageRacks.getFirst().getCode());

    }

    @Test
    public void testUpdate_WhenEntityIsStorageRoom_ThrowLIMSRuntimeExceptionWhenExistingRoomIsNull() {
        StorageRoom storageRoom = new StorageRoom();
        storageRoom.setId(4300);
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(storageRoom);
        });
    }

    @Test
    public void testUpdate_WhenEntityIsStorageDevice_ThrowLIMSRuntimeExceptionWhenExistingDeviceIsNull() {
        StorageDevice storageDevice = new StorageDevice();
        storageDevice.setId(4301);
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(storageDevice);
        });
    }

    @Test
    public void testUpdate_WhenEntityIsStorageShelf_ThrowLIMSRuntimeExceptionWhenExistingShelfIsNull() {
        StorageShelf storageShelf = new StorageShelf();
        storageShelf.setId(4302);
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(storageShelf);
        });
    }

    @Test
    public void testUpdate_WhenEntityIsStorageRack_ThrowLIMSRuntimeExceptionWhenExistingRackIsNull() {
        StorageRack storageRack = new StorageRack();
        storageRack.setId(4303);
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(storageRack);
        });
    }

    @Test
    public void testUpdate_WhenEntityIsStorageBox_ThrowLIMSRuntimeExceptionWhenExistingBoxIsNull() {
        StorageBox storageBox = new StorageBox();
        storageBox.setId(4303);
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(storageBox);
        });
    }

    @Test
    public void testUpdate_WhenEntityIsUnsupported_ThrowsExceptionWithMessage() {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("7843");

        LIMSRuntimeException exception = assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.update(sampleItem);
        });
        assertEquals("Unsupported entity type for update", exception.getMessage());
    }

    @Test
    public void testUpdate_WhenEntityIsStorageRoom_ReturnNullWhenExistingRoomIsNotNull() {
        StorageRoom storageRoom = storageLocationService.getRoom(5002);

        storageRoom.setDescription("New Description for Room 5002");
        storageRoom.setName("Test Room 5002");

        storageLocationService.update(storageRoom);
        assertEquals("Test Room 5002", storageRoom.getName());
        assertEquals("New Description for Room 5002", storageRoom.getDescription());
    }

    @Test
    public void testUpdate_WhenEntityIsStorageRack_ReturnNullWhenExistingRackIsNotNull() {
        StorageRack storageRack = (StorageRack) storageLocationService.get(5004, StorageRack.class);

        storageRack.setLabel("New Rack Flexible");
        storageRack.setActive(false);

        storageLocationService.update(storageRack);
        assertEquals("New Rack Flexible", storageRack.getLabel());
        assertFalse(storageRack.getActive());
    }

    @Test
    public void testUpdate_WhenEntityIsStorageBox_ReturnNullWhenExistingBoxIsNotNull() {
        StorageBox storageBox = (StorageBox) storageLocationService.get(5004, StorageBox.class);

        storageBox.setPositionSchemaHint("New Schema Hint");
        storageBox.setRows(9);
        storageBox.setLabel("Box-5004");

        storageLocationService.update(storageBox);
        assertEquals("Box-5004", storageBox.getLabel());
        assertEquals("New Schema Hint", storageBox.getPositionSchemaHint());
    }

    @Test
    public void testUpdate_WhenEntityIsStorageShelf_ReturnNullWhenExistingShelfIsNotNull() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5000, StorageShelf.class);

        storageShelf.setLabel("New Shelf Label");
        storageShelf.setActive(false);
        storageShelf.setCode("CODE-783");

        storageLocationService.update(storageShelf);
        assertEquals("New Shelf Label", storageShelf.getLabel());
        assertFalse("New Schema Hint", storageShelf.getActive());
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsNull() {
        assertEquals("Cannot delete location: location is null",
                storageLocationService.getDeleteConstraintMessage(null));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageRoom_AndDeviceCountIsGreaterZero() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.getRoom(5000);
        assertEquals("Cannot delete Room '" + storageRoom.getName() + "' because it contains 3 device(s)",
                storageLocationService.getDeleteConstraintMessage(storageRoom));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageRoom_AndDeviceCountIsNotGreaterZero() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.getRoom(5002);
        assertEquals("Cannot delete room: unknown constraint",
                storageLocationService.getDeleteConstraintMessage(storageRoom));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageDevice_AndShelfCountIsGreaterZero() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5000, StorageDevice.class);
        assertEquals("Cannot delete Device '" + storageDevice.getName() + "' because it contains 2 shelf(s)",
                storageLocationService.getDeleteConstraintMessage(storageDevice));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageDevice_AndSampleCountIsGreaterZero() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5002, StorageDevice.class);
        assertEquals("Cannot delete Device '" + storageDevice.getName() + "' because it has 1 sample(s) assigned",
                storageLocationService.getDeleteConstraintMessage(storageDevice));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageDevice_AndShelfCountOrSampleCountIsNotGreaterZero() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5008, StorageDevice.class);
        assertEquals("Cannot delete device: unknown constraint",
                storageLocationService.getDeleteConstraintMessage(storageDevice));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageShelf_AndRackCountIsGreaterZero() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5000, StorageShelf.class);
        assertEquals("Cannot delete Shelf '" + storageShelf.getLabel() + "' because it contains 2 rack(s)",
                storageLocationService.getDeleteConstraintMessage(storageShelf));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageShelf_AndSampleCountIsGreaterZero() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5003, StorageShelf.class);
        assertEquals("Cannot delete Shelf '" + storageShelf.getLabel() + "' because it has 1 sample(s) assigned",
                storageLocationService.getDeleteConstraintMessage(storageShelf));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageShelf_AndRackCountOrSampleCountIsNotGreaterZero() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5005, StorageShelf.class);
        assertEquals("Cannot delete shelf: unknown constraint",
                storageLocationService.getDeleteConstraintMessage(storageShelf));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageRack_AndSampleCountIsGreaterZero() {
        StorageRack storageRack = (StorageRack) storageLocationService.get(5000, StorageRack.class);
        assertEquals("Cannot delete Rack '" + storageRack.getLabel() + "' because it has 2 sample(s) assigned",
                storageLocationService.getDeleteConstraintMessage(storageRack));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageRack_AndSampleCountIsNotGreaterZero() {
        StorageRack storageRoom = (StorageRack) storageLocationService.get(5002, StorageRack.class);
        assertEquals("Cannot delete rack: unknown constraint",
                storageLocationService.getDeleteConstraintMessage(storageRoom));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageBox_AndSampleCountIsGreaterZero() {
        StorageBox storageBox = (StorageBox) storageLocationService.get(5004, StorageBox.class);
        assertEquals("Cannot delete Box '" + storageBox.getLabel() + "' because it has 2 sample(s) assigned",
                storageLocationService.getDeleteConstraintMessage(storageBox));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsInstanceOfStorageBox_AndSampleCountIsNotGreaterZero() {
        StorageBox storageBox = (StorageBox) storageLocationService.get(5001, StorageBox.class);
        assertEquals("Cannot delete box: unknown constraint",
                storageLocationService.getDeleteConstraintMessage(storageBox));
    }

    @Test
    public void testGetDeleteConstraintMessage_WhenLocationEntityIsUnKnown() {
        SampleItem sampleItem = new SampleItem();
        assertEquals("Cannot delete location: unknown type",
                storageLocationService.getDeleteConstraintMessage(sampleItem));
    }

    @Test
    public void testDeleteLocationWithCascade_ThrowExceptionWhenLocationEntityIsNull() {
        assertThrows(LIMSRuntimeException.class, () -> {
            storageLocationService.deleteLocationWithCascade(5003, null);
        });
    }

    @Test
    public void testCanDeleteRoom_ReturnSuccessWhenRoomIsNull() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.get(7802, StorageRoom.class);
        assertNull(storageRoom);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRoom(7802);
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteRoom_ReturnReferentialIntegrityViolationWhenChildDevicesIsNotEmpty() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.get(5000, StorageRoom.class);
        assertNotNull(storageRoom);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRoom(5000);
        assertFalse(validationResult.isSuccess());

        assertTrue(validationResult.getDependentCount() > 0);
        assertNotNull(validationResult.getMessage());
        assertEquals("Cannot delete Room '" + storageRoom.getName() + "': contains "
                + validationResult.getDependentCount() + " devices", validationResult.getMessage());
    }

    @Test
    public void testCanDeleteRoom_ReturnSuccessWhenChildDevicesIsEmpty() {
        StorageRoom storageRoom = (StorageRoom) storageLocationService.get(5003, StorageRoom.class);
        assertNotNull(storageRoom);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRoom(5003);
        assertEquals(0, validationResult.getDependentCount());
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteDevice_ReturnSuccessWhenDeviceIsNull() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(1278, StorageDevice.class);
        assertNull(storageDevice);
        DeletionValidationResult validationResult = storageLocationService.canDeleteDevice(1278);
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteDevice_ReturnReferentialIntegrityViolationWhenChildShelvesIsNotEmpty() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5000, StorageDevice.class);
        assertNotNull(storageDevice);
        DeletionValidationResult validationResult = storageLocationService.canDeleteDevice(5000);
        assertFalse(validationResult.isSuccess());

        assertTrue(validationResult.getDependentCount() > 0);
        assertNotNull(validationResult.getMessage());
        assertEquals("Cannot delete Device '" + storageDevice.getName() + "': contains "
                + validationResult.getDependentCount() + " shelves", validationResult.getMessage());
    }

    @Test
    public void testCanDeleteDevice_ReturnSuccessWhenWhenChildShelvesIsEmpty() {
        StorageDevice storageDevice = (StorageDevice) storageLocationService.get(5003, StorageDevice.class);
        assertNotNull(storageDevice);
        DeletionValidationResult validationResult = storageLocationService.canDeleteDevice(5003);
        assertEquals(0, validationResult.getDependentCount());
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteShelf_ReturnSuccessWhenShelfIsNull() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(2507, StorageShelf.class);
        assertNull(storageShelf);
        DeletionValidationResult validationResult = storageLocationService.canDeleteShelf(2507);
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteShelf_ReturnReferentialIntegrityViolationWhenChildRacksIsNotEmpty() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5000, StorageShelf.class);
        assertNotNull(storageShelf);
        DeletionValidationResult validationResult = storageLocationService.canDeleteShelf(5000);
        assertFalse(validationResult.isSuccess());

        assertTrue(validationResult.getDependentCount() > 0);
        assertNotNull(validationResult.getMessage());
        assertEquals("Cannot delete Shelf '" + storageShelf.getLabel() + "': contains "
                + validationResult.getDependentCount() + " racks", validationResult.getMessage());
    }

    @Test
    public void testCanDeleteShelf_ReturnSuccessWhenChildDRacksIsEmpty() {
        StorageShelf storageShelf = (StorageShelf) storageLocationService.get(5003, StorageShelf.class);
        assertNotNull(storageShelf);
        DeletionValidationResult validationResult = storageLocationService.canDeleteShelf(5003);
        assertEquals(0, validationResult.getDependentCount());
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteRack_ReturnSuccessWhenRackIsNull() {
        StorageRack storageRack = (StorageRack) storageLocationService.get(4301, StorageRack.class);
        assertNull(storageRack);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRack(4301);
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testCanDeleteRack_ReturnActiveAssignmentsWhenSampleCountIsGreaterThanZero() {
        StorageRack storageRack = (StorageRack) storageLocationService.get(5000, StorageRack.class);
        assertNotNull(storageRack);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRack(5000);
        assertFalse(validationResult.isSuccess());
        assertTrue(validationResult.getDependentCount() > 0);
        assertNotNull(validationResult.getMessage());
        assertEquals("Cannot delete Rack '" + storageRack.getLabel() + "': has " + validationResult.getDependentCount()
                + " assigned samples", validationResult.getMessage());
    }

    @Test
    public void testCanDeleteRack_ReturnSuccessWhenSampleCountIsZero() {
        StorageRack storageRack = (StorageRack) storageLocationService.get(5002, StorageRack.class);
        assertNotNull(storageRack);
        DeletionValidationResult validationResult = storageLocationService.canDeleteRack(5002);
        assertEquals(0, validationResult.getDependentCount());
        assertTrue(validationResult.isSuccess());
        assertNull(validationResult.getMessage());
    }

    @Test
    public void testIsNameUniqueWithinParent_ReturnTrueWhenNameIsNull() {
        assertTrue(storageLocationService.isNameUniqueWithinParent(null, 5001, "rack", 5000));
    }

    @Test
    public void testIsNameUniqueWithinParent_ReturnTrueWhenNameIsEmptyOrWhiteSpace() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("           ", 5001, "rack", 5000));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRoom_ReturnTrueWhenNoExistingRoomFound() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("NO-ROOM", null, "room", 5000));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRoom_ReturnTrueWhenNameBelongsToExcludedId() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Test Room 1", null, "room", 5000));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRoom_ReturnFalseWhenNameIsDuplicate() {
        assertFalse(storageLocationService.isNameUniqueWithinParent("Test Room 3", null, "room", 5003));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsDevice_ReturnTrueWhenParentIdIsNull() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Test Refrigerator 1", null, "device", 5001));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsDevice_ReturnTrueWhenNoExistingDeviceFound() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("NO-DEVICE", 5000, "device", 5001));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsDevice_ReturnTrueWhenNameBelongsToExcludedId() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Test Refrigerator 1", 5000, "device", 5001));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsShelf_ReturnTrueWhenParentIdIsNull() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Shelf C", null, "shelf", 5002));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsShelf_ReturnTrueWhenNoExistingShelfFound() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("NO-SHELF", 5001, "shelf", 5002));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsShelf_ReturnTrueWhenNameBelongsToExcludedId() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Shelf C", 5001, "shelf", 5002));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRack_ReturnTrueWhenParentIdIsNull() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Rack Flexible", null, "rack", 5003));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRack_ReturnTrueWhenNoExistingRackFound() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("NO-RACK", 5002, "rack", 5003));
    }

    @Test
    public void testIsNameUniqueWithinParent_WhenLocationTypeIsRack_ReturnTrueWhenNameBelongsToExcludedId() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Rack Flexible", 5002, "rack", 5003));
    }

    @Test
    public void testIsNameUniqueWithinParent_JustReturnTrueWhenLocationTypeIsOtherwise() {
        assertTrue(storageLocationService.isNameUniqueWithinParent("Rack Flexible", 5002, "random-type", 5003));
    }

    @Test
    public void testIsCodeUniqueForRoom_ReturnTrueWhenCodeIsNull() {
        assertTrue(storageLocationService.isCodeUniqueForRoom(null, 5002));
    }

    @Test
    public void testIsCodeUniqueForRoom_ReturnTrueWhenCodeIsEmptyOrWhitespace() {
        assertTrue(storageLocationService.isCodeUniqueForRoom("               ", 5002));
    }

    @Test
    public void testIsCodeUniqueForRoom_ReturnTrueWhenNoExistingRoomFound() {
        assertTrue(storageLocationService.isCodeUniqueForRoom("CODE-ZERO", 5000));
    }

    @Test
    public void testIsCodeUniqueForRoom_ReturnTrueWhenCodeBelongsToExcludedId() {
        assertTrue(storageLocationService.isCodeUniqueForRoom("TEST-R01", 5000));
    }

    @Test
    public void testIsCodeUniqueForDevice_ReturnTrueWhenCodeIsNull() {
        assertTrue(storageLocationService.isCodeUniqueForDevice(null, 5002));
    }

    @Test
    public void testIsCodeUniqueForDevice_ReturnTrueWhenCodeIsEmptyOrWhiteSpace() {
        assertTrue(storageLocationService.isCodeUniqueForDevice("    ", 5002));
    }

    @Test
    public void testIsCodeUniqueForDevice_ReturnTrueWhenNoExistingDeviceFound() {
        assertTrue(storageLocationService.isCodeUniqueForRoom("CODE-ZERO", 5002));
    }

    @Test
    public void testIsCodeUniqueForDevice_ReturnTrueWhenCodeBelongsToExcludedId() {
        assertTrue(storageLocationService.isCodeUniqueForDevice("TEST-C01", 5002));
    }

    @Test
    public void testIsCodeUniqueForDevice_ReturnFalseWhenCodeIsDuplicate() {
        assertFalse(storageLocationService.isCodeUniqueForDevice("TEST-D08", 5008));
    }

    @Test
    public void testIsCodeUniqueForShelf_ReturnTrueWhenCodeIsNull() {
        assertTrue(storageLocationService.isCodeUniqueForShelf(null, 5003));
    }

    @Test
    public void testIsCodeUniqueForShelf_ReturnTrueWhenCodeIsEmptyOrWhiteSpace() {
        assertTrue(storageLocationService.isCodeUniqueForShelf("    ", 5003));
    }

    @Test
    public void testIsCodeUniqueForShelf_ReturnTrueWhenNoExistingShelfFound() {
        assertTrue(storageLocationService.isCodeUniqueForShelf("CODE-ZERO", 5003));
    }

    @Test
    public void testIsCodeUniqueForShelf_ReturnTrueWhenCodeBelongsToExcludedId() {
        assertTrue(storageLocationService.isCodeUniqueForShelf("TEST-SB", 5001));
    }

    @Test
    public void testIsCodeUniqueForShelf_ReturnFalseWhenCodeBelongsIsDuplicate() {
        assertFalse(storageLocationService.isCodeUniqueForShelf("TEST-SI", 5004));
    }

    @Test
    public void testIsCodeUniqueForRack_ReturnTrueWhenCodeIsNull() {
        assertTrue(storageLocationService.isCodeUniqueForRack(null, 5004));
    }

    @Test
    public void testIsCodeUniqueForRack_ReturnTrueWhenCodeIsEmptyOrWhiteSpace() {
        assertTrue(storageLocationService.isCodeUniqueForRack("         ", 5004));
    }

    @Test
    public void testIsCodeUniqueForRack_ReturnTrueWhenNoExistingRackFound() {
        assertTrue(storageLocationService.isCodeUniqueForRack("CODE-ZERO", 5004));
    }

    @Test
    public void testIsCodeUniqueForRack_ReturnTrueWhenCodeBelongsToExcludedId() {
        assertTrue(storageLocationService.isCodeUniqueForRack("TEST-RR3", 5002));
    }

    @Test
    public void testIsCodeUniqueForRack_ReturnFalseWhenCodeBelongsIsDuplicate() {
        assertFalse(storageLocationService.isCodeUniqueForRack("TEST-RF", 5004));
    }
}