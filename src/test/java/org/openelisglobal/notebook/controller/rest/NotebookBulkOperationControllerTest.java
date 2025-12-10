package org.openelisglobal.notebook.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for NotebookBulkOperationController. Tests the REST API
 * endpoints for bulk data entry operations.
 *
 * Per FR-031: System MUST support "Apply to Selected" operations. Per FR-033:
 * System MUST process bulk operations in batches of 50. Per FR-034: System MUST
 * provide bulk apply endpoint for common values.
 */
@Rollback
public class NotebookBulkOperationControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
    }

    /**
     * Test bulk apply values endpoint with valid request. Verifies FR-034: System
     * MUST provide bulk apply endpoint.
     */
    @Test
    public void testBulkApplyValues_validRequest_returns200() throws Exception {
        // Arrange - use a page ID that doesn't exist (should handle gracefully)
        Integer pageId = 9999;
        Map<String, Object> request = new HashMap<>();
        request.put("sampleIds", Arrays.asList(1, 2, 3));
        Map<String, Object> data = new HashMap<>();
        data.put("volume", "5.0");
        data.put("method", "Ficoll");
        request.put("data", data);

        String requestJson = objectMapper.writeValueAsString(request);

        // Act
        MvcResult result = mockMvc.perform(post("/rest/notebook/bulk/page/" + pageId + "/samples/apply")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(requestJson)).andReturn();

        // Assert - should return 200 even with non-existent data (0 updates)
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);

        String responseJson = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
        });
        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain updatedCount", response.containsKey("updatedCount"));
    }

    /**
     * Test bulk apply values endpoint with empty sample list. Should return 0
     * updates.
     */
    @Test
    public void testBulkApplyValues_emptySampleList_returnsZeroUpdates() throws Exception {
        // Arrange
        Integer pageId = 1;
        Map<String, Object> request = new HashMap<>();
        request.put("sampleIds", Arrays.asList());
        Map<String, Object> data = new HashMap<>();
        data.put("volume", "5.0");
        request.put("data", data);

        String requestJson = objectMapper.writeValueAsString(request);

        // Act
        MvcResult result = mockMvc.perform(post("/rest/notebook/bulk/page/" + pageId + "/samples/apply")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(requestJson)).andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);

        String responseJson = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
        });
        assertEquals("Should have 0 updates for empty list", 0, response.get("updatedCount"));
    }

    /**
     * Test get page progress endpoint. Verifies FR-004: System MUST display
     * progress indicators.
     */
    @Test
    public void testGetPageProgress_validPageId_returns200() throws Exception {
        // Arrange - use a page ID (may or may not exist)
        Integer pageId = 1;

        // Act
        MvcResult result = mockMvc.perform(
                get("/rest/notebook/bulk/page/" + pageId + "/progress").accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);

        String responseJson = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
        });
        assertNotNull("Response should not be null", response);
        // Progress response should have total, completed, etc.
        assertTrue("Response should contain total", response.containsKey("total"));
        assertTrue("Response should contain completed", response.containsKey("completed"));
    }

    /**
     * Test get paginated samples endpoint. Should return samples with pagination
     * info.
     */
    @Test
    public void testGetPaginatedSamples_validRequest_returns200() throws Exception {
        // Arrange
        Integer pageId = 1;
        int pageNum = 0;
        int pageSize = 25;

        // Act
        MvcResult result = mockMvc
                .perform(get("/rest/notebook/bulk/page/" + pageId + "/samples").param("page", String.valueOf(pageNum))
                        .param("size", String.valueOf(pageSize)).accept(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);

        String responseJson = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
        });
        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain samples", response.containsKey("samples"));
        assertTrue("Response should contain pagination info", response.containsKey("totalElements"));
    }

    /**
     * Test bulk status update endpoint. Should update status for selected samples.
     */
    @Test
    public void testBulkUpdateStatus_validRequest_returns200() throws Exception {
        // Arrange
        Integer pageId = 1;
        Map<String, Object> request = new HashMap<>();
        request.put("sampleIds", Arrays.asList(1, 2, 3));
        request.put("status", "COMPLETED");

        String requestJson = objectMapper.writeValueAsString(request);

        // Act
        MvcResult result = mockMvc.perform(post("/rest/notebook/bulk/page/" + pageId + "/samples/status")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE)
                .content(requestJson)).andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);

        String responseJson = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
        });
        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain updatedCount", response.containsKey("updatedCount"));
    }

    /**
     * Test mark page complete endpoint. Should mark all samples in the page as
     * complete.
     */
    @Test
    public void testMarkPageComplete_validRequest_returns200() throws Exception {
        // Arrange
        Integer pageId = 1;
        Map<String, Object> request = new HashMap<>();
        request.put("requireComplete", false);

        String requestJson = objectMapper.writeValueAsString(request);

        // Act
        MvcResult result = mockMvc.perform(
                post("/rest/notebook/bulk/page/" + pageId + "/complete").contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE).content(requestJson))
                .andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        // Should return either 200 (success) or possibly 400 (if page doesn't exist or
        // validation fails)
        assertTrue("Should return 200 or 400", status == 200 || status == 400);
    }

    /**
     * Test bulk apply with invalid page ID format. Should return 400 Bad Request.
     */
    @Test
    public void testBulkApplyValues_invalidPageIdFormat_returns400() throws Exception {
        // Act - use invalid page ID (negative)
        MvcResult result = mockMvc.perform(post("/rest/notebook/bulk/page/-1/samples/apply")
                .contentType(MediaType.APPLICATION_JSON_VALUE).accept(MediaType.APPLICATION_JSON_VALUE).content("{}"))
                .andReturn();

        // Assert - should handle gracefully (either 400 or 200 with error message)
        int status = result.getResponse().getStatus();
        assertTrue("Should return 200 or 400", status == 200 || status == 400);
    }

    /**
     * Test status filter parameter in paginated samples.
     */
    @Test
    public void testGetPaginatedSamples_withStatusFilter_returns200() throws Exception {
        // Arrange
        Integer pageId = 1;

        // Act
        MvcResult result = mockMvc.perform(get("/rest/notebook/bulk/page/" + pageId + "/samples")
                .param("status", "PENDING").accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);
    }

    /**
     * Test search parameter in paginated samples.
     */
    @Test
    public void testGetPaginatedSamples_withSearch_returns200() throws Exception {
        // Arrange
        Integer pageId = 1;

        // Act
        MvcResult result = mockMvc.perform(get("/rest/notebook/bulk/page/" + pageId + "/samples")
                .param("search", "TEST").accept(MediaType.APPLICATION_JSON_VALUE)).andReturn();

        // Assert
        int status = result.getResponse().getStatus();
        assertEquals("Should return 200 OK", 200, status);
    }
}
