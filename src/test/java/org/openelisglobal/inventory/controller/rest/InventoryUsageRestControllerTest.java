package org.openelisglobal.inventory.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryUsageRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void testGetById_ValidId_ShouldReturnUsage() throws Exception {
        mockMvc.perform(get("/rest/inventory/usage/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000))
                .andExpect(jsonPath("$.quantityUsed").value(5.0));
    }

    @Test
    public void testGetByTestResultId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/usage/test-result/5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].testResultId").value(5000));
    }

    @Test
    public void testGetByLotId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/usage/lot/1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].lot.id").value(1002));
    }

    @Test
    public void testGetByItemId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/usage/item/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].inventoryItem.id").value(1001));
    }

    @Test
    public void testGetByAnalysisId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/usage/analysis/6000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].analysisId").value(6000));
    }
}
