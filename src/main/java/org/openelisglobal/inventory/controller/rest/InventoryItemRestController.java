package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.department.service.DepartmentIsolationService;
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

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

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
    public ResponseEntity<List<InventoryItem>> getAllActive(HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.getAllActive(), request);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAll(@RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) Boolean isActive, @RequestParam(required = false) String projectName,
            HttpServletRequest request) {
        try {
            List<InventoryItem> items;

            if (itemType != null || isActive != null || projectName != null) {
                // Use filtered approach
                items = inventoryItemService.getAll();
                items = items.stream().filter(item -> itemType == null || item.getItemType().equals(itemType))
                        .filter(item -> isActive == null || item.isActive() == isActive)
                        .filter(item -> projectName == null
                                || (item.getProjectName() != null && item.getProjectName().equals(projectName)))
                        .toList();
            } else {
                items = inventoryItemService.getAll();
            }

            return ResponseEntity.ok(filterAccessible(items, request));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get paginated inventory items with filtering and sorting
     *
     * @param limit     Maximum number of results per page (default: 20, max: 1000)
     * @param offset    Number of results to skip (default: 0)
     * @param sortBy    Field to sort by (default: name)
     * @param sortOrder Sort direction: "asc" or "desc" (default: asc)
     * @param itemType  Filter by item type: REAGENT, RDT, CARTRIDGE, HIV_KIT,
     *                  SYPHILIS_KIT
     * @param isActive  Filter by active status: true/false
     * @param search    Search term for item name
     * @return Paginated response with items and metadata
     */
    @GetMapping(value = "/paged", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPagedItems(@RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder, @RequestParam(required = false) String itemType,
            @RequestParam(required = false) Boolean isActive, @RequestParam(required = false) String search,
            HttpServletRequest request) {
        try {
            ItemType type = null;
            if (itemType != null && !itemType.trim().isEmpty() && !itemType.equalsIgnoreCase("ALL")) {
                try {
                    type = ItemType.valueOf(itemType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }

            List<InventoryItem> items = inventoryItemService.getPagedItems(limit, offset, sortBy, sortOrder, type,
                    isActive, search);
            items = filterAccessible(items, request);

            Long totalRecords = (long) items.size();

            int currentPage = (offset / limit) + 1;
            int totalPages = (int) Math.ceil((double) totalRecords / limit);
            boolean hasMore = offset + limit < totalRecords;

            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("totalRecords", totalRecords);
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("currentPage", currentPage);
            response.put("totalPages", totalPages);
            response.put("hasMore", hasMore);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> getById(@PathVariable String id, HttpServletRequest request) {
        try {
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (item == null) {
                return ResponseEntity.notFound().build();
            }
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/type/{itemType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByType(@PathVariable ItemType itemType, HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.getByItemType(itemType), request);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByCategory(@PathVariable String category, HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.getByCategory(category), request);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/project/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByProject(@PathVariable String projectName,
            HttpServletRequest request) {
        try {
            List<InventoryItem> items = inventoryItemService.getAll().stream()
                    .filter(item -> projectName.equals(item.getProjectName())).collect(Collectors.toList());
            return ResponseEntity.ok(filterAccessible(items, request));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> search(@RequestParam String query, HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.searchByName(query), request);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/low-stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getLowStockItems(HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.getLowStockItems(), request);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StockResponse> getTotalStock(@PathVariable String id, HttpServletRequest request) {
        try {
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
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
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (item.getFhirUuid() == null) {
                item.setFhirUuid(java.util.UUID.randomUUID());
            }

            Long itemId = inventoryItemService.insert(item);
            InventoryItem savedItem = inventoryItemService.get(itemId);
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
            if (!departmentIsolationService.canAccessInventoryItem(existingItem, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            item.setId(Long.valueOf(id));

            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            InventoryItem updatedItem = inventoryItemService.update(item);
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
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

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
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (!departmentIsolationService.canAccessInventoryItem(item, request)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

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

    private List<InventoryItem> filterAccessible(List<InventoryItem> items, HttpServletRequest request) {
        return items.stream().filter(item -> departmentIsolationService.canAccessInventoryItem(item, request)).toList();
    }
}
