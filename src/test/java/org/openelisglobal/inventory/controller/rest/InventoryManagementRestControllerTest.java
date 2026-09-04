package org.openelisglobal.inventory.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.controller.rest.InventoryManagementRestController.ConsumeRequest;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
public class InventoryManagementRestControllerTest extends BaseWebContextSensitiveTest {

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
    public void testConsumeInventory_ShouldSucceed() throws Exception {
        ConsumeRequest request = new ConsumeRequest();
        request.setItemId("1000");
        request.setQuantity(10.0);

        mockMvc.perform(post("/rest/inventory/management/consume").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedLots").isArray())
                .andExpect(jsonPath("$.consumedLots[0].quantityConsumed").value(10.0));
    }

    @Test
    public void testConsumeInventory_InsufficientStock_ShouldReturnConflict() throws Exception {
        ConsumeRequest request = new ConsumeRequest();
        request.setItemId("1000");
        request.setQuantity(1000.0);

        mockMvc.perform(post("/rest/inventory/management/consume").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession))
                .andExpect(status().isConflict());
    }

    @Test
    public void testConsumeInventory_InvalidItemId_ShouldReturn400() throws Exception {
        ConsumeRequest request = new ConsumeRequest();
        request.setItemId("abc");
        request.setQuantity(10.0);

        mockMvc.perform(post("/rest/inventory/management/consume").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession))
                .andExpect(status().isBadRequest()); // NumberFormatException is an IllegalArgumentException
    }

    @Test
    public void testConsumeInventory_NullItemId_ShouldReturn400() throws Exception {
        ConsumeRequest request = new ConsumeRequest();
        request.setItemId(null);
        request.setQuantity(10.0);

        mockMvc.perform(post("/rest/inventory/management/consume").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession))
                .andExpect(status().isBadRequest()); // Long.valueOf(null) throws NumberFormatException("null")
    }

    @Test
    public void testReceiveInventory_ShouldSucceed() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setId(1001L);

        InventoryStorageLocation location = new InventoryStorageLocation();
        location.setId(1001L);

        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setStorageLocation(location);
        lot.setLotNumber("NEW-LOT-001");
        lot.setInitialQuantity(50.0);
        lot.setCurrentQuantity(50.0);

        mockMvc.perform(post("/rest/inventory/management/receive").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lot)).session(mockSession)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lotNumber").value("NEW-LOT-001"));
    }

    @Test
    public void testReceiveInventory_MissingLotNumber_ShouldReturnBadRequest() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setId(1001L);

        InventoryLot lot = new InventoryLot();
        lot.setInventoryItem(item);
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        // lotNumber is missing

        mockMvc.perform(post("/rest/inventory/management/receive").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lot)).session(mockSession)).andExpect(status().isBadRequest());
    }

    @Test
    public void testReceiveInventory_MissingInventoryItem_ShouldReturnBadRequest() throws Exception {
        InventoryLot lot = new InventoryLot();
        lot.setLotNumber("NEW-LOT-002");
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        // inventoryItem is missing

        mockMvc.perform(post("/rest/inventory/management/receive").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lot)).session(mockSession)).andExpect(status().isBadRequest());
    }

    @Test
    public void testCheckAvailability_ShouldReturnTrue() throws Exception {
        mockMvc.perform(
                get("/rest/inventory/management/check-availability").param("itemId", "1000").param("quantity", "50.0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    public void testCheckAvailability_ShouldReturnFalse() throws Exception {
        mockMvc.perform(
                get("/rest/inventory/management/check-availability").param("itemId", "1000").param("quantity", "500.0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isAvailable").value(false));
    }

    @Test
    public void testCheckAvailability_InvalidItemId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                get("/rest/inventory/management/check-availability").param("itemId", "xyz").param("quantity", "10.0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCheckAvailability_MissingParams_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/rest/inventory/management/check-availability")).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetAlerts_ShouldReturnAlerts() throws Exception {
        mockMvc.perform(get("/rest/inventory/management/alerts").param("expirationWarningDays", "30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lowStockItems").isArray())
                .andExpect(jsonPath("$.expiringLots").isArray()).andExpect(jsonPath("$.expiredLots").isArray());
    }

    @Test
    public void testGetAlerts_DefaultBehavior_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/rest/inventory/management/alerts")).andExpect(status().isOk());
    }
}
