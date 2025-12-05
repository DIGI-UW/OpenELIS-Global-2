package org.openelisglobal.storage.controller;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;

/**
 * Integration tests for pagination functionality in
 * SampleStorageRestController. Tests verify that pagination parameters are
 * correctly handled and responses include pagination metadata.
 */
@Rollback
public class SampleStorageRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
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
}
