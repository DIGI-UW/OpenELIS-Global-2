package org.openelisglobal.inventory.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryTransactionRestControllerTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
    }

    @Test
    public void testGetById_ValidId_ShouldReturnTransaction() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000))
                .andExpect(jsonPath("$.transactionType").value("RECEIPT"));
    }

    @Test
    public void testGetByLotId_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/lot/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].lotId").value(1000));
    }

    @Test
    public void testGetByType_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/type/RECEIPT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].transactionType").value("RECEIPT"));
    }

    @Test
    public void testGetByDateRange_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/transactions/date-range")
                .param("startDate", "2025-01-01")
                .param("endDate", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetByReference_ShouldReturnList() throws Exception {
        // Since we don't have reference data in the basic test set, we just expect 200 and an array
        mockMvc.perform(get("/rest/inventory/transactions/reference")
                .param("referenceId", "1")
                .param("referenceType", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
