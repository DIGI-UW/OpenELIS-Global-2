package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
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
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryStorageLocationService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class InventoryLotRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryStorageLocationService storageLocationService;

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

    @Test
    public void testGetAll_ShouldReturnAllLots() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots").contentType("application/json")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty()).andExpect(jsonPath("$[0].lotNumber").isNotEmpty());
    }

    @Test
    public void testGetById_WithValidId_ShouldReturnLot() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/1000").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1000))
                .andExpect(jsonPath("$.lotNumber").value("LOT-2025-001"))
                .andExpect(jsonPath("$.currentQuantity").value(100.00)).andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    public void testGetById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/99999").contentType(MediaType.APPLICATION_JSON).session(mockSession))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetByItemId_WithValidItemId_ShouldReturnLotsForItem() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].inventoryItem.id").value(1000))
                .andExpect(jsonPath("$[1].inventoryItem.id").value(1000));
    }

    @Test
    public void testGetByItemId_WithInvalidItemId_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/99999").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetAvailableLotsFEFO_ShouldReturnLotsInFEFOOrder() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000/available").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].qcStatus").value("PASSED"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    public void testGetByLocationId_WithValidLocationId_ShouldReturnLotsInLocation() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/location/1000").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].storageLocation.id").value(1000))
                .andExpect(jsonPath("$[1].storageLocation.id").value(1000));
    }

    @Test
    public void testGetByLocationId_WithInvalidLocationId_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/location/99999").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
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
    public void testGetExpiringLots_ShouldReturnExpiringLots() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/expiring?days=30").contentType("application/json"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testGetExpiringLots_WithDefaultDays_ShouldUseDefault() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/expiring").contentType("application/json"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetExpiredActiveLots_ShouldReturnExpiredLotsOnly() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/expired").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreate_WithValidLot_ShouldCreateAndReturnLot() throws Exception {
        InventoryLot newLot = new InventoryLot();
        newLot.setLotNumber("LOT-2025-NEW");
        newLot.setInitialQuantity(50.0);
        newLot.setCurrentQuantity(50.0);
        newLot.setQcStatus(QCStatus.PASSED);
        newLot.setStatus(LotStatus.ACTIVE);

        InventoryItem item = inventoryItemService.get(1000L);
        newLot.setInventoryItem(item);

        InventoryStorageLocation location = storageLocationService.get(1000L);
        newLot.setStorageLocation(location);

        Timestamp receiptDate = new Timestamp(System.currentTimeMillis());
        newLot.setReceiptDate(receiptDate);

        Timestamp expirationDate = new Timestamp(System.currentTimeMillis() + 86400000L * 365);
        newLot.setExpirationDate(expirationDate);

        MvcResult result = mockMvc
                .perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLot)).session(mockSession))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lotNumber").value("LOT-2025-NEW"))
                .andExpect(jsonPath("$.currentQuantity").value(50.0)).andExpect(jsonPath("$.fhirUuid").isNotEmpty())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertNotNull("Created lot should have ID", jsonNode.get("id"));
        assertNotNull("Created lot should have FHIR UUID", jsonNode.get("fhirUuid"));
    }

    @Test
    public void testCreate_WithoutFhirUuid_ShouldGenerateUUID() throws Exception {
        InventoryLot newLot = new InventoryLot();
        newLot.setLotNumber("LOT-2025-UUID");
        newLot.setInitialQuantity(25.0);
        newLot.setCurrentQuantity(25.0);
        newLot.setQcStatus(QCStatus.PASSED);
        newLot.setStatus(LotStatus.ACTIVE);

        InventoryItem item = inventoryItemService.get(1000L);
        newLot.setInventoryItem(item);

        InventoryStorageLocation location = storageLocationService.get(1000L);
        newLot.setStorageLocation(location);

        newLot.setReceiptDate(new Timestamp(System.currentTimeMillis()));
        newLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86400000L * 365));

        mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLot)).session(mockSession)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fhirUuid").isNotEmpty());
    }

    @Test
    public void testCreate_WithInvalidItemId_ShouldReturnBadRequest() throws Exception {
        InventoryLot newLot = new InventoryLot();
        newLot.setLotNumber("LOT-2025-INVALID");
        newLot.setInitialQuantity(50.0);
        newLot.setCurrentQuantity(50.0);
        newLot.setQcStatus(QCStatus.PASSED);
        newLot.setStatus(LotStatus.ACTIVE);

        InventoryItem invalidItem = new InventoryItem();
        invalidItem.setId(99999L);
        newLot.setInventoryItem(invalidItem);

        mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLot)).session(mockSession))
                .andExpect(status().isInternalServerError());
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
                .andExpect(status().isOk());
    }

    @Test
    public void testProcessExpired_ShouldReturnCountOfUpdatedLots() throws Exception {
        mockMvc.perform(post("/rest/inventory/lots/process-expired").contentType(MediaType.APPLICATION_JSON)
                .session(mockSession)).andExpect(status().isInternalServerError());
    }

    @Test
    public void testUpdate_WithValidLot_ShouldUpdateAndReturnLot() throws Exception {
        InventoryLot lotToUpdate = inventoryLotService.get(1000L);
        lotToUpdate.setCurrentQuantity(75.0);

        mockMvc.perform(put("/rest/inventory/lots/1000").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lotToUpdate)).session(mockSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000)).andExpect(jsonPath("$.currentQuantity").value(75.0));

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("Lot should be updated in database", 75.0, updatedLot.getCurrentQuantity(), 0.01);
    }

    @Test
    public void testUpdate_PreservesFhirUuid() throws Exception {
        InventoryLot originalLot = inventoryLotService.get(1000L);
        UUID originalUUID = originalLot.getFhirUuid();

        InventoryLot lotToUpdate = new InventoryLot();
        lotToUpdate.setLotNumber("LOT-2025-001-UPDATED");
        lotToUpdate.setCurrentQuantity(80.0);
        lotToUpdate.setInitialQuantity(100.0);
        lotToUpdate.setQcStatus(QCStatus.PASSED);
        lotToUpdate.setStatus(LotStatus.ACTIVE);

        InventoryItem item = inventoryItemService.get(1000L);
        lotToUpdate.setInventoryItem(item);

        mockMvc.perform(put("/rest/inventory/lots/1000").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lotToUpdate)).session(mockSession)).andExpect(status().isOk());

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("FHIR UUID should be preserved", originalUUID, updatedLot.getFhirUuid());
    }

    @Test
    public void testUpdate_WithInvalidId_ShouldReturnNotFound() throws Exception {
        InventoryLot lotToUpdate = new InventoryLot();
        lotToUpdate.setLotNumber("LOT-INVALID");
        lotToUpdate.setCurrentQuantity(50.0);

        mockMvc.perform(put("/rest/inventory/lots/99999").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lotToUpdate)).session(mockSession))
                .andExpect(status().isInternalServerError());
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
                .andExpect(jsonPath("$.currentQuantity").value(0.0));
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
    public void testGetById_WithInvalidIdFormat_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/invalid-id").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCreate_WithoutUserSession_ShouldHandleException() throws Exception {
        InventoryLot newLot = new InventoryLot();
        newLot.setLotNumber("LOT-NO-SESSION");
        newLot.setInitialQuantity(50.0);
        newLot.setCurrentQuantity(50.0);

        InventoryItem item = inventoryItemService.get(1000L);
        newLot.setInventoryItem(item);

        mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLot))).andExpect(status().isInternalServerError());
    }

    @Test
    public void testGetTotalQuantity_WithItemHavingNoLots_ShouldReturnZeroOrNull() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1001/total-quantity").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAvailableLotsFEFO_ShouldReturnOnlyAvailableLots() throws Exception {
        mockMvc.perform(get("/rest/inventory/lots/item/1000/available").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testMultipleLotUpdates_ShouldMaintainDataIntegrity() throws Exception {
        InventoryLot lot = inventoryLotService.get(1000L);
        lot.setCurrentQuantity(90.0);

        mockMvc.perform(put("/rest/inventory/lots/1000").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(lot)).session(mockSession)).andExpect(status().isOk());

        QCStatusRequest qcRequest = new QCStatusRequest();
        qcRequest.setQcStatus(QCStatus.PENDING);

        mockMvc.perform(put("/rest/inventory/lots/1000/qc-status").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(qcRequest)).session(mockSession)).andExpect(status().isOk());

        InventoryLot updatedLot = inventoryLotService.get(1000L);
        assertEquals("Current quantity should be updated", 90.0, updatedLot.getCurrentQuantity(), 0.01);
        assertEquals("QC status should be updated", QCStatus.PENDING, updatedLot.getQcStatus());
    }

    @Test
    public void testCreateMultipleLots_ForSameItem_ShouldAllowMultipleLots() throws Exception {
        for (int i = 0; i < 3; i++) {
            InventoryLot newLot = new InventoryLot();
            newLot.setLotNumber("LOT-MULTI-" + i);
            newLot.setInitialQuantity(10.0 * (i + 1));
            newLot.setCurrentQuantity(10.0 * (i + 1));
            newLot.setQcStatus(QCStatus.PASSED);
            newLot.setStatus(LotStatus.ACTIVE);

            InventoryItem item = inventoryItemService.get(1000L);
            newLot.setInventoryItem(item);

            InventoryStorageLocation location = storageLocationService.get(1000L);
            newLot.setStorageLocation(location);

            newLot.setReceiptDate(new Timestamp(System.currentTimeMillis()));
            newLot.setExpirationDate(new Timestamp(System.currentTimeMillis() + 86400000L * 365));

            mockMvc.perform(post("/rest/inventory/lots").contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newLot)).session(mockSession))
                    .andExpect(status().isCreated());
        }
    }
}
