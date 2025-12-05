package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-lots")
public class InventoryLotRestController extends BaseRestController {

    @Autowired
    private InventoryLotService inventoryLotService;

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> getInventoryLotById(@PathVariable String id) {
        InventoryLot lot = inventoryLotService.get(id);
        if (lot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lot);
    }

    @GetMapping(value = "/by-item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getLotsByItem(@PathVariable String itemId) {
        List<InventoryLot> lots = inventoryLotService.findByInventoryItemId(itemId);
        return ResponseEntity.ok(lots);
    }

    @GetMapping(value = "/by-item/{itemId}/fefo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getAvailableLotsByItemFEFO(@PathVariable String itemId) {
        List<InventoryLot> lots = inventoryLotService.findAvailableLotsByItemFEFO(itemId);
        return ResponseEntity.ok(lots);
    }

    @GetMapping(value = "/expiring-soon", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getExpiringSoonLots(@RequestParam(defaultValue = "30") int daysAhead) {
        List<InventoryLot> lots = inventoryLotService.findExpiringSoon(daysAhead);
        return ResponseEntity.ok(lots);
    }

    @GetMapping(value = "/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getExpiredActiveLots() {
        List<InventoryLot> lots = inventoryLotService.findExpiredActiveLots();
        return ResponseEntity.ok(lots);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> createInventoryLot(HttpServletRequest httpRequest,
            @Valid @RequestBody InventoryLot lot) {
        lot.setSysUserId(getSysUserId(httpRequest));
        String id = inventoryLotService.insert(lot);
        InventoryLot created = inventoryLotService.get(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> updateInventoryLot(HttpServletRequest httpRequest, @PathVariable String id,
            @Valid @RequestBody InventoryLot lot) {
        InventoryLot existing = inventoryLotService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        lot.setId(id);
        lot.setSysUserId(getSysUserId(httpRequest));
        inventoryLotService.update(lot);
        InventoryLot updated = inventoryLotService.get(id);
        return ResponseEntity.ok(updated);
    }
}
