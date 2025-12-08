package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Storage Search REST endpoints. Tests tab-specific
 * search functionality per FR-064 and FR-064a (Phase 3.1 in plan.md): - Samples
 * tab: Search by sample ID, accession prefix, location path (OR logic) - Rooms
 * tab: Search by name and code - Devices tab: Search by name, code, and type -
 * Shelves tab: Search by label - Racks tab: Search by label
 *
 * All searches use case-insensitive partial/substring matching.
 *
 * Uses E2E test data from storage-e2e.xml: - Samples with accession numbers
 * E2E001-E2E010 - Sample items assigned to various storage locations - Storage
 * hierarchy (rooms, devices, shelves, racks, positions)
 */
@Rollback
public class StorageSearchRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/storage-e2e.xml");
    }

    @Test
    public void testSearchSamples_BySampleId_ReturnsMatching() throws Exception {
        // Search by parent Sample accession number (search should match SampleItem ID,
        // External ID, or parent Sample accession)
        // Use the accession number prefix which is more reliable than numeric ID
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "E2E001"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        boolean found = false;
        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            if (sampleAccessionNumber != null && sampleAccessionNumber.contains("E2E001")) {
                found = true;
                break;
            }
        }
        assertTrue("Should find SampleItem with matching parent Sample accession number", found);
    }

    @Test
    public void testSearchSamples_ByAccessionPrefix_ReturnsMatching() throws Exception {
        // Search by parent Sample accession number prefix (e.g., "E2E" matches
        // "E2E001", "E2E002", etc.)
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "E2E"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        // Verify all returned SampleItems have parent Sample accession number matching
        // prefix
        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            assertNotNull("Parent Sample accession number should not be null", sampleAccessionNumber);
            assertTrue("Parent Sample accession number should contain prefix",
                    sampleAccessionNumber.toLowerCase().contains("e2e"));
        }
    }

    @Test
    public void testSearchSamples_ByLocationPath_ReturnsMatching() throws Exception {
        // Search by location path substring (e.g., "Freezer" matches "Main Laboratory >
        // Freezer Unit 1 > ...")
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample", samples.size() >= 1);

        // Verify all returned samples have location path containing "Freezer"
        for (Map<String, Object> sample : samples) {
            String location = (String) sample.get("location");
            assertNotNull("Location should not be null", location);
            assertTrue("Location should contain 'Freezer' (case-insensitive)",
                    location.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void testSearchSamples_CombinedFields_OR_Logic() throws Exception {
        // Search should match ANY of the three fields (sample ID, accession prefix,
        // location path)
        // Test with a query that matches location path but not ID or accession
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Main Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample via location path", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_CaseInsensitive() throws Exception {
        // "freezer" should match "Freezer Unit 1" (case-insensitive)
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Should find samples even with lowercase query
        assertTrue("Should return at least one matching sample (case-insensitive)", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_PartialMatch() throws Exception {
        // "E2E00" should match "E2E001", "E2E002", etc. (partial substring)
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "E2E00"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample (partial match)", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_EmptyQuery_ReturnsAll() throws Exception {
        // Empty search should return all samples
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", ""))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        // Should return all samples (at least the E2E samples)
        assertTrue("Should return all samples when query is empty", samples.size() >= 1);
    }

    @Test
    public void testSearchSamples_NoMatches_ReturnsEmpty() throws Exception {
        // Query that matches nothing should return empty array
        MvcResult result = mockMvc
                .perform(get("/rest/storage/samples/search").param("q", "NONEXISTENT-SAMPLE-ID-999999"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertEquals("Should return empty array when no matches", 0, samples.size());
    }

    @Test
    public void testSearchRooms_ByName_ReturnsMatching() throws Exception {
        // Search by name (case-insensitive partial) - using E2E room name
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Main Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);

        // Verify all returned rooms have name containing query
        for (Map<String, Object> room : rooms) {
            String name = (String) room.get("name");
            assertNotNull("Name should not be null", name);
            assertTrue("Name should contain query (case-insensitive)", name.toLowerCase().contains("main laboratory"));
        }
    }

    @Test
    public void testSearchRooms_ByCode_ReturnsMatching() throws Exception {
        // Search by code (case-insensitive partial) - using E2E room code
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "MAIN"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);
    }

    @Test
    public void testSearchRooms_CombinedFields_OR_Logic() throws Exception {
        // Search should match name OR code
        // Query "ROOM" should match both name and code (if any contain "room")
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room (name or code)", rooms.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByName_ReturnsMatching() throws Exception {
        // Search by name - using E2E device name
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "Freezer Unit"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByCode_ReturnsMatching() throws Exception {
        // Search by code - using E2E device code
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "FRZ01"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void testSearchDevices_ByType_ReturnsMatching() throws Exception {
        // Search by type (freezer, refrigerator, etc.)
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);

        // Verify all returned devices have matching deviceType (physical type, not
        // hierarchy level "type")
        for (Map<String, Object> device : devices) {
            String deviceType = (String) device.get("deviceType");
            assertNotNull("DeviceType should not be null", deviceType);
            assertTrue("DeviceType should match query (case-insensitive)",
                    deviceType.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void testSearchDevices_CombinedFields_OR_Logic() throws Exception {
        // Search should match name OR code OR type
        // Query "freezer" should match type
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device (name or code or type)", devices.size() >= 1);
    }

    @Test
    public void testSearchShelves_ByLabel_ReturnsMatching() throws Exception {
        // Search by label (case-insensitive partial) - using E2E shelf label
        MvcResult result = mockMvc.perform(get("/rest/storage/shelves/search").param("q", "Shelf-A"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);
        assertTrue("Should return at least one matching shelf", shelves.size() >= 1);

        // Verify all returned shelves have label containing query
        for (Map<String, Object> shelf : shelves) {
            String label = (String) shelf.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("shelf-a"));
        }
    }

    @Test
    public void testSearchRacks_ByLabel_ReturnsMatching() throws Exception {
        // Search by label (case-insensitive partial) - using E2E rack label
        MvcResult result = mockMvc.perform(get("/rest/storage/racks/search").param("q", "Rack R1"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertTrue("Should return at least one matching rack", racks.size() >= 1);

        // Verify all returned racks have label containing query
        for (Map<String, Object> rack : racks) {
            String label = (String) rack.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("rack r1"));
        }
    }
}
