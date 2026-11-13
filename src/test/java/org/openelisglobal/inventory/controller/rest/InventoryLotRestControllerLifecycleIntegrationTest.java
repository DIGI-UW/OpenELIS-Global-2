package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.AdjustQuantityRequest;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.DisposeRequest;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.OpenLotRequest;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.QCStatusRequest;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.StatusRequest;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

public class InventoryLotRestControllerLifecycleIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryLotService inventoryLotService;

    private ObjectMapper objectMapper;
    private MockHttpSession mockSession;
    private UserSessionData userSessionData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/inventory-test-data.xml");
        objectMapper = new ObjectMapper();

        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        mockSession = new MockHttpSession();
        mockSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
    }

    private String lotJson(String lotNumber, Long itemId, Double initialQuantity, Double currentQuantity,
            QCStatus qcStatus, LotStatus status, Long receiptDate, Long expirationDate, Integer version)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("lotNumber", lotNumber);
        body.put("inventoryItem", Map.of("id", itemId));
        body.put("initialQuantity", initialQuantity);
        body.put("currentQuantity", currentQuantity);
        if (qcStatus != null) {
            body.put("qcStatus", qcStatus.name());
        }
        if (status != null) {
            body.put("status", status.name());
        }
        if (receiptDate != null) {
            body.put("receiptDate", receiptDate);
        }
        if (expirationDate != null) {
            body.put("expirationDate", expirationDate);
        }
        if (version != null) {
            body.put("version", version);
        }
        return objectMapper.writeValueAsString(body);
    }

    @Test
    public void testGetById_WithInvalidIdFormat_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/invalid-id").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetById_WithInvalidId_ShouldReturnInternalServerError() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/99999").contentType(MediaType.APPLICATION_JSON).session(mockSession))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetByItemId_WithValidItemId_ShouldReturnLotsForItem() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].inventoryItem.id").value(1000))
                .andExpect(jsonPath("$[1].inventoryItem.id").value(1000));
    }

    @Test
    public void testGetByItemId_WithInvalidItemId_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/99999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetAvailableLotsFEFO_ShouldReturnLotsInFEFOOrder() throws Exception {
        long dayMs = 86400000L;
        jdbcTemplate.update("UPDATE inventory_lot SET expiration_date = ? WHERE id = 1001",
                new Timestamp(System.currentTimeMillis() + 30 * dayMs));
        jdbcTemplate.update("UPDATE inventory_lot SET expiration_date = ? WHERE id = 1000",
                new Timestamp(System.currentTimeMillis() + 90 * dayMs));

        mockMvc.perform(get("/rest/inventory/lots/item/1000/available").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1001))
                .andExpect(jsonPath("$[1].id").value(1000)).andExpect(jsonPath("$[0].qcStatus").value("PASSED"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    public void testGetAvailableLotsFEFO_ShouldReturnOnlyAvailableLots() throws Exception {
        jdbcTemplate.update("UPDATE inventory_lot SET expiration_date = ? WHERE id = 1000",
                new Timestamp(System.currentTimeMillis() + 90L * 86400000L));

        mockMvc.perform(get("/rest/inventory/lots/item/1000/available").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1000));
    }

    @Test
    public void testGetByLotNumber_WithValidLotNumber_ShouldReturnLot() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/lot-number/LOT-2025-001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lotNumber").value("LOT-2025-001"))
                .andExpect(jsonPath("$.id").value(1000));
    }

    @Test
    public void testGetByLotNumber_WithInvalidLotNumber_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/lot-number/INVALID-LOT").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetTotalQuantity_WithValidItemId_ShouldReturnTotalQuantity() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000/total-quantity").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(150.0));
    }

    @Test
    public void testGetTotalQuantity_WithItemHavingNoLots_ShouldReturnZero() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/99999/total-quantity").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(0.0));
    }

    @Test
    public void testGetExpiringLots_ShouldReturnExpiringLots() throws Exception {
        jdbcTemplate.update("UPDATE inventory_lot SET expiration_date = ? WHERE id = 1001",
                new Timestamp(System.currentTimeMillis() + 10L * 86400000L));

        mockMvc.perform(get("/rest/inventory/lots/expiring?days=30").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].lotNumber").value("LOT-2025-002"));
    }

    @Test
    public void testGetExpiringLots_WithDefaultDays_ShouldUseDefault() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/expiring").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetExpiredActiveLots_ShouldReturnExpiredLotsOnly() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/expired").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testCreate_WithValidLot_ShouldCreateAndReturnLot() throws Exception {
        long now = System.currentTimeMillis();
        String body = lotJson("LOT-2025-NEW", 1000L, 50.0, 50.0, QCStatus.PASSED, LotStatus.ACTIVE, now,
                now + 86400000L * 365, null);

        MvcResult result = mockMvc
                .perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON).content(body)
                        .session(mockSession))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lotNumber").value("LOT-2025-NEW"))
                .andExpect(jsonPath("$.currentQuantity").value(50.0)).andExpect(jsonPath("$.fhirUuid").isNotEmpty())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotNull("Created lot should have ID", jsonNode.get("id"));
        assertNotNull("Created lot should have FHIR UUID", jsonNode.get("fhirUuid"));
    }

    @Test
    public void testCreate_WithoutFhirUuid_ShouldGenerateUUID() throws Exception {
        long now = System.currentTimeMillis();
        String body = lotJson("LOT-2025-UUID", 1000L, 25.0, 25.0, QCStatus.PASSED, LotStatus.ACTIVE, now,
                now + 86400000L * 365, null);

        mockMvc.perform(
                post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON).content(body).session(mockSession))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.fhirUuid").isNotEmpty());
    }

    @Test
    public void testCreate_WithInvalidItemId_ShouldReturnNotFound() throws Exception {
        String body = lotJson("LOT-2025-INVALID", 99999L, 50.0, 50.0, QCStatus.PASSED, LotStatus.ACTIVE, null, null,
                null);

        mockMvc.perform(
                post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON).content(body).session(mockSession))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreate_WithoutUserSession_ShouldHandleException() throws Exception {
        String body = lotJson("LOT-NO-SESSION", 1000L, 50.0, 50.0, null, null, null, null, null);

        mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testCreateMultipleLots_ForSameItem_ShouldAllowMultipleLots() throws Exception {
        long now = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            double qty = 10.0 * (i + 1);
            String body = lotJson("LOT-MULTI-" + i, 1000L, qty, qty, QCStatus.PASSED, LotStatus.ACTIVE, now,
                    now + 86400000L * 365, null);

            mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON).content(body)
                    .session(mockSession)).andExpect(status().isCreated());
        }
    }

    @Test
    public void testOpenLot_WithValidLotId_ShouldOpenLotAndReturnUpdated() throws Exception {
        OpenLotRequest openRequest = new OpenLotRequest();
        openRequest.setOpenedDate(new Timestamp(System.currentTimeMillis()));

        mockMvc.perform(post("/rest/inventory/lots/1000/open").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(openRequest)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000)).andExpect(jsonPath("$.status").value("IN_USE"));
    }

    @Test
    public void testOpenLot_WithoutOpenedDate_ShouldUseCurrentDate() throws Exception {
        mockMvc.perform(post("/rest/inventory/lots/1000/open").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new OpenLotRequest())).session(mockSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_USE"));
    }

    @Test
    public void testProcessExpired_WhenNoExpiredActiveLots_ShouldReturnZeroUpdated() throws Exception {
        jdbcTemplate.update("UPDATE inventory_lot SET expiration_date = ?",
                new Timestamp(System.currentTimeMillis() + 365L * 86400000L));

        mockMvc.perform(post("/rest/inventory/lots/process-expired").contentType(MediaType.APPLICATION_JSON)
                .session(mockSession)).andExpect(status().isOk()).andExpect(jsonPath("$.lotsUpdated").value(0));
    }

    @Test
    public void testUpdate_PreservesFhirUuid() throws Exception {
        InventoryLot originalLot = inventoryLotService.get(1000L);
        UUID originalUUID = originalLot.getFhirUuid();

        String body = lotJson("LOT-2025-001-UPDATED", 1000L, 100.0, 80.0, QCStatus.PASSED, LotStatus.ACTIVE, null, null,
                originalLot.getVersion());

        mockMvc.perform(put("/rest/inventory/lots/1000").contentType(MediaType.APPLICATION_JSON).content(body)
                .session(mockSession)).andExpect(status().isOk());

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("FHIR UUID should be preserved", originalUUID, updatedLot.getFhirUuid());
    }

    @Test
    public void testUpdate_WithInvalidId_ShouldReturnNotFound() throws Exception {
        String body = lotJson("LOT-INVALID", 1000L, 50.0, 50.0, null, null, null, null, null);

        mockMvc.perform(put("/rest/inventory/lots/99999").contentType(MediaType.APPLICATION_JSON).content(body)
                .session(mockSession)).andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateQCStatus_WithValidStatus_ShouldUpdateAndReturn() throws Exception {
        QCStatusRequest request = new QCStatusRequest();
        request.setQcStatus(QCStatus.FAILED);
        request.setNotes("Failed QC test - contamination detected");

        mockMvc.perform(put("/rest/inventory/lots/1000/qc-status").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.qcStatus").value("FAILED"));

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("QC Status should be updated", QCStatus.FAILED, updatedLot.getQcStatus());
    }

    @Test
    public void testUpdateQCStatus_WithoutNotes_ShouldUpdateWithoutNotes() throws Exception {
        QCStatusRequest request = new QCStatusRequest();
        request.setQcStatus(QCStatus.PENDING);

        mockMvc.perform(put("/rest/inventory/lots/1000/qc-status").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.qcStatus").value("PENDING"));
    }

    @Test
    public void testUpdateStatus_WithValidStatus_ShouldUpdateAndReturn() throws Exception {
        StatusRequest request = new StatusRequest();
        request.setStatus(LotStatus.EXPIRED);

        mockMvc.perform(put("/rest/inventory/lots/1000/status").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("Status should be updated", LotStatus.EXPIRED, updatedLot.getStatus());
    }

    @Test
    public void testAdjustQuantity_WithValidQuantity_ShouldAdjustAndReturn() throws Exception {
        AdjustQuantityRequest request = new AdjustQuantityRequest();
        request.setNewQuantity(85.0);
        request.setReason("Breakage during storage");

        mockMvc.perform(post("/rest/inventory/lots/1000/adjust").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuantity").value(85.0));

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("Quantity should be adjusted", 85.0, updatedLot.getCurrentQuantity(), 0.01);
    }

    @Test
    public void testAdjustQuantity_ToZero_ShouldAllowZeroQuantity() throws Exception {
        AdjustQuantityRequest request = new AdjustQuantityRequest();
        request.setNewQuantity(0.0);
        request.setReason("Complete consumption");

        mockMvc.perform(post("/rest/inventory/lots/1000/adjust").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuantity").value(0.0)).andExpect(jsonPath("$.status").value("CONSUMED"));
    }

    @Test
    public void testDisposeLot_WithValidRequest_ShouldDisposeAndReturn() throws Exception {
        DisposeRequest request = new DisposeRequest();
        request.setReason("Expired");
        request.setNotes("Lot has expired and must be disposed");

        mockMvc.perform(post("/rest/inventory/lots/1000/dispose").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPOSED"));

        InventoryLot disposedLot = inventoryLotService.get(1000L);
        assertEquals("Lot should be marked as DISPOSED", LotStatus.DISPOSED, disposedLot.getStatus());
    }

    @Test
    public void testDisposeLot_WithoutRequest_ShouldDisposeWithoutNotes() throws Exception {
        mockMvc.perform(
                post("/rest/inventory/lots/1000/dispose").contentType(MediaType.APPLICATION_JSON).session(mockSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISPOSED"));
    }

    @Test
    public void testMultipleLotUpdates_ShouldMaintainDataIntegrity() throws Exception {
        AdjustQuantityRequest adjustRequest = new AdjustQuantityRequest();
        adjustRequest.setNewQuantity(90.0);
        adjustRequest.setReason("Cycle count");

        mockMvc.perform(post("/rest/inventory/lots/1000/adjust").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adjustRequest)).session(mockSession))
                .andExpect(status().isOk());

        QCStatusRequest qcRequest = new QCStatusRequest();
        qcRequest.setQcStatus(QCStatus.PENDING);

        mockMvc.perform(put("/rest/inventory/lots/1000/qc-status").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(qcRequest)).session(mockSession)).andExpect(status().isOk());

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("Current quantity should be updated", 90.0, updatedLot.getCurrentQuantity(), 0.01);
        assertEquals("QC status should be updated", QCStatus.PENDING, updatedLot.getQcStatus());
    }
}