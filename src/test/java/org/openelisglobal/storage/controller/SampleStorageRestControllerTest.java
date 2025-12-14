package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.storage.BaseStorageTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for pagination functionality in
 * SampleStorageRestController. Tests verify that pagination parameters are
 * correctly handled and responses include pagination metadata.
 * 
 * Extends BaseStorageTest to load storage hierarchy and E2E test fixtures.
 */
@Rollback
public class SampleStorageRestControllerTest extends BaseStorageTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(); // BaseStorageTest loads storage-e2e.xml with test data
    }

    @Test
    public void testGetSampleItems_WithPaginationParams_ReturnsPagedResults() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items").param("page", "0").param("size", "25")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(25)).andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.totalItems").exists());
    }

    @Test
    public void testGetSampleItems_DefaultParams_Returns25Items() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(25));
    }

    @Test
    public void testGetSampleItems_CustomPageSize_ReturnsSpecifiedSize() throws Exception {
        // Test page size 50
        mockMvc.perform(get("/rest/storage/sample-items").param("page", "0").param("size", "50")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(50));

        // Test page size 100
        mockMvc.perform(get("/rest/storage/sample-items").param("page", "0").param("size", "100")
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize").value(100));
    }

    @Test
    public void testGetSampleItems_InvalidPageSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/rest/storage/sample-items").param("page", "0").param("size", "75") // Invalid - not 25,
                                                                                                 // 50, or 100
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testGetSampleItems_WithStatusFilterActive_ReturnsFilteredResults() throws Exception {
        // Test active filter
        mockMvc.perform(get("/rest/storage/sample-items").param("status", "active").param("page", "0")
                .param("size", "25").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.totalItems").exists());
    }

    @Test
    public void testGetSampleItems_WithStatusFilterDisposed_ReturnsFilteredResults() throws Exception {
        // Test disposed filter
        mockMvc.perform(get("/rest/storage/sample-items").param("status", "disposed").param("page", "0")
                .param("size", "25").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray()).andExpect(jsonPath("$.totalItems").exists());
    }

    @Test
    public void testGetSampleItems_AssignedSamplesHaveLocation() throws Exception {
        // CRITICAL: Verify that sample items with storage assignments have location
        // populated
        // This prevents the "location not showing" bug discovered during manual testing
        String response = mockMvc
                .perform(get("/rest/storage/sample-items").param("page", "0").param("size", "25")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray()).andReturn().getResponse()
                .getContentAsString();

        // Parse response and verify at least one assigned sample has non-empty location
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode items = root.get("items");

        boolean foundAssignedWithLocation = false;
        for (com.fasterxml.jackson.databind.JsonNode item : items) {
            String location = item.has("location") ? item.get("location").asText() : "";
            // If location is not empty, it means sample is assigned and location path was
            // built
            if (location != null && !location.isEmpty() && !location.equals("Unassigned")) {
                foundAssignedWithLocation = true;
                break;
            }
        }

        assertTrue("At least one assigned sample should have a non-empty location field (hierarchical path)",
                foundAssignedWithLocation);
    }
}
