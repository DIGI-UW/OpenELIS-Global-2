package org.openelisglobal.storage.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.springframework.beans.factory.annotation.Autowired;

public class StorageDashboardServiceImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageDashboardService storageDashboardService;

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/StorageDashboardServiceImplTest.xml");
    }

    @Test
    public void filterSamples_shouldReturnSamplesMatchingLocationAndStatus_whenBothAreProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterSamples("Room A", "active");

        assertNotNull(result);
        assertEquals(2, result.size());

        for (Map<String, Object> sample : result) {
            String status = (String) sample.get("status");
            String location = (String) sample.get("location");
            assertTrue("1".equals(status));
            assertTrue(location.contains("Room A"));
        }
    }

    @Test
    public void filterSamples_shouldReturnAllSamplesInLocation_whenOnlyLocationIsProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterSamples("Room B", null);

        assertNotNull(result);
        assertEquals(3, result.size());

        for (Map<String, Object> sample : result) {
            String location = (String) sample.get("location");
            assertTrue(location.contains("Room B"));
        }
    }

    @Test
    public void filterSamples_shouldReturnSamplesMatchingStatus_whenOnlyStatusIsProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterSamples(null, "disposed");

        assertNotNull(result);

        for (Map<String, Object> sample : result) {
            String status = (String) sample.get("status");
            assertEquals("24", status);
        }
    }

    @Test
    public void filterRooms_shouldReturnActiveAndInactiveRoomsBasedOnStatusFlag() throws Exception {
        List<StorageRoom> activeRooms = storageDashboardService.filterRooms(true);

        assertNotNull(activeRooms);
        assertEquals(3, activeRooms.size());

        for (StorageRoom room : activeRooms) {
            assertTrue(room.getActive());
        }

        List<StorageRoom> inactiveRooms = storageDashboardService.filterRooms(false);

        assertNotNull(inactiveRooms);
        assertEquals(1, inactiveRooms.size());

        for (StorageRoom room : inactiveRooms) {
            assertFalse(room.getActive());
        }
    }

    @Test
    public void filterDevices_shouldReturnDevicesMatchingTypeRoomAndStatus_whenAllAreProvided() throws Exception {
        List<StorageDevice> result = storageDashboardService.filterDevices(StorageDevice.DeviceType.FREEZER, 1, true);

        assertNotNull(result);
        assertEquals(2, result.size());

        for (StorageDevice device : result) {
            assertEquals(StorageDevice.DeviceType.FREEZER, device.getTypeEnum());
            assertEquals(Integer.valueOf(1), device.getParentRoom().getId());
            assertTrue(device.getActive());
        }
    }

    @Test
    public void filterDevices_shouldReturnDevicesMatchingType_whenOnlyTypeIsProvided() throws Exception {
        List<StorageDevice> result = storageDashboardService.filterDevices(StorageDevice.DeviceType.REFRIGERATOR, null,
                null);

        assertNotNull(result);
        assertEquals(3, result.size());

        for (StorageDevice device : result) {
            assertEquals(StorageDevice.DeviceType.REFRIGERATOR, device.getTypeEnum());
        }
    }

    @Test
    public void filterShelves_shouldReturnShelvesMatchingDeviceRoomAndStatus_whenAllAreProvided() throws Exception {
        List<StorageShelf> result = storageDashboardService.filterShelves(3, 2, true);

        assertNotNull(result);
        assertEquals(2, result.size());

        for (StorageShelf shelf : result) {
            assertEquals(Integer.valueOf(3), shelf.getParentDevice().getId());
            assertEquals(Integer.valueOf(2), shelf.getParentDevice().getParentRoom().getId());
            assertTrue(shelf.getActive());
        }
    }

    @Test
    public void filterRacks_shouldReturnRacksMatchingRoomShelfDeviceAndStatus_whenAllAreProvided() throws Exception {
        List<StorageRack> result = storageDashboardService.filterRacks(1, 1, 1, true);

        assertNotNull(result);
        assertEquals(1, result.size());

        StorageRack rack = result.get(0);
        assertEquals(Integer.valueOf(1), rack.getParentShelf().getId());
        assertEquals(Integer.valueOf(1), rack.getParentShelf().getParentDevice().getId());
        assertEquals(Integer.valueOf(1), rack.getParentShelf().getParentDevice().getParentRoom().getId());
        assertTrue(rack.getActive());
    }

    @Test
    public void getRacksForAPI_shouldIncludeParentIdentifiers_whenReturningRacks() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.getRacksForAPI(null, null, null, null);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        for (Map<String, Object> rack : result) {
            assertTrue(rack.containsKey("parentRoomId"));
            assertNotNull(rack.get("parentRoomId"));
            assertTrue(rack.containsKey("parentShelfId"));
            assertTrue(rack.containsKey("parentDeviceId"));
        }
    }

    @Test
    public void filterShelvesForAPI_shouldReturnShelvesMatchingDeviceId_whenDeviceIdIsProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterShelvesForAPI(2, null, null);

        assertNotNull(result);
        assertEquals(3, result.size());

        for (Map<String, Object> shelf : result) {
            assertEquals(Integer.valueOf(2), shelf.get("parentDeviceId"));
        }
    }

    @Test
    public void filterShelvesForAPI_shouldReturnShelvesMatchingRoomId_whenRoomIdIsProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterShelvesForAPI(null, 3, null);

        assertNotNull(result);
        assertEquals(2, result.size());

        for (Map<String, Object> shelf : result) {
            assertEquals(3, shelf.get("parentRoomId"));
        }
    }

    @Test
    public void filterShelvesForAPI_shouldReturnShelvesMatchingStatus_whenStatusIsProvided() throws Exception {
        List<Map<String, Object>> activeShelves = storageDashboardService.filterShelvesForAPI(null, null, true);

        assertNotNull(activeShelves);
        assertFalse(activeShelves.isEmpty());

        for (Map<String, Object> shelf : activeShelves) {
            assertTrue((Boolean) shelf.get("active"));
        }

        List<Map<String, Object>> inactiveShelves = storageDashboardService.filterShelvesForAPI(null, null, false);

        assertNotNull(inactiveShelves);
        assertFalse(inactiveShelves.isEmpty());

        for (Map<String, Object> shelf : inactiveShelves) {
            assertFalse((Boolean) shelf.get("active"));
        }
    }

    @Test
    public void filterShelvesForAPI_shouldReturnShelvesMatchingAllFilters_whenAllAreProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterShelvesForAPI(1, 1, true);

        assertNotNull(result);
        assertEquals(2, result.size());

        for (Map<String, Object> shelf : result) {
            assertEquals(Integer.valueOf(1), shelf.get("parentDeviceId"));
            assertEquals(Integer.valueOf(1), shelf.get("parentRoomId"));
            assertTrue((Boolean) shelf.get("active"));
        }
    }

    @Test
    public void getRacksForAPI_shouldReturnRacksMatchingAllFilters_whenAllAreProvided() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.getRacksForAPI(1, 1, 1, true);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        for (Map<String, Object> rack : result) {
            assertEquals(Integer.valueOf(1), rack.get("parentShelfId"));
            assertEquals(Integer.valueOf(1), rack.get("parentDeviceId"));
            assertEquals(Integer.valueOf(1), rack.get("parentRoomId"));
            assertTrue((Boolean) rack.get("active"));
        }
    }

    @Test
    public void filterSamples_shouldReturnEmptyList_whenNoSamplesMatchFilters() throws Exception {
        List<Map<String, Object>> result = storageDashboardService.filterSamples("NonExistentRoom", "active");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void filterDevices_shouldReturnEmptyList_whenNoDevicesMatchFilters() throws Exception {
        List<StorageDevice> result = storageDashboardService.filterDevices(StorageDevice.DeviceType.FREEZER, 999, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
