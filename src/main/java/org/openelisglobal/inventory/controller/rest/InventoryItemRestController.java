package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.action.IActionConstants;
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

    @GetMapping(value = "/assignable-departments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getAssignableDepartments(HttpServletRequest request) {
        try {
            return ResponseEntity.ok(departmentIsolationService.getAssignableWorkflowDepartments(request));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/linked-projects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getLinkedProjects(
            @RequestParam(required = false) Integer departmentId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(departmentIsolationService.getAssignableInventoryProjects(request, departmentId));
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
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) List<Integer> departmentIds, HttpServletRequest request) {
        try {
            Set<Integer> requestedDepartmentIds = resolveRequestedDepartmentIds(departmentId, departmentIds);
            List<InventoryItem> items = filterAccessible(inventoryItemService.getAll(), request).stream()
                    .filter(item -> itemType == null || item.getItemType().equals(itemType))
                    .filter(item -> isActive == null || item.isActive() == isActive)
                    .filter(item -> projectName == null
                            || (item.getProjectName() != null && item.getProjectName().equals(projectName)))
                    .filter(item -> requestedDepartmentIds.isEmpty() || requestedDepartmentIds.stream()
                            .anyMatch(requestedDepartmentId -> departmentIsolationService
                                    .inventoryBelongsToDepartment(item, requestedDepartmentId)))
                    .toList();
            return ResponseEntity.ok(items);
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
            @RequestParam(required = false) Integer departmentId,
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

            List<InventoryItem> items;
            long totalRecords;
            if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                Set<Integer> adminDepartmentScope = departmentId == null ? null : Set.of(departmentId);
                items = inventoryItemService.getPagedItems(limit, offset, sortBy, sortOrder, type, isActive, search,
                        adminDepartmentScope);
                totalRecords = inventoryItemService.getPagedItemsCount(type, isActive, search, adminDepartmentScope);
            } else {
                Set<Integer> departmentScope = departmentIsolationService.getRestrictedUserTestSectionIds(request);
                items = inventoryItemService.getPagedItems(limit, offset, sortBy, sortOrder, type, isActive, search,
                        departmentScope);
                totalRecords = inventoryItemService.getPagedItemsCount(type, isActive, search, departmentScope);
            }

            int currentPage = limit > 0 ? (offset / limit) + 1 : 1;
            int totalPages = limit > 0 ? (int) Math.ceil((double) totalRecords / limit) : 0;
            boolean hasMore = offset + limit < totalRecords;

            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("totalRecords", Long.valueOf(totalRecords));
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
    public ResponseEntity<List<InventoryItem>> getByType(@PathVariable ItemType itemType,
            @RequestParam(required = false) List<Integer> departmentIds, HttpServletRequest request) {
        try {
            List<InventoryItem> items = filterAccessible(inventoryItemService.getByItemType(itemType), request,
                    departmentIds);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByCategory(@PathVariable String category,
            HttpServletRequest request) {
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
    public ResponseEntity<?> create(@Valid @RequestBody InventoryItem item, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);
            Integer departmentId = departmentIsolationService.resolveDepartmentForStrictScopedCreate(request,
                    item.getDepartmentTestSectionId(), item.getProjectName());
            if (departmentId == null) {
                if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                    return jsonError(HttpStatus.BAD_REQUEST, "Select a department (departmentTestSectionId).");
                }
                if (departmentIsolationService.getRestrictedUserTestSectionIds(request).isEmpty()) {
                    return jsonError(HttpStatus.FORBIDDEN, "Select a department first.");
                }
                return jsonError(HttpStatus.BAD_REQUEST, "Select a department first.");
            }
            item.setDepartmentTestSectionId(departmentId);
            if (!departmentIsolationService.isInventoryProjectConsistent(departmentId, item.getProjectName())) {
                return jsonError(HttpStatus.BAD_REQUEST,
                        "Selected linked notebook / project belongs to a different department.");
            }
            if (!departmentIsolationService.canAccessInventoryItemStrictIntersection(item, request)) {
                return jsonError(HttpStatus.FORBIDDEN, "Access denied");
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

            UserSessionData usd = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);
            Integer departmentId = existingItem.getDepartmentTestSectionId();
            if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                departmentId = item.getDepartmentTestSectionId();
            }
            departmentId = departmentIsolationService.resolveDepartmentForStrictScopedCreate(request, departmentId,
                    item.getProjectName());
            if (departmentId == null) {
                if (departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            item.setDepartmentTestSectionId(departmentId);
            if (!departmentIsolationService.isInventoryProjectConsistent(departmentId, item.getProjectName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (!departmentIsolationService.canAccessInventoryItemStrictIntersection(item, request)) {
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
            UserSessionData usd = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
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
            UserSessionData usd = (UserSessionData) request.getSession()
                    .getAttribute(IActionConstants.USER_SESSION_DATA);
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

    private List<InventoryItem> filterAccessible(List<InventoryItem> items, HttpServletRequest request,
            List<Integer> departmentIds) {
        List<InventoryItem> accessibleItems = filterAccessible(items, request);
        if (departmentIds == null || departmentIds.isEmpty()) {
            return accessibleItems;
        }
        Set<Integer> requestedDepartmentIds = Set.copyOf(departmentIds);
        return accessibleItems.stream()
                .filter(item -> requestedDepartmentIds.stream()
                        .anyMatch(departmentId -> departmentIsolationService.inventoryBelongsToDepartment(item,
                                departmentId)))
                .toList();
    }

    private Set<Integer> resolveRequestedDepartmentIds(Integer departmentId, List<Integer> departmentIds) {
        if (departmentIds != null && !departmentIds.isEmpty()) {
            return Set.copyOf(departmentIds);
        }
        return departmentId == null ? Set.of() : Set.of(departmentId);
    }

    private ResponseEntity<Map<String, String>> jsonError(HttpStatus status, String message) {
        Map<String, String> err = new HashMap<>();
        err.put("error", message);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(err);
    }
}
