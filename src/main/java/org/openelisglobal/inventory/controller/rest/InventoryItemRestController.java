package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import javax.validation.Valid;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory-items")
public class InventoryItemRestController extends BaseRestController {

    @Autowired
    private InventoryItemService inventoryItemService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAllInventoryItems() {
        List<InventoryItem> items = inventoryItemService.findAllActive();
        return ResponseEntity.ok(items);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> getInventoryItemById(@PathVariable String id) {
        InventoryItem item = inventoryItemService.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    @GetMapping(value = "/by-type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getInventoryItemsByType(@RequestParam String itemType) {
        List<InventoryItem> items = inventoryItemService.findByItemType(itemType);
        return ResponseEntity.ok(items);
    }

    @GetMapping(value = "/by-category", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getInventoryItemsByCategory(@RequestParam String category) {
        List<InventoryItem> items = inventoryItemService.findByCategory(category);
        return ResponseEntity.ok(items);
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> searchInventoryItems(@RequestParam String name) {
        List<InventoryItem> items = inventoryItemService.searchByName(name);
        return ResponseEntity.ok(items);
    }

    @GetMapping(value = "/low-stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getLowStockItems() {
        List<InventoryItem> items = inventoryItemService.findLowStockItems();
        return ResponseEntity.ok(items);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> createInventoryItem(HttpServletRequest httpRequest,
            @Valid @RequestBody InventoryItem item) {
        item.setSysUserId(getSysUserId(httpRequest));
        String id = inventoryItemService.insert(item);
        InventoryItem created = inventoryItemService.get(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> updateInventoryItem(HttpServletRequest httpRequest, @PathVariable String id,
            @Valid @RequestBody InventoryItem item) {
        InventoryItem existing = inventoryItemService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        item.setId(id);
        item.setSysUserId(getSysUserId(httpRequest));
        inventoryItemService.update(item);
        InventoryItem updated = inventoryItemService.get(id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteInventoryItem(HttpServletRequest httpRequest, @PathVariable String id) {
        InventoryItem item = inventoryItemService.get(id);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        item.setIsActive(IActionConstants.NO);
        item.setSysUserId(getSysUserId(httpRequest));
        inventoryItemService.update(item);
        return ResponseEntity.noContent().build();
    }
}
