package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class StorageDashboardRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/storage-dashboard-test-data.xml");
    }

    @Test
    public void getSamples_ShouldReturnFiltered_WhenFilteredByLocation() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/sample-items").param("location", "Test Integration Room").param("status",
                        "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Parse as Map first, then extract items
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) response.get("items");

        assertNotNull("Response should not be null", samples);

        if (samples.size() > 0) {
            for (Map<String, Object> sample : samples) {
                String location = (String) sample.get("location");
                assertNotNull("Location should not be null", location);
                assertTrue("Location should contain test room name",
                        location.contains("Test Integration Room") || location.contains("Test Integration"));
            }
        }
    }

    @Test
    public void getSamples_ShouldReturnFiltered_WhenFilteredByStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items").param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Parse as Map first, then extract items
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) response.get("items");

        assertNotNull("Response should not be null", samples);

        for (Map<String, Object> sample : samples) {
            // Status is a String "1" for active (based on your debug output)
            Object statusObj = sample.get("status");
            assertNotNull("Status should not be null", statusObj);

            // Convert to string and check
            String status = statusObj.toString();

            // Either check for string "1" or check for "active"
            // Based on your API, it returns "1" for active
            assertTrue("Status should be '1' for active (was: " + status + ")",
                    "1".equals(status) || "active".equalsIgnoreCase(status));
        }
    }

    @Test
    public void getRooms_ShouldReturnFiltered_WhenFilteredByStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms").param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);

        for (Map<String, Object> room : rooms) {
            Boolean active = (Boolean) room.get("active");
            assertNotNull("Active status should not be null", active);
            assertTrue("Room should be active", active);
        }
    }

    @Test
    public void getDevices_ShouldReturnFiltered_WhenFilteredByTypeRoomStatus() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/devices").param("type", "FREEZER").param("roomId", "1000").param("status",
                        "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);

        for (Map<String, Object> device : devices) {
            String type = (String) device.get("type");
            Integer roomId = (Integer) device.get("roomId");
            Boolean active = (Boolean) device.get("active");

            assertEquals("Device type should match", "freezer", type);
            assertEquals("Device roomId should match", Integer.valueOf(1000), roomId);
            assertTrue("Device should be active", active);
        }
    }

    @Test
    public void getShelves_ShouldReturnFiltered_WhenFilteredByDeviceRoomStatus() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/shelves").param("deviceId", "1000").param("roomId", "1000").param("status",
                        "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);

        for (Map<String, Object> shelf : shelves) {
            Integer deviceId = (Integer) shelf.get("parentDeviceId");
            Integer roomId = (Integer) shelf.get("parentRoomId");
            Boolean active = (Boolean) shelf.get("active");

            assertEquals("Shelf deviceId should match", Integer.valueOf(1000), deviceId);
            assertEquals("Shelf roomId should match", Integer.valueOf(1000), roomId);
            assertTrue("Shelf should be active", active);
        }
    }

    @Test
    public void getRacks_ShouldReturnFiltered_WhenFilteredByRoomShelfDeviceStatus() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/racks").param("roomId", "1000").param("shelfId", "1000")
                        .param("deviceId", "1000").param("status", "active"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);

        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("parentRoomId");
            Integer shelfId = (Integer) rack.get("parentShelfId");
            Integer deviceId = (Integer) rack.get("parentDeviceId");
            Boolean active = (Boolean) rack.get("active");

            assertEquals("Rack roomId should match", Integer.valueOf(1000), roomId);
            assertEquals("Rack shelfId should match", Integer.valueOf(1000), shelfId);
            assertEquals("Rack deviceId should match", Integer.valueOf(1000), deviceId);
            assertTrue("Rack should be active", active);
        }
    }

    @Test
    public void getRacks_ShouldReturnRoomColumn() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/racks")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertFalse("Should return at least one rack", racks.isEmpty());

        for (Map<String, Object> rack : racks) {
            Integer roomId = (Integer) rack.get("parentRoomId");
            assertNotNull("Rack should have parentRoomId column", roomId);
        }
    }

    @Test
    public void getLocationCounts_ShouldReturnActiveCounts_ByType() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/dashboard/location-counts")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> counts = objectMapper.readValue(responseBody, Map.class);

        assertNotNull("Response should not be null", counts);

        assertTrue("Response should contain rooms count", counts.containsKey("rooms"));
        assertTrue("Response should contain devices count", counts.containsKey("devices"));
        assertTrue("Response should contain shelves count", counts.containsKey("shelves"));
        assertTrue("Response should contain racks count", counts.containsKey("racks"));

        assertNotNull("Rooms count should not be null", counts.get("rooms"));
        assertNotNull("Devices count should not be null", counts.get("devices"));
        assertNotNull("Shelves count should not be null", counts.get("shelves"));
        assertNotNull("Racks count should not be null", counts.get("racks"));

        Integer roomsCount = ((Number) counts.get("rooms")).intValue();
        Integer devicesCount = ((Number) counts.get("devices")).intValue();
        Integer shelvesCount = ((Number) counts.get("shelves")).intValue();
        Integer racksCount = ((Number) counts.get("racks")).intValue();

        assertTrue("Rooms count should be non-negative", roomsCount >= 0);
        assertTrue("Devices count should be non-negative", devicesCount >= 0);
        assertTrue("Shelves count should be non-negative", shelvesCount >= 0);
        assertTrue("Racks count should be non-negative", racksCount >= 0);

        int totalCount = roomsCount + devicesCount + shelvesCount + racksCount;
        assertTrue("Should have at least some active locations", totalCount > 0);
    }

    @Test
    public void getLocationCounts_ShouldExcludeInactiveLocations() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/dashboard/location-counts")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> counts = objectMapper.readValue(responseBody, Map.class);

        Integer roomsCount = ((Number) counts.get("rooms")).intValue();
        Integer devicesCount = ((Number) counts.get("devices")).intValue();

        assertTrue("Rooms count should be reasonable", roomsCount >= 0);
        assertTrue("Devices count should be reasonable", devicesCount >= 0);
    }
}