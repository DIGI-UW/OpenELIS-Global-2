package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for SampleStorageRestController sample item responses. */
public class SampleStorageRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private ObjectMapper objectMapper;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        jdbcTemplate = new JdbcTemplate(dataSource);

        executeDataSetWithStateManagement("testdata/user-role.xml");
        executeDataSetWithStateManagement("testdata/sample-storage-integration-test-data.xml");
    }

    private void createTestStorageHierarchyWithSamples() throws Exception {
        Integer rackId = 1001;
        String externalId = "TEST-SAMPLE-2-TUBE-1";
        MvcResult assignmentResult = mockMvc.perform(post("/rest/storage/sample-items/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"sampleItemId\":\"%s\",\"locationId\":\"%d\",\"locationType\":\"rack\",\"positionCoordinate\":\"A1\",\"notes\":\"Integration test assignment\"}",
                        externalId, rackId)))
                .andReturn();

        int status = assignmentResult.getResponse().getStatus();
        String responseBody = assignmentResult.getResponse().getContentAsString();

        assertEquals("Assignment should succeed", 201, status);
        assertNotNull("Assignment response should not be null", responseBody);
    }

    @Test
    public void testGetSamples_ReturnsCompleteData_NoLazyInitializationException() throws Exception {
        createTestStorageHierarchyWithSamples();
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertNotNull("Response should not be null", responseContent);
        assertFalse("Response should not be empty", responseContent.trim().isEmpty());

        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Response should contain 'items' array", responseJson.has("items"));
        JsonNode items = responseJson.get("items");
        assertTrue("Response items should be an array", items.isArray());

        assertTrue("Response should contain at least one sample", items.size() > 0);

        JsonNode firstSample = items.get(0);
        assertNotNull("First sample should not be null", firstSample);
        assertTrue("Sample should have 'id' field", firstSample.has("id"));
        assertTrue("SampleItem should have 'sampleItemId' field", firstSample.has("sampleItemId"));
        assertTrue("Sample should have 'location' field", firstSample.has("location"));

        String location = firstSample.get("location").asText();
        assertNotNull("Location should not be null", location);
        assertFalse("Location should not be empty", location.trim().isEmpty());
        assertTrue("Location should contain hierarchical separator '>'", location.contains(">"));

        assertTrue("Location should contain room name",
                location.contains("Test Integration Room") || location.contains("Room"));
        assertTrue("Location should contain device name",
                location.contains("Test Freezer") || location.contains("Freezer"));
        assertTrue("Location should contain shelf label",
                location.contains("Test Shelf") || location.contains("Shelf"));
        assertTrue("Location should contain rack label", location.contains("Test Rack") || location.contains("Rack"));
        assertTrue("Location should contain position coordinate", location.contains("A1"));

        assertEquals("Response status should be 200", 200, result.getResponse().getStatus());
    }

    @Test
    public void testGetSamples_ReturnsCorrectDataStructure() throws Exception {
        createTestStorageHierarchyWithSamples();
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items")).andExpect(status().isOk()).andReturn();

        String responseContent = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseContent);
        assertTrue("Response should be an object", responseJson.isObject());
        assertTrue("Response should contain 'items' array", responseJson.has("items"));
        JsonNode items = responseJson.get("items");
        assertTrue("Response items should be an array", items.isArray());

        boolean foundAssignedSample = false;
        for (JsonNode sample : items) {
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
    public void testGetSamples_CountOnly_ReturnsMetrics() throws Exception {
        createTestStorageHierarchyWithSamples();
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
    public void testGetSampleItemLocation_WithValidId_ReturnsLocation() throws Exception {
        createTestStorageHierarchyWithSamples();

        String sampleItemId = "20000";

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
    public void testGetSampleItemLocation_WithUnassignedId_ReturnsEmptyLocation() throws Exception {
        String sampleItemId = "20001";

        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items/" + sampleItemId)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(responseBody);

        assertEquals("SampleItemId should match", String.valueOf(sampleItemId), response.get("sampleItemId").asText());
        String hierarchicalPath = response.get("hierarchicalPath").asText();
        assertEquals("HierarchicalPath should be empty for unassigned SampleItem", "", hierarchicalPath);
    }

    @Test
    public void testGetSampleItemLocation_WithEmptyId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items/999999")).andExpect(status().isOk());
    }
}
