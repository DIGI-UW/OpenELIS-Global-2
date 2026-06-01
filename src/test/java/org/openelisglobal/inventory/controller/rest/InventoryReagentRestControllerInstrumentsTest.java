package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
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
        when(inventoryLotService.getByInventoryItemId(any())).thenReturn(List.of(new InventoryLot()));
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
        when(inventoryLotService.getByInventoryItemId(any())).thenReturn(List.of(new InventoryLot()));
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
        when(inventoryLotService.getByInventoryItemId(eq(20L))).thenReturn(List.of(new InventoryLot()));
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

    @Test
    public void getInstrumentsIncludesPendingLotsForNotebookSelectorsWhenLotsRequired() throws Exception {
        InventoryItem reagent = buildItem(30L, "Prep Reagent", ItemType.REAGENT);
        InventoryLot pendingLot = new InventoryLot();
        pendingLot.setId(501L);
        pendingLot.setLotNumber("LOT-501");
        pendingLot.setStatus(LotStatus.ACTIVE);
        pendingLot.setQcStatus(QCStatus.PENDING);
        pendingLot.setCurrentQuantity(10.0);

        when(inventoryItemService.getByItemType(ItemType.REAGENT)).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId(30L)).thenReturn(List.of(pendingLot));
        when(departmentIsolationService.canAccessInventoryItem(eq(reagent), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "true")
                        .param("itemTypes", "REAGENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertEquals("Prep Reagent", body.get(0).get("name").asText());
        assertEquals("PENDING", body.get(0).get("qcStatus").asText());
    }

    @Test
    public void getInstrumentsIncludesFailedQcLotsForNotebookSelectors() throws Exception {
        InventoryItem reagent = buildItem(40L, "Failed QC Reagent", ItemType.REAGENT);
        InventoryLot failedLot = buildLot(601L, "LOT-601", LotStatus.ACTIVE, QCStatus.FAILED, 5.0);

        when(inventoryItemService.getByItemType(ItemType.REAGENT)).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId(40L)).thenReturn(List.of(failedLot));
        when(departmentIsolationService.canAccessInventoryItem(eq(reagent), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "true")
                        .param("itemTypes", "REAGENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertEquals("FAILED", body.get(0).get("qcStatus").asText());
        assertTrue(body.get(0).get("selectionWarnings").toString().contains("QC_FAILED"));
    }

    @Test
    public void getInstrumentsIncludesZeroQuantityLotsForNotebookSelectors() throws Exception {
        InventoryItem reagent = buildItem(41L, "Depleted Reagent", ItemType.REAGENT);
        InventoryLot emptyLot = buildLot(602L, "LOT-602", LotStatus.ACTIVE, QCStatus.PASSED, 0.0);

        when(inventoryItemService.getByItemType(ItemType.REAGENT)).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId(41L)).thenReturn(List.of(emptyLot));
        when(departmentIsolationService.canAccessInventoryItem(eq(reagent), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "true")
                        .param("itemTypes", "REAGENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertTrue(body.get(0).get("selectionWarnings").toString().contains("ZERO_QUANTITY"));
    }

    @Test
    public void getInstrumentsIncludesInactiveLotsForNotebookSelectors() throws Exception {
        InventoryItem reagent = buildItem(42L, "Expired Lot Reagent", ItemType.REAGENT);
        InventoryLot expiredLot = buildLot(603L, "LOT-603", LotStatus.EXPIRED, QCStatus.PASSED, 12.0);

        when(inventoryItemService.getByItemType(ItemType.REAGENT)).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId(42L)).thenReturn(List.of(expiredLot));
        when(departmentIsolationService.canAccessInventoryItem(eq(reagent), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "true")
                        .param("itemTypes", "REAGENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertTrue(body.get(0).get("selectionWarnings").toString().contains("INACTIVE_LOT"));
    }

    @Test
    public void getInstrumentsIncludesItemsWithoutLotsForNotebookSelectors() throws Exception {
        InventoryItem reagent = buildItem(43L, "No Lot Reagent", ItemType.REAGENT);

        when(inventoryItemService.getByItemType(ItemType.REAGENT)).thenReturn(List.of(reagent));
        when(inventoryLotService.getByInventoryItemId(43L)).thenReturn(List.of());
        when(departmentIsolationService.canAccessInventoryItem(eq(reagent), any())).thenReturn(true);

        MvcResult result = mockMvc
                .perform(get("/rest/inventory/instruments").param("status", "active").param("requireLots", "true")
                        .param("itemTypes", "REAGENT").session(buildSession()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(1, body.size());
        assertTrue(body.get(0).get("selectionWarnings").toString().contains("NO_LOTS"));
        assertFalse(body.get(0).has("lotNumber"));
    }

    private InventoryLot buildLot(Long id, String lotNumber, LotStatus status, QCStatus qcStatus, Double quantity) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        lot.setLotNumber(lotNumber);
        lot.setStatus(status);
        lot.setQcStatus(qcStatus);
        lot.setCurrentQuantity(quantity);
        return lot;
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
