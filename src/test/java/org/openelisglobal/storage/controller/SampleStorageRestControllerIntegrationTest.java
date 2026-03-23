package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class SampleStorageRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/sample-storage-integration-test-data.xml");
    }

    @Test
    public void getSamples_ShouldReturnCompleteData_NoLazyInitializationException() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertNotNull("Response should not be null", responseContent);
        assertFalse("Response should not be empty", responseContent.trim().isEmpty());

        // Get the items array from the paginated response
        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode itemsArray = responseJson.get("items");

        // Now use itemsArray for all assertions
        assertTrue("Response should be an array", itemsArray.isArray());
        assertTrue("Response should contain at least one sample", itemsArray.size() > 0);

        JsonNode firstSample = itemsArray.get(0);
        assertNotNull("First sample should not be null", firstSample);
        assertTrue("Sample should have 'id' field", firstSample.has("id"));
        assertTrue("SampleItem should have 'sampleItemId' field", firstSample.has("sampleItemId"));
        assertTrue("Sample should have 'location' field", firstSample.has("location"));

        String location = firstSample.get("location").asText();
        assertNotNull("Location should not be null", location);
        assertFalse("Location should not be empty", location.trim().isEmpty());
        assertTrue("Location should contain hierarchical separator '>'", location.contains(">"));

        assertTrue("Location should contain room name", location.contains("Test Integration Room"));
        assertTrue("Location should contain device name", location.contains("Test Freezer"));
        assertTrue("Location should contain shelf label", location.contains("Test Shelf"));
        assertTrue("Location should contain rack label", location.contains("Test Rack"));
        assertTrue("Location should contain position coordinate", location.contains("A1"));

        assertEquals("Response status should be 200", 200, result.getResponse().getStatus());
    }

    @Test
    public void getSamples_ShouldReturnCorrectDataStructure() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items")).andExpect(status().isOk()).andReturn();

        String responseContent = result.getResponse().getContentAsString();

        JsonNode responseJson = objectMapper.readTree(responseContent);
        JsonNode itemsArray = responseJson.get("items");

        boolean foundAssignedSample = false;
        for (JsonNode sample : itemsArray) {
            assertTrue("Sample should have 'id' field", sample.has("id"));
            assertTrue("SampleItem should have 'sampleItemId' field", sample.has("sampleItemId"));

            String location = sample.has("location") ? sample.get("location").asText() : "";
            if (location != null && !location.trim().isEmpty()) {
                foundAssignedSample = true;
                assertTrue("Assigned sample should have 'type' field", sample.has("type"));
                assertTrue("Assigned sample should have 'status' field", sample.has("status"));
                assertTrue("Assigned sample should have 'assignedBy' field", sample.has("assignedBy"));
                assertTrue("Assigned sample should have 'date' field", sample.has("date"));
            }
        }
        assertTrue("Should have at least one sample with storage assignment", foundAssignedSample);
    }

    @Test
    public void getSamples_ShouldReturnMetrics_WhenCountOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items?countOnly=true")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an array", responseJson.isArray());
        assertTrue("Response should contain metrics", responseJson.size() > 0);

        JsonNode metrics = responseJson.get(0);
        assertTrue("Metrics should have 'totalSampleItems' field", metrics.has("totalSampleItems"));
        assertTrue("Metrics should have 'active' field", metrics.has("active"));
        assertTrue("Metrics should have 'disposed' field", metrics.has("disposed"));
        assertTrue("Metrics should have 'storageLocations' field", metrics.has("storageLocations"));

        assertTrue("totalSampleItems should be >= 0", metrics.get("totalSampleItems").asInt() >= 0);
    }

    @Test
    public void getSampleItemLocation_ShouldReturnLocation_ForAssignedSample() throws Exception {
        String sampleItemId = "1001";

        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items/" + sampleItemId)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertNotNull("Response should not be null", response);
        assertEquals("SampleItemId should match", sampleItemId, response.get("sampleItemId").asText());
        assertTrue("Response should contain hierarchicalPath", response.has("hierarchicalPath"));
        String hierarchicalPath = response.get("hierarchicalPath").asText();
        assertNotNull("HierarchicalPath should not be null", hierarchicalPath);
        assertFalse("HierarchicalPath should not be empty", hierarchicalPath.trim().isEmpty());
        assertTrue("HierarchicalPath should contain '>' separator", hierarchicalPath.contains(">"));
    }

    @Test
    public void getSampleItemLocation_ShouldReturnEmptyLocation_ForUnassignedSample() throws Exception {
        String sampleItemId = "1002";

        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items/" + sampleItemId)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertEquals("SampleItemId should match", sampleItemId, response.get("sampleItemId").asText());
        String hierarchicalPath = response.get("hierarchicalPath").asText();
        assertEquals("HierarchicalPath should be empty for unassigned SampleItem", "", hierarchicalPath);
    }

    @Test
    public void getSampleItemLocation_ShouldReturn200_ForNonExistentId() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items/999999")).andExpect(status().isOk());
    }
}