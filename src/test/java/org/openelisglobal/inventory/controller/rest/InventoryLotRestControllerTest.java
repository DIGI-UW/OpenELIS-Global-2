package org.openelisglobal.inventory.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.storage.service.SampleStorageService;

@RunWith(MockitoJUnitRunner.class)
public class InventoryLotRestControllerTest {

    @Mock
    private InventoryLotService inventoryLotService;

    @Mock
    private SampleStorageService sampleStorageService;

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

    private InventoryLot lot(Long id) {
        InventoryLot lot = new InventoryLot();
        lot.setId(id);
        return lot;
    }
}
