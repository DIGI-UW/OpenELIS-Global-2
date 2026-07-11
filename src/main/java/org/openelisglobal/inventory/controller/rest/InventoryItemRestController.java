package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryItemTypeService;
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

    @Autowired
    private InventoryItemTypeService inventoryItemTypeService;

    /**
     * Active item types, sorted for display — sourced from the admin-managed
     * {@code inventory_item_type} table (OGC-658 Part A), not a hardcoded enum.
     * Labels are resolved for the current request locale so the frontend no longer
     * needs its own hardcoded label map.
     */
    @GetMapping(value = "/types", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ItemTypeOption>> getAllItemTypes() {
        try {
            List<ItemTypeOption> types = inventoryItemTypeService.getAllActiveOrderedBySortOrder().stream()
                    .map(type -> new ItemTypeOption(type.getCode(), type.getLabel())).collect(Collectors.toList());
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
            InventoryItem item = inventoryItemService.get(id);
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
    public ResponseEntity<List<InventoryItem>> getByType(@PathVariable String itemType) {
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
            Double stock = inventoryItemService.getTotalCurrentStock(id);
            boolean inStock = inventoryItemService.isInStock(id);
            return ResponseEntity.ok(new StockResponse(stock, inStock));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@Valid @RequestBody InventoryItem item, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);

            // Generate FHIR UUID if not provided
            if (item.getFhirUuid() == null) {
                item.setFhirUuid(java.util.UUID.randomUUID());
            }

            // OGC-658 Part C: always insert here, never inventoryItemService.save()'s
            // insert-vs-update heuristic — that heuristic treats a non-blank id as
            // "this is an update", which is wrong for create-with-explicit-code and
            // would skip insert()'s code normalization/collision handling entirely.
            String code = inventoryItemService.insert(item);
            InventoryItem savedItem = inventoryItemService.get(code);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
        } catch (org.openelisglobal.common.exception.LocalizedValidationException e) {
            // OGC-658 Part C (C8): malformed/duplicate codes surface as a clean, i18n-keyed
            // 400 rather than a generic 500 or a hardcoded English message — see
            // InventoryItemServiceImpl.insert() / CodeGenerator.normalize().
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(e));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private java.util.Map<String, Object> errorBody(
            org.openelisglobal.common.exception.LocalizedValidationException e) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("message", e.getMessage());
        body.put("errorCode", e.getErrorCode());
        body.put("params", e.getParams());
        return body;
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> update(@PathVariable String id, @Valid @RequestBody InventoryItem item,
            HttpServletRequest request) {
        try {
            InventoryItem existingItem = inventoryItemService.get(id);
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

            inventoryItemService.deactivateItem(id, sysUserId);
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

            inventoryItemService.activateItem(id, sysUserId);
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

    @Setter
    @Getter
    public static class ItemTypeOption {
        private String code;
        private String label;

        public ItemTypeOption(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

}
