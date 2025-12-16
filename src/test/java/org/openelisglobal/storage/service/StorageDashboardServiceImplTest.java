package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.valueholder.*;

/**
 * Unit tests for StorageDashboardService filter logic. Tests that filter
 * methods correctly combine multiple filter criteria with AND logic (per
 * FR-066).
 * 
 * TDD: These tests are written BEFORE implementation. They should fail
 * initially.
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageDashboardServiceImplTest {

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private StorageLocationService storageLocationService;

    @InjectMocks
    private StorageDashboardServiceImpl dashboardService;

    private List<Map<String, Object>> mockSamples;
    private List<StorageRoom> mockRooms;
    private List<StorageDevice> mockDevices;
    private List<StorageShelf> mockShelves;
    private List<StorageRack> mockRacks;

    @Before
    public void setUp() {
        dashboardService = new StorageDashboardServiceImpl();
        // Use reflection or setter injection to set mocks
        try {
            java.lang.reflect.Field sampleServiceField = StorageDashboardServiceImpl.class
                    .getDeclaredField("sampleStorageService");
            sampleServiceField.setAccessible(true);
            sampleServiceField.set(dashboardService, sampleStorageService);

            java.lang.reflect.Field locationServiceField = StorageDashboardServiceImpl.class
                    .getDeclaredField("storageLocationService");
            locationServiceField.setAccessible(true);
            locationServiceField.set(dashboardService, storageLocationService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocks", e);
        }
        setupMockData();
    }

    private void setupMockData() {
        // Mock samples
        mockSamples = new ArrayList<>();
        Map<String, Object> sample1 = new HashMap<>();
        sample1.put("id", 1);
        sample1.put("status", "active");
        sample1.put("location", "Room1 > Device1 > Position1");
        mockSamples.add(sample1);

        Map<String, Object> sample2 = new HashMap<>();
        sample2.put("id", 2);
        sample2.put("status", "active");
        sample2.put("location", "Room2 > Device2 > Position2");
        mockSamples.add(sample2);

        Map<String, Object> sample3 = new HashMap<>();
        sample3.put("id", 3);
        sample3.put("status", "disposed");
        sample3.put("location", "Room1 > Device1 > Position3");
        mockSamples.add(sample3);

        // Mock rooms
        mockRooms = new ArrayList<>();
        StorageRoom room1 = new StorageRoom();
        room1.setId(1);
        room1.setActive(true);
        mockRooms.add(room1);

        StorageRoom room2 = new StorageRoom();
        room2.setId(2);
        room2.setActive(false);
        mockRooms.add(room2);

        // Mock devices
        mockDevices = new ArrayList<>();
        StorageDevice device1 = new StorageDevice();
        device1.setId(1);
        device1.setTypeEnum(StorageDevice.DeviceType.FREEZER);
        device1.setParentRoom(room1);
        device1.setActive(true);
        mockDevices.add(device1);

        StorageDevice device2 = new StorageDevice();
        device2.setId(2);
        device2.setTypeEnum(StorageDevice.DeviceType.REFRIGERATOR);
        device2.setParentRoom(room1);
        device2.setActive(true);
        mockDevices.add(device2);

        // Mock shelves
        mockShelves = new ArrayList<>();
        StorageShelf shelf1 = new StorageShelf();
        shelf1.setId(1);
        shelf1.setParentDevice(device1);
        shelf1.setActive(true);
        mockShelves.add(shelf1);

        // Mock racks
        mockRacks = new ArrayList<>();
        StorageRack rack1 = new StorageRack();
        rack1.setId(1);
        rack1.setParentShelf(shelf1);
        rack1.setActive(true);
        mockRacks.add(rack1);
    }

    /**
     * Test: filterSamples should combine location and status filters with AND logic
     */
    @Test
    public void testFilterSamples_ByLocationAndStatus_CombinesWithAND() {
        // Given: All samples from service
        when(sampleStorageService.getAllSamplesWithAssignments()).thenReturn(mockSamples);

        // When: Filter by location containing "Room1" AND status "active"
        List<Map<String, Object>> result = dashboardService.filterSamples(
                "Room1", "active");

        // Then: Should return only sample1 (matches both filters)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 sample matching both filters", 1, result.size());
        assertEquals("Sample ID should be 1", 1, result.get(0).get("id"));
        assertEquals("Status should be active", "active", result.get(0).get("status"));
        assertTrue("Location should contain Room1", 
                ((String) result.get(0).get("location")).contains("Room1"));
    }

    /**
     * Test: filterRooms should filter by status
     */
    @Test
    public void testFilterRooms_ByStatus_ReturnsMatching() {
        // Given: All rooms from service
        when(storageLocationService.getRooms()).thenReturn(mockRooms);

        // When: Filter by active status
        List<StorageRoom> result = dashboardService.filterRooms(true);

        // Then: Should return only active rooms
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 active room", 1, result.size());
        assertTrue("Room should be active", result.get(0).getActive());
    }

    /**
     * Test: filterDevices should combine type, roomId, and status filters with AND logic
     */
    @Test
    public void testFilterDevices_ByTypeRoomStatus_CombinesWithAND() {
        // Given: All devices from service
        when(storageLocationService.getAllDevices()).thenReturn(mockDevices);

        // When: Filter by type FREEZER, roomId 1, and active status
        List<StorageDevice> result = dashboardService.filterDevices(
                StorageDevice.DeviceType.FREEZER, 1, true);

        // Then: Should return only device1 (matches all three filters)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 device matching all filters", 1, result.size());
        assertEquals("Device type should be FREEZER", 
                StorageDevice.DeviceType.FREEZER, result.get(0).getTypeEnum());
        assertEquals("Device roomId should be 1", 
                Integer.valueOf(1), result.get(0).getParentRoom().getId());
        assertTrue("Device should be active", result.get(0).getActive());
    }

    /**
     * Test: filterShelves should combine deviceId, roomId, and status filters with AND logic
     */
    @Test
    public void testFilterShelves_ByDeviceRoomStatus_CombinesWithAND() {
        // Given: All shelves from service
        when(storageLocationService.getAllShelves()).thenReturn(mockShelves);

        // When: Filter by deviceId 1, roomId 1, and active status
        List<StorageShelf> result = dashboardService.filterShelves(1, 1, true);

        // Then: Should return only shelf1 (matches all three filters)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 shelf matching all filters", 1, result.size());
        assertEquals("Shelf deviceId should be 1", 
                Integer.valueOf(1), result.get(0).getParentDevice().getId());
        assertEquals("Shelf roomId should be 1", 
                Integer.valueOf(1), result.get(0).getParentDevice().getParentRoom().getId());
        assertTrue("Shelf should be active", result.get(0).getActive());
    }

    /**
     * Test: filterRacks should combine roomId, shelfId, deviceId, and status filters with AND logic
     */
    @Test
    public void testFilterRacks_ByRoomShelfDeviceStatus_CombinesWithAND() {
        // Given: All racks from service
        when(storageLocationService.getAllRacks()).thenReturn(mockRacks);

        // When: Filter by roomId 1, shelfId 1, deviceId 1, and active status
        List<StorageRack> result = dashboardService.filterRacks(1, 1, 1, true);

        // Then: Should return only rack1 (matches all four filters)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 rack matching all filters", 1, result.size());
        assertEquals("Rack shelfId should be 1", 
                Integer.valueOf(1), result.get(0).getParentShelf().getId());
        assertEquals("Rack deviceId should be 1", 
                Integer.valueOf(1), result.get(0).getParentShelf().getParentDevice().getId());
        assertEquals("Rack roomId should be 1", 
                Integer.valueOf(1), result.get(0).getParentShelf().getParentDevice().getParentRoom().getId());
        assertTrue("Rack should be active", result.get(0).getActive());
    }

    /**
     * Test: getRacksForAPI should include parentRoomId column (FR-065a)
     * Note: Uses parentRoomId (not roomId) for consistency with other parent-prefixed keys
     */
    @Test
    public void testGetRacks_IncludesRoomColumn() {
        // Given: All racks from service
        when(storageLocationService.getAllRacks()).thenReturn(mockRacks);

        // When: Get racks for API
        List<Map<String, Object>> result = dashboardService.getRacksForAPI(null, null, null, null);

        // Then: All racks should have parentRoomId column
        assertNotNull("Result should not be null", result);
        assertFalse("Should return at least one rack", result.isEmpty());
        for (Map<String, Object> rack : result) {
            assertTrue("Rack should have parentRoomId key", rack.containsKey("parentRoomId"));
            assertNotNull("Rack parentRoomId should not be null", rack.get("parentRoomId"));
        }
    }

    /**
     * Test: filterShelvesForAPI should filter by parentDeviceId (not deviceId) This
     * test verifies the fix for the bug where filterShelvesForAPI was looking for
     * "deviceId" instead of "parentDeviceId" in the Map objects.
     */
    @Test
    public void testFilterShelvesForAPI_ByDeviceId_ReturnsMatchingShelves() {
        // Given: Mock shelves from getShelvesForAPI (which returns Maps with
        // parentDeviceId)
        List<Map<String, Object>> mockShelvesForAPI = new ArrayList<>();
        Map<String, Object> shelf1 = new HashMap<>();
        shelf1.put("id", 1);
        shelf1.put("parentDeviceId", 1); // CRITICAL: Uses parentDeviceId, not deviceId
        shelf1.put("parentRoomId", 1);
        shelf1.put("active", true);
        mockShelvesForAPI.add(shelf1);

        Map<String, Object> shelf2 = new HashMap<>();
        shelf2.put("id", 2);
        shelf2.put("parentDeviceId", 2); // Different device
        shelf2.put("parentRoomId", 1);
        shelf2.put("active", true);
        mockShelvesForAPI.add(shelf2);

        when(storageLocationService.getShelvesForAPI(null)).thenReturn(mockShelvesForAPI);

        // When: Filter by deviceId 1
        List<Map<String, Object>> result = dashboardService.filterShelvesForAPI(1, null, null);

        // Then: Should return only shelf1 (matches deviceId 1)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 shelf matching deviceId 1", 1, result.size());
        assertEquals("Shelf ID should be 1", 1, result.get(0).get("id"));
        assertEquals("Shelf parentDeviceId should be 1", 1, result.get(0).get("parentDeviceId"));
    }

    /**
     * Test: filterShelvesForAPI should filter by parentRoomId (not roomId)
     */
    @Test
    public void testFilterShelvesForAPI_ByRoomId_ReturnsMatchingShelves() {
        // Given: Mock shelves from getShelvesForAPI (which returns Maps with
        // parentRoomId)
        List<Map<String, Object>> mockShelvesForAPI = new ArrayList<>();
        Map<String, Object> shelf1 = new HashMap<>();
        shelf1.put("id", 1);
        shelf1.put("parentDeviceId", 1);
        shelf1.put("parentRoomId", 1); // CRITICAL: Uses parentRoomId, not roomId
        shelf1.put("active", true);
        mockShelvesForAPI.add(shelf1);

        Map<String, Object> shelf2 = new HashMap<>();
        shelf2.put("id", 2);
        shelf2.put("parentDeviceId", 2);
        shelf2.put("parentRoomId", 2); // Different room
        shelf2.put("active", true);
        mockShelvesForAPI.add(shelf2);

        when(storageLocationService.getShelvesForAPI(null)).thenReturn(mockShelvesForAPI);

        // When: Filter by roomId 1
        List<Map<String, Object>> result = dashboardService.filterShelvesForAPI(null, 1, null);

        // Then: Should return only shelf1 (matches roomId 1)
        assertNotNull("Result should not be null", result);
        assertEquals("Should return 1 shelf matching roomId 1", 1, result.size());
        assertEquals("Shelf ID should be 1", 1, result.get(0).get("id"));
        assertEquals("Shelf parentRoomId should be 1", 1, result.get(0).get("parentRoomId"));
    }
}
