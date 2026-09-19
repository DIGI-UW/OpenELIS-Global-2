package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.inventory.controller.rest.InventoryLotRestController.AdjustQuantityRequest;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.storage.service.SampleStorageService;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class InventoryLotRestControllerTest {

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private SampleStorageService sampleStorageService;

    @Mock
    private InventoryItemService inventoryItemService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private InventoryLotRestController controller;

    @Test
    public void getAll_resolvesLotLocationsInOneBulkCall() {
        when(inventoryLotService.getAll()).thenReturn(Arrays.asList(lot(1L), lot(2L)));
        when(sampleStorageService.getLocationsForInventoryLots(Arrays.asList(1L, 2L))).thenReturn(
                Collections.singletonMap("2", Collections.singletonMap("hierarchicalPath", "Room > Freezer")));

        List<InventoryLot> body = controller.getAll().getBody();

        verify(sampleStorageService, times(1)).getLocationsForInventoryLots(Arrays.asList(1L, 2L));
        verify(sampleStorageService, never()).getInventoryLotLocation(anyString());
        assertNull("Lot without an assignment keeps a null location", body.get(0).getLocation());
        assertEquals("Room > Freezer", body.get(1).getLocation().get("hierarchicalPath"));
    }

    @Test
    public void adjustQuantity_answers404WithBody_whenLotDoesNotExist() {
        stubSession();
        when(inventoryLotService.adjustLotQuantity(999L, 3.0, "recount", "7"))
                .thenThrow(new ObjectNotFoundException(999L, InventoryLot.class.getName()));

        ResponseEntity<?> response = controller.adjustQuantity("999", adjust(3.0, "recount"), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("InventoryLot 999 not found", body(response).get("error"));
    }

    @Test
    public void adjustQuantity_answers400WithBody_whenServiceRefuses() {
        stubSession();
        when(inventoryLotService.adjustLotQuantity(5L, 3.0, "recount", "7"))
                .thenThrow(new IllegalStateException("Cannot adjust a DISPOSED lot: LOT-5"));

        ResponseEntity<?> response = controller.adjustQuantity("5", adjust(3.0, "recount"), request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Cannot adjust a DISPOSED lot: LOT-5", body(response).get("error"));
    }

    @Test
    public void disposeLot_answers400WithBody_whenAlreadyDisposed() {
        stubSession();
        when(inventoryLotService.disposeLot(5L, null, null, "7"))
                .thenThrow(new IllegalStateException("Lot already disposed: LOT-5"));

        ResponseEntity<?> response = controller.disposeLot("5", null, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Lot already disposed: LOT-5", body(response).get("error"));
    }

    @Test
    public void create_answers400WithLocalizedBody_whenBarcodeIsTaken() {
        stubSession();
        InventoryItem item = new InventoryItem();
        item.setId(13L);
        when(inventoryItemService.get(13L)).thenReturn(item);
        when(inventoryLotService.save(any(InventoryLot.class))).thenThrow(new LocalizedValidationException(
                "inventory.lot.error.duplicateBarcode", "Barcode already in use", Map.of("barcode", "BC-123")));
        InventoryLot lot = lot(null);
        lot.setInventoryItem(item);
        lot.setBarcode("BC-123");

        ResponseEntity<?> response = controller.create(lot, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("inventory.lot.error.duplicateBarcode", body(response).get("errorCode"));
        assertEquals(Map.of("barcode", "BC-123"), body(response).get("params"));
        assertEquals("Barcode already in use", body(response).get("message"));
    }

    @Test
    public void update_answers404WithBody_whenLotDoesNotExist() {
        when(inventoryLotService.get(999L)).thenThrow(new ObjectNotFoundException(999L, InventoryLot.class.getName()));

        ResponseEntity<?> response = controller.update("999", lot(null), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("InventoryLot 999 not found", body(response).get("error"));
    }

    @Test
    public void update_keepsTheStoredBarcode_whenTheBodyOmitsIt() {
        stubSession();
        InventoryLot stored = lot(6L);
        stored.setBarcode("PAR-500-001-LOT-6");
        when(inventoryLotService.get(6L)).thenReturn(stored);

        controller.update("6", lot(null), request);

        assertEquals("PAR-500-001-LOT-6", updatedLot().getBarcode());
    }

    @Test
    public void update_assignsABarcode_whenTheStoredLotHasNone() {
        stubSession();
        when(inventoryLotService.get(6L)).thenReturn(lot(6L));
        InventoryLot body = lot(null);
        body.setBarcode("PAR-500-001-LOT-6");

        controller.update("6", body, request);

        assertEquals("A lot that carries no barcode can still be given one", "PAR-500-001-LOT-6",
                updatedLot().getBarcode());
    }

    private InventoryLot updatedLot() {
        ArgumentCaptor<InventoryLot> captor = ArgumentCaptor.forClass(InventoryLot.class);
        verify(inventoryLotService).update(captor.capture());
        return captor.getValue();
    }

    private void stubSession() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(7);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(IActionConstants.USER_SESSION_DATA)).thenReturn(usd);
    }

    private AdjustQuantityRequest adjust(Double newQuantity, String reason) {
        AdjustQuantityRequest adjust = new AdjustQuantityRequest();
        adjust.setNewQuantity(newQuantity);
        adjust.setReason(reason);
        return adjust;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    private InventoryLot lot(Long id) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        return lot;
    }
}
