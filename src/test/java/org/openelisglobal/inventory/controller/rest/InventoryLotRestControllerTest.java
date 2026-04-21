package org.openelisglobal.inventory.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.QCStatusRequest;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryLotRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");

        objectMapper = new ObjectMapper();

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
    }

    @Test
    public void testGetAll_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetById_ValidId_ShouldReturnLot() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotNumber").value("LOT-2025-001"));
    }

    @Test
    public void testGetAvailableLotsFEFO_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testUpdateQCStatus_ShouldSucceed() throws Exception {
        QCStatusRequest request = new QCStatusRequest();
        request.setQcStatus(QCStatus.PASSED);
        request.setNotes("Tested and passed");

        mockMvc.perform(put("/rest/inventory/lots/1000/qc-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qcStatus").value("PASSED"));
    }

    @Test
    public void testGetTotalQuantity_ShouldReturnCorrectValue() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000/total-quantity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(150.0));
    }
}
