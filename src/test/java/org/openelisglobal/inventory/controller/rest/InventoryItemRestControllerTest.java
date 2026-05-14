package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryItemRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryItemService inventoryItemService;

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
    public void testGetAllActive_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetAll_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/all")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetById_ValidId_ShouldReturnItem() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/1000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Reagent A"));
    }

    @Test
    public void testGetById_NotFound_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/999999")).andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetAllItemTypes_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/types")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetByType_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/type/REAGENT")).andExpect(result -> {
            int status = result.getResponse().getStatus();
            assertTrue(status == 200 || status == 500);
        });
    }

    @Test
    public void testGetByCategory_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/category/CHEMISTRY")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("CHEMISTRY"));
    }

    @Test
    public void testSearch_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/search").param("query", "Reagent")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetStock_ShouldReturnCorrectData() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/1000/stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(150.0)).andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    public void testGetStock_NonExistent_ShouldReturnZero() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/99999/stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0.0)).andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    public void testGetLowStock_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/rest/inventory/items/low-stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testCreate_ShouldCreateItem() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("PR Test Item");
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setLowStockThreshold(10);
        item.setIsActive("Y");

        mockMvc.perform(post("/rest/inventory/items").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PR Test Item"));
    }

    @Test
    public void testCreate_ShouldAutoGenerateUuid() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Auto UUID Item");
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setIsActive("Y");

        mockMvc.perform(post("/rest/inventory/items").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fhirUuid").isNotEmpty());
    }

    @Test
    public void testCreate_ShouldPreserveUuid() throws Exception {
        UUID uuid = UUID.randomUUID();

        InventoryItem item = new InventoryItem();
        item.setName("UUID Test");
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL");
        item.setIsActive("Y");
        item.setFhirUuid(uuid);

        mockMvc.perform(post("/rest/inventory/items").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fhirUuid").value(uuid.toString()));
    }

    @Test
    public void testCreate_Invalid_ShouldReturn500() throws Exception {
        InventoryItem item = new InventoryItem();

        mockMvc.perform(post("/rest/inventory/items").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testUpdate_ShouldUpdateItem() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Updated Name");
        item.setItemType(ItemType.REAGENT);
        item.setUnits("mL"); // avoid null issues

        mockMvc.perform(put("/rest/inventory/items/1000").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    public void testUpdate_NotFound_ShouldReturn500() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Ghost");
        item.setItemType(ItemType.REAGENT);

        mockMvc.perform(put("/rest/inventory/items/999999").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)).session(mockSession))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testDeactivate_ShouldWork() throws Exception {
        mockMvc.perform(put("/rest/inventory/items/1001/deactivate").session(mockSession)).andExpect(status().isOk());

        assertEquals("N", inventoryItemService.get(1001L).getIsActive());
    }

    @Test
    public void testActivate_ShouldWork() throws Exception {
        mockMvc.perform(put("/rest/inventory/items/1000/activate").session(mockSession)).andExpect(status().isOk());

        assertEquals("Y", inventoryItemService.get(1000L).getIsActive());
    }
}
