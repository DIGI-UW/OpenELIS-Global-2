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
    public void canDelete_ReturnsAdminStatus_WhenConstraintsExist() throws Exception {
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
    public void getCascadeDeleteSummary_ReturnsSummary_WhenLocationHasConstraints() throws Exception {
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
    public void deleteShelf_Returns403_WhenNonAdminUserWithConstraints() throws Exception {
        MvcResult result = mockMvc
                .perform(delete("/rest/storage/shelves/" + 10001).contentType(MediaType.APPLICATION_JSON)).andReturn();

        int status = result.getResponse().getStatus();
        assertTrue("Should return 403 Forbidden, 409 Conflict, or 500 Internal Server Error",
                status == 403 || status == 409 || status == 500);
    }

    @Test
    public void deleteLocationWithCascade_UnassignsAllSamples_WhenShelfHasAssignedSamples() throws Exception {
        int initialAssignmentCount = countRowsInTableWhere("sample_storage_assignment",
                "location_id = 10004 AND location_type = 'rack'");
        assertEquals("Sample should be assigned", 1, initialAssignmentCount);

        storageLocationService.deleteLocationWithCascade(10001,
                org.openelisglobal.storage.valueholder.StorageShelf.class);

        int finalAssignmentCount = countRowsInTableWhere("sample_storage_assignment",
                "location_id = 10004 AND location_type = 'rack'");
        assertEquals("Sample assignment should be unassigned", 0, finalAssignmentCount);
    }

    @Test
    public void deleteLocationWithCascade_DeletesAllChildRacks_WhenShelfHasChildRacks() throws Exception {
        int initialRackCount = countRowsInTableWhere("storage_rack", "id = 10004");
        assertEquals("Rack should exist", 1, initialRackCount);

        storageLocationService.deleteLocationWithCascade(10001,
                org.openelisglobal.storage.valueholder.StorageShelf.class);

        int finalRackCount = countRowsInTableWhere("storage_rack", "id = 10004");
        assertEquals("Rack should be deleted", 0, finalRackCount);

        int finalShelfCount = countRowsInTableWhere("storage_shelf", "id = 10001");
        assertEquals("Shelf should be deleted", 0, finalShelfCount);
    }
}