package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class InventoryReagentRestControllerInstrumentsTest {

    @InjectMocks
    private InventoryReagentRestController controller;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private DepartmentIsolationService departmentIsolationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getInstrumentsReturnsOnlyEquipmentWhenItemTypeFilterIsEquipment() throws Exception {
        InventoryItem equipment = buildItem(1L, "Centrifuge", ItemType.EQUIPMENT);

        when(inventoryItemService.getByItemType(ItemType.EQUIPMENT)).thenReturn(List.of(equipment));
        when(inventoryLotService.getAvailableLotsByItemFEFO(any())).thenReturn(List.of(new InventoryLot()));
        when(departmentIsolationService.canAccessInventoryItem(any(), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "false")
                        .param("itemTypes", "EQUIPMENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertEquals("Centrifuge", body.get(0).get("name").asText());
        assertEquals("EQUIPMENT", body.get(0).get("itemType").asText());
    }

    @Test
    public void getInstrumentsDefaultsToEquipmentOnlyWhenItemTypesOmitted() throws Exception {
        InventoryItem equipment = buildItem(10L, "Freezer", ItemType.EQUIPMENT);

        when(inventoryItemService.getByItemType(ItemType.EQUIPMENT)).thenReturn(List.of(equipment));
        when(inventoryLotService.getAvailableLotsByItemFEFO(any())).thenReturn(List.of(new InventoryLot()));
        when(departmentIsolationService.canAccessInventoryItem(any(), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "false")
                        .session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(body.size() >= 1);
        for (JsonNode row : body) {
            assertEquals("EQUIPMENT", row.get("itemType").asText());
        }
    }

    @Test
    public void getInstrumentsHonorsDepartmentFilterForRestrictedScope() throws Exception {
        InventoryItem equipment = buildItem(20L, "Plate reader", ItemType.EQUIPMENT);
        equipment.setDepartmentTestSectionId(7);

        when(inventoryItemService.getByItemType(ItemType.EQUIPMENT)).thenReturn(List.of(equipment));
        when(inventoryLotService.getAvailableLotsByItemFEFO(eq(20L))).thenReturn(List.of(new InventoryLot()));
        when(departmentIsolationService.canAccessInventoryItem(eq(equipment), any())).thenReturn(true);
        when(departmentIsolationService.inventoryBelongsToDepartment(eq(equipment), eq(7))).thenReturn(true);
        when(departmentIsolationService.inventoryBelongsToDepartment(eq(equipment), eq(99))).thenReturn(false);

        MvcResult allowed = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "false")
                        .param("itemTypes", "EQUIPMENT").param("departmentIds", "7").session(buildSession())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        assertEquals(1, objectMapper.readTree(allowed.getResponse().getContentAsString()).size());

        MvcResult denied = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "false")
                        .param("itemTypes", "EQUIPMENT").param("departmentIds", "99").session(buildSession())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
        assertEquals(0, objectMapper.readTree(denied.getResponse().getContentAsString()).size());
    }

    private InventoryItem buildItem(Long id, String name, ItemType type) {
        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setName(name);
        item.setItemType(type);
        item.setIsActive("Y");
        return item;
    }

    private MockHttpSession buildSession() {
        MockHttpSession session = new MockHttpSession();
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        usd.setLoginLabUnit(7);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }
}
