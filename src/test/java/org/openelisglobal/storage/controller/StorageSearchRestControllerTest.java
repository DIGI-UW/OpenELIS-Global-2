package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class StorageSearchRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/storage-search-integration-test.xml");
    }

    @Test
    public void searchSamples_ReturnsMatching_WhenBySampleId() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "INT001"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        boolean found = false;
        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            if (sampleAccessionNumber != null && sampleAccessionNumber.contains("INT001")) {
                found = true;
                break;
            }
        }
        assertTrue("Should find SampleItem with matching parent Sample accession number", found);
    }

    @Test
    public void searchSamples_ReturnsMatching_WhenByAccessionPrefix() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "INT"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> sampleItems = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", sampleItems);
        assertTrue("Should return at least one matching SampleItem", sampleItems.size() >= 1);

        for (Map<String, Object> sampleItem : sampleItems) {
            String sampleAccessionNumber = (String) sampleItem.get("sampleAccessionNumber");
            assertNotNull("Parent Sample accession number should not be null", sampleAccessionNumber);
            assertTrue("Parent Sample accession number should contain prefix",
                    sampleAccessionNumber.toLowerCase().contains("int"));
        }
    }

    @Test
    public void searchSamples_ReturnsMatching_WhenByLocationPath() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample", samples.size() >= 1);

        for (Map<String, Object> sample : samples) {
            String location = (String) sample.get("location");
            assertNotNull("Location should not be null", location);
            assertTrue("Location should contain 'Freezer' (case-insensitive)",
                    location.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void searchSamples_ReturnsResults_WhenCombinedFieldsWithORLogic() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "Main Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample via location path", samples.size() >= 1);
    }

    @Test
    public void searchSamples_ReturnsResults_WhenCaseInsensitiveQuery() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample (case-insensitive)", samples.size() >= 1);
    }

    @Test
    public void searchSamples_ReturnsResults_WhenPartialMatch() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", "INT00"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return at least one matching sample (partial match)", samples.size() >= 1);
    }

    @Test
    public void searchSamples_ReturnsAll_WhenEmptyQuery() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/samples/search").param("q", ""))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> samples = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", samples);
        assertTrue("Should return all samples when query is empty", samples.size() >= 1);
    }

    @Test
    public void searchSamples_ReturnsEmpty_WhenNoMatches() throws Exception {
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
    public void searchRooms_ReturnsMatching_WhenByName() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Main Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);

        for (Map<String, Object> room : rooms) {
            String name = (String) room.get("name");
            assertNotNull("Name should not be null", name);
            assertTrue("Name should contain query (case-insensitive)", name.toLowerCase().contains("main laboratory"));
        }
    }

    @Test
    public void searchRooms_ReturnsMatching_WhenByCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "MAIN"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room", rooms.size() >= 1);
    }

    @Test
    public void searchRooms_ReturnsResults_WhenCombinedFieldsWithORLogic() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/rooms/search").param("q", "Laboratory"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> rooms = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", rooms);
        assertTrue("Should return at least one matching room (name or code)", rooms.size() >= 1);
    }

    @Test
    public void searchDevices_ReturnsMatching_WhenByName() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "Freezer Unit"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void searchDevices_ReturnsMatching_WhenByCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "FRZ01"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);
    }

    @Test
    public void searchDevices_ReturnsMatching_WhenByType() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device", devices.size() >= 1);

        for (Map<String, Object> device : devices) {
            String deviceType = (String) device.get("deviceType");
            assertNotNull("DeviceType should not be null", deviceType);
            assertTrue("DeviceType should match query (case-insensitive)",
                    deviceType.toLowerCase().contains("freezer"));
        }
    }

    @Test
    public void searchDevices_ReturnsResults_WhenCombinedFieldsWithORLogic() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/devices/search").param("q", "freezer"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> devices = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", devices);
        assertTrue("Should return at least one matching device (name or code or type)", devices.size() >= 1);
    }

    @Test
    public void searchShelves_ReturnsMatching_WhenByLabel() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/shelves/search").param("q", "Shelf-A"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> shelves = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", shelves);
        assertTrue("Should return at least one matching shelf", shelves.size() >= 1);

        for (Map<String, Object> shelf : shelves) {
            String label = (String) shelf.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("shelf-a"));
        }
    }

    @Test
    public void searchRacks_ReturnsMatching_WhenByLabel() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/racks/search").param("q", "Rack R1"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        List<Map<String, Object>> racks = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

        assertNotNull("Response should not be null", racks);
        assertTrue("Should return at least one matching rack", racks.size() >= 1);

        for (Map<String, Object> rack : racks) {
            String label = (String) rack.get("label");
            assertNotNull("Label should not be null", label);
            assertTrue("Label should contain query (case-insensitive)", label.toLowerCase().contains("rack r1"));
        }
    }
}