package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
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
@RequestMapping("/rest/inventory/items")
public class InventoryItemRestController extends BaseRestController {

    @Autowired
    private InventoryItemService inventoryItemService;

    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ItemType>> getAllItemTypes() {
        try {
            List<ItemType> types = inventoryItemService.getAllItemTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAllActive() {
        try {
            List<InventoryItem> items = inventoryItemService.getAllActive();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAll() {
        try {
            List<InventoryItem> items = inventoryItemService.getAll();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> getById(@PathVariable String id) {
        try {
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/type/{itemType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByType(@PathVariable ItemType itemType) {
        try {
            List<InventoryItem> items = inventoryItemService.getByItemType(itemType);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByCategory(@PathVariable String category) {
        try {
            List<InventoryItem> items = inventoryItemService.getByCategory(category);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> search(@RequestParam String query) {
        try {
            List<InventoryItem> items = inventoryItemService.searchByName(query);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/low-stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getLowStockItems() {
        try {
            List<InventoryItem> items = inventoryItemService.getLowStockItems();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StockResponse> getTotalStock(@PathVariable String id) {
        try {
            Double stock = inventoryItemService.getTotalCurrentStock(Long.valueOf(id));
            boolean inStock = inventoryItemService.isInStock(Long.valueOf(id));
            return ResponseEntity.ok(new StockResponse(stock, inStock));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> create(@Valid @RequestBody InventoryItem item, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);

            // Generate FHIR UUID if not provided
            if (item.getFhirUuid() == null) {
                item.setFhirUuid(java.util.UUID.randomUUID());
            }

            InventoryItem savedItem = inventoryItemService.save(item);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> update(@PathVariable String id, @Valid @RequestBody InventoryItem item,
            HttpServletRequest request) {
        try {
            InventoryItem existingItem = inventoryItemService.get(Long.valueOf(id));
            if (existingItem == null) {
                return ResponseEntity.notFound().build();
            }

            // Update only the fields that can be changed
            existingItem.setName(item.getName());
            existingItem.setItemType(item.getItemType());
            existingItem.setCategory(item.getCategory());
            existingItem.setManufacturer(item.getManufacturer());
            existingItem.setUnits(item.getUnits());
            existingItem.setLowStockThreshold(item.getLowStockThreshold());

            // Type-specific fields
            existingItem.setStabilityAfterOpening(item.getStabilityAfterOpening());
            existingItem.setStorageRequirements(item.getStorageRequirements());
            existingItem.setCompatibleAnalyzers(item.getCompatibleAnalyzers());
            existingItem.setTestsPerKit(item.getTestsPerKit());

            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            existingItem.setSysUserId(sysUserId);

            InventoryItem updatedItem = inventoryItemService.update(existingItem);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deactivate(@PathVariable String id, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            inventoryItemService.deactivateItem(Long.valueOf(id), sysUserId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> activate(@PathVariable String id, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            inventoryItemService.activateItem(Long.valueOf(id), sysUserId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class StockResponse {
        private Double quantity;
        private Boolean inStock;

        public StockResponse(Double quantity, Boolean inStock) {
            this.quantity = quantity;
            this.inStock = inStock;
        }

    }

}
