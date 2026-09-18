package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryTagService;
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

    @Autowired
    private InventoryTagService inventoryTagService;

    /**
     * Suggestions for the item editor's tag typeahead. Retired tags are left out:
     * the point of retiring one is that it stops being offered for new tagging,
     * while every item already carrying it keeps it.
     */
    @GetMapping(value = "/tags", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getAllTags() {
        try {
            return ResponseEntity.ok(inventoryTagService.getActiveTagNames());
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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
    public ResponseEntity<?> create(@Valid @RequestBody InventoryItem item, HttpServletRequest request) {
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
        } catch (LocalizedValidationException e) {
            return ResponseEntity.badRequest().body(InventoryErrorBody.localized(e));
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
            // itemType is deliberately absent: tags classify an item now, and leaving the
            // legacy
            // column writable here would make it a second answer to the same question, free
            // to
            // drift away from the tags the moment someone edits one.
            existingItem.setTags(item.getTags());
            existingItem.setCategory(item.getCategory());
            existingItem.setManufacturer(item.getManufacturer());
            existingItem.setUnits(item.getUnits());
            existingItem.setLowStockThreshold(item.getLowStockThreshold());
            // Absent from this whitelist until now, so an edit returned 200 and silently
            // persisted nothing while create worked — the column existed and the board
            // displayed it, but nothing could change it.
            existingItem.setLeadTimeDays(item.getLeadTimeDays());

            // Type-specific fields
            existingItem.setStabilityAfterOpening(item.getStabilityAfterOpening());
            existingItem.setStorageRequirements(item.getStorageRequirements());
            existingItem.setCompatibleAnalyzers(item.getCompatibleAnalyzers());
            existingItem.setTestsPerKit(item.getTestsPerKit());

            // The editor gained these when it became the only place an item is
            // defined. Create binds the whole entity, so a field left out of this
            // list persists on POST and silently does nothing on PUT — which is
            // exactly how editing a lead time returned 200 and changed nothing.
            existingItem.setCatalogNumber(item.getCatalogNumber());
            existingItem.setExpirationAlertDays(item.getExpirationAlertDays());
            existingItem.setTrackLots(item.getTrackLots());

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

    /**
     * Record that the selected items have been ordered, so the board can stop
     * reporting them as needing attention while still showing the shortfall.
     *
     * <p>
     * One endpoint for one item and for a selection, because marking three rows at
     * once is the common case and a per-item loop on the client would half-apply on
     * the first failure.
     */
    @PostMapping(value = "/mark-ordered", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> markOrdered(@RequestBody OrderMarkRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            LocalDate expected = request.getExpectedDate() == null || request.getExpectedDate().isEmpty() ? null
                    : LocalDate.parse(request.getExpectedDate());
            int changed = inventoryItemService.markOrdered(request.getItemIds(), request.getNote(), expected,
                    sysUserId);
            return ResponseEntity.ok(new OrderMarkResponse(changed));
        } catch (DateTimeParseException e) {
            LogEvent.logError(e);
            Map<String, Object> body = new HashMap<>();
            body.put("message", "expectedDate must be an ISO date, yyyy-MM-dd");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Undo a mark-as-ordered. */
    @PostMapping(value = "/clear-ordered", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> clearOrdered(@RequestBody OrderMarkRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            return ResponseEntity
                    .ok(new OrderMarkResponse(inventoryItemService.clearOrdered(request.getItemIds(), sysUserId)));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class OrderMarkRequest {
        private List<Long> itemIds;
        private String note;
        /** ISO yyyy-MM-dd, or absent. */
        private String expectedDate;
    }

    @Setter
    @Getter
    public static class OrderMarkResponse {
        private int changed;

        public OrderMarkResponse(int changed) {
            this.changed = changed;
        }
    }

}
