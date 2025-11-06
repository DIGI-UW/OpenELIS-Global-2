package org.openelisglobal.storage.service;

import static org.junit.Assert.*;
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

/**
 * Test for StorageLocationService.searchLocations to verify parent IDs and
 * names are included
 */
@RunWith(MockitoJUnitRunner.class)
public class StorageLocationServiceSearchTest {

    @Mock
    private StorageSearchService storageSearchService;

    @InjectMocks
    private StorageLocationServiceImpl storageLocationService;

    @Before
    public void setUp() {
        // Use reflection to inject mock
        try {
            java.lang.reflect.Field searchServiceField = StorageLocationServiceImpl.class
                    .getDeclaredField("storageSearchService");
            searchServiceField.setAccessible(true);
            searchServiceField.set(storageLocationService, storageSearchService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock", e);
        }
    }

    @Test
    public void testSearchLocations_Device_IncludesParentRoomIdAndName() {
        // Given: Device with parent room
        List<Map<String, Object>> devices = new ArrayList<>();
        Map<String, Object> device = new HashMap<>();
        device.put("id", 10);
        device.put("name", "Main Freezer");
        device.put("code", "FRZ-01");
        device.put("type", "device"); // Hierarchy level from getDevicesForAPI
        device.put("deviceType", "freezer"); // Physical type from getDevicesForAPI
        device.put("parentRoomId", 1);
        device.put("roomName", "Main Laboratory");
        device.put("parentRoomName", "Main Laboratory"); // From getDevicesForAPI
        devices.add(device);

        List<Map<String, Object>> rooms = new ArrayList<>();
        List<Map<String, Object>> shelves = new ArrayList<>();
        List<Map<String, Object>> racks = new ArrayList<>();

        when(storageSearchService.searchRooms("Freezer")).thenReturn(rooms);
        when(storageSearchService.searchDevices("Freezer")).thenReturn(devices);
        when(storageSearchService.searchShelves("Freezer")).thenReturn(shelves);
        when(storageSearchService.searchRacks("Freezer")).thenReturn(racks);

        // When: Search for locations
        List<Map<String, Object>> results = storageLocationService.searchLocations("Freezer");

        // Then: Result should include parentRoomId and parentRoomName
        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one device", results.size() >= 1);

        Map<String, Object> deviceResult = results.stream().filter(r -> "device".equals(r.get("type"))).findFirst()
                .orElse(null);

        assertNotNull("Device result should exist", deviceResult);
        assertEquals("Should have correct ID", 10, deviceResult.get("id"));
        assertEquals("Should have type as hierarchy level", "device", deviceResult.get("type"));
        assertEquals("Should have deviceType as physical type", "freezer", deviceResult.get("deviceType"));
        assertEquals("Should have parentRoomId", 1, deviceResult.get("parentRoomId"));
        assertEquals("Should have parentRoomName", "Main Laboratory", deviceResult.get("parentRoomName"));
        assertNotNull("Should have hierarchicalPath", deviceResult.get("hierarchicalPath"));
        assertTrue("Hierarchical path should contain room name",
                ((String) deviceResult.get("hierarchicalPath")).contains("Main Laboratory"));
    }

    @Test
    public void testSearchLocations_Shelf_IncludesParentDeviceAndRoomIdsAndNames() {
        // Given: Shelf with parent device and room (only parent-prefixed fields from
        // getShelvesForAPI)
        List<Map<String, Object>> shelves = new ArrayList<>();
        Map<String, Object> shelf = new HashMap<>();
        shelf.put("id", 20);
        shelf.put("label", "Shelf-A");
        shelf.put("parentDeviceId", 10);
        shelf.put("deviceName", "Main Freezer");
        shelf.put("parentDeviceName", "Main Freezer");
        shelf.put("parentRoomId", 1);
        shelf.put("roomName", "Main Laboratory");
        shelf.put("parentRoomName", "Main Laboratory");
        shelves.add(shelf);

        List<Map<String, Object>> rooms = new ArrayList<>();
        List<Map<String, Object>> devices = new ArrayList<>();
        List<Map<String, Object>> racks = new ArrayList<>();

        when(storageSearchService.searchRooms("Shelf")).thenReturn(rooms);
        when(storageSearchService.searchDevices("Shelf")).thenReturn(devices);
        when(storageSearchService.searchShelves("Shelf")).thenReturn(shelves);
        when(storageSearchService.searchRacks("Shelf")).thenReturn(racks);

        // When: Search for locations
        List<Map<String, Object>> results = storageLocationService.searchLocations("Shelf");

        // Then: Result should include parentDeviceId, parentDeviceName, parentRoomId,
        // parentRoomName
        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one shelf", results.size() >= 1);

        Map<String, Object> shelfResult = results.stream().filter(r -> "shelf".equals(r.get("type"))).findFirst()
                .orElse(null);

        assertNotNull("Shelf result should exist", shelfResult);
        assertEquals("Should have correct ID", 20, shelfResult.get("id"));
        assertEquals("Should have parentDeviceId", 10, shelfResult.get("parentDeviceId"));
        assertEquals("Should have parentDeviceName", "Main Freezer", shelfResult.get("parentDeviceName"));
        assertEquals("Should have parentRoomId", 1, shelfResult.get("parentRoomId"));
        assertEquals("Should have parentRoomName", "Main Laboratory", shelfResult.get("parentRoomName"));
        assertNotNull("Should have hierarchicalPath", shelfResult.get("hierarchicalPath"));
        String hierarchicalPath = (String) shelfResult.get("hierarchicalPath");
        assertTrue("Hierarchical path should contain room name", hierarchicalPath.contains("Main Laboratory"));
        assertTrue("Hierarchical path should contain device name", hierarchicalPath.contains("Main Freezer"));
    }

    @Test
    public void testSearchLocations_Rack_IncludesAllParentIdsAndNames() {
        // Given: Rack with parent shelf, device, and room (only parent-prefixed fields
        // from getRacksForAPI)
        List<Map<String, Object>> racks = new ArrayList<>();
        Map<String, Object> rack = new HashMap<>();
        rack.put("id", 30);
        rack.put("label", "Rack R1");
        rack.put("parentShelfId", 20);
        rack.put("shelfLabel", "Shelf-A");
        rack.put("parentShelfLabel", "Shelf-A");
        rack.put("parentDeviceId", 10);
        rack.put("deviceName", "Main Freezer");
        rack.put("parentDeviceName", "Main Freezer");
        rack.put("parentRoomId", 1);
        rack.put("roomName", "Main Laboratory");
        rack.put("parentRoomName", "Main Laboratory");
        racks.add(rack);

        List<Map<String, Object>> rooms = new ArrayList<>();
        List<Map<String, Object>> devices = new ArrayList<>();
        List<Map<String, Object>> shelves = new ArrayList<>();

        when(storageSearchService.searchRooms("Rack")).thenReturn(rooms);
        when(storageSearchService.searchDevices("Rack")).thenReturn(devices);
        when(storageSearchService.searchShelves("Rack")).thenReturn(shelves);
        when(storageSearchService.searchRacks("Rack")).thenReturn(racks);

        // When: Search for locations
        List<Map<String, Object>> results = storageLocationService.searchLocations("Rack");

        // Then: Result should include all parent IDs and names
        assertNotNull("Results should not be null", results);
        assertTrue("Should return at least one rack", results.size() >= 1);

        Map<String, Object> rackResult = results.stream().filter(r -> "rack".equals(r.get("type"))).findFirst()
                .orElse(null);

        assertNotNull("Rack result should exist", rackResult);
        assertEquals("Should have correct ID", 30, rackResult.get("id"));
        assertEquals("Should have parentShelfId", 20, rackResult.get("parentShelfId"));
        assertEquals("Should have parentShelfLabel", "Shelf-A", rackResult.get("parentShelfLabel"));
        assertEquals("Should have parentDeviceId", 10, rackResult.get("parentDeviceId"));
        assertEquals("Should have parentDeviceName", "Main Freezer", rackResult.get("parentDeviceName"));
        assertEquals("Should have parentRoomId", 1, rackResult.get("parentRoomId"));
        assertEquals("Should have parentRoomName", "Main Laboratory", rackResult.get("parentRoomName"));
        assertNotNull("Should have hierarchicalPath", rackResult.get("hierarchicalPath"));
        String hierarchicalPath = (String) rackResult.get("hierarchicalPath");
        assertTrue("Hierarchical path should contain room name", hierarchicalPath.contains("Main Laboratory"));
        assertTrue("Hierarchical path should contain device name", hierarchicalPath.contains("Main Freezer"));
        assertTrue("Hierarchical path should contain shelf label", hierarchicalPath.contains("Shelf-A"));
    }
}
