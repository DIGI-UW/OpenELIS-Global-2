package org.openelisglobal.inventory.controller.rest;

import static org.hamcrest.CoreMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
public class InventoryTransactionRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void testGetById_ValidId_ShouldReturnTransaction() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/1000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000)).andExpect(jsonPath("$.transactionType").value("RECEIPT"))
                .andExpect(jsonPath("$.quantityChange").value(100.0)).andExpect(jsonPath("$.lot.id").value(1000));
    }

    @Test
    public void testGetById_InvalidId_ShouldReturn404() throws Exception {
        // BaseObjectService throws an exception if the entity is not found by ID in
        // this project's configuration
        mockMvc.perform(get("/rest/inventory/transactions/9999")).andExpect(status().isNotFound());
    }

    @Test
    public void testGetById_NonNumericId_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/abc")).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetByLotId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/lot/1000")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0].lot.id").value(1000));
    }

    @Test
    public void testGetByType_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/type/RECEIPT")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$[0].transactionType").value("RECEIPT"));
    }

    @Test
    public void testGetByType_InvalidEnum_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/type/INVALID_TYPE")).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetByDateRange_WithData_ShouldReturnNonEmptyList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/date-range").param("startDate", "2025-01-01").param("endDate",
                "2025-12-31")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(not(0)));
    }

    @Test
    public void testGetByDateRange_MissingParams_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/date-range")).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetByReference_ShouldReturnList() throws Exception {
        // Since we don't have reference data in the basic test set, we just expect 200
        // and an array
        mockMvc.perform(get("/rest/inventory/transactions/reference").param("referenceId", "1").param("referenceType",
                "MANUAL")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetByReference_MissingParams_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/reference")).andExpect(status().isBadRequest());
    }
}
