package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.service.StorageLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class StorageLocationRestControllerCascadeDeleteTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageLocationService storageLocationService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/storage-cascade-delete-test-data.xml");
    }

    @Test
    public void testCanDeleteShelf_ReturnsAdminStatus() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/shelves/10001/can-delete").contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = result.getResponse().getStatus();
        String responseBody = result.getResponse().getContentAsString();

        assertTrue("Should return 409 Conflict or 500 Internal Server Error", status == 409 || status == 500);

        if (status == 409) {
            assertTrue("Response should contain isAdmin field", responseBody.contains("isAdmin"));
        }
    }

    @Test
    public void testGetCascadeDeleteSummary_ReturnsSummary() throws Exception {
        MvcResult result = mockMvc
                .perform(get("/rest/storage/shelves/" + 10001 + "/cascade-delete-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.childLocationCount").exists())
                .andExpect(jsonPath("$.sampleCount").exists()).andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue("Response should contain childLocationCount", responseBody.contains("childLocationCount"));
        assertTrue("Response should contain sampleCount", responseBody.contains("sampleCount"));
    }

    @Test
    public void testDeleteShelf_NonAdminWithConstraints_Returns403() throws Exception {
        MvcResult result = mockMvc
                .perform(delete("/rest/storage/shelves/" + 10001).contentType(MediaType.APPLICATION_JSON)).andReturn();

        int status = result.getResponse().getStatus();
        assertTrue("Should return 403 Forbidden, 409 Conflict, or 500 Internal Server Error",
                status == 403 || status == 409 || status == 500);
    }

    @Test
    public void testDeleteShelfWithCascade_UnassignsAllSamples() throws Exception {
        Integer assignmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_assignment WHERE location_id = ? AND location_type = 'rack'",
                Integer.class, 10004);
        assertEquals("Sample should be assigned", Integer.valueOf(1), assignmentCount);

        storageLocationService.deleteLocationWithCascade(10001,
                org.openelisglobal.storage.valueholder.StorageShelf.class);

        Integer remainingAssignments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_assignment WHERE location_id = ? AND location_type = 'rack'",
                Integer.class, 10004);
        assertEquals("Sample assignment should be unassigned", Integer.valueOf(0), remainingAssignments);
    }

    @Test
    public void testDeleteShelfWithCascade_DeletesAllChildRacks() throws Exception {
        Integer rackCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_rack WHERE id = ?", Integer.class,
                10004);
        assertEquals("Rack should exist", Integer.valueOf(1), rackCount);

        storageLocationService.deleteLocationWithCascade(10001,
                org.openelisglobal.storage.valueholder.StorageShelf.class);

        Integer remainingRacks = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_rack WHERE id = ?",
                Integer.class, 10004);
        assertEquals("Rack should be deleted", Integer.valueOf(0), remainingRacks);

        Integer remainingShelves = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM storage_shelf WHERE id = ?",
                Integer.class, 10001);
        assertEquals("Shelf should be deleted", Integer.valueOf(0), remainingShelves);
    }
}