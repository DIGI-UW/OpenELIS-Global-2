package org.openelisglobal.storage.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * OGC-657: the sample-item metrics endpoint counts sample occupants only, now
 * that an assignment row may describe an inventory lot instead.
 */
public class SampleStorageRestControllerMetricsTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleStorageService sampleStorageService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/inventory-lot-storage-test-data.xml");
        cleanRowsInCurrentConnection(new String[] { "sample_storage_movement", "sample_storage_assignment" });
    }

    @After
    public void cleanUp() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample_storage_movement", "sample_storage_assignment" });
    }

    @Test
    public void countOnlyMetrics_ignoreInventoryLotAssignments() throws Exception {
        long before = readTotalSampleItems();

        sampleStorageService.assignInventoryLotWithLocation("7000", "7000", "room", null, "metrics probe",
                TEST_SYS_USER_ID);

        assertEquals("A stored lot is not a sample item", before, readTotalSampleItems());
    }

    private long readTotalSampleItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/storage/sample-items").param("countOnly", "true"))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get(0).get("totalSampleItems").asLong();
    }
}
