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
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacAuditService;
import org.openelisglobal.rbac.service.RbacPermissionService;
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
    private RbacPermissionService rbacPermissionService;

    @Autowired
    private RbacAuditService rbacAuditService;

    private boolean checkInventoryPermission(HttpServletRequest request, String action, String departmentId) {
        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId == null)
                return false;
            boolean hasPermission = departmentId != null
                    ? rbacPermissionService.hasPermission(sysUserId, "INVENTORY", action, departmentId)
                    : rbacPermissionService.hasPermission(sysUserId, "INVENTORY", action);
            if (!hasPermission) {
                RbacContext ctx = RbacContext.get();
                rbacAuditService.logDenied(sysUserId, ctx != null ? ctx.getUsername() : "unknown", "INVENTORY", action,
                        null, null, departmentId, null, request.getRemoteAddr(), "Access denied");
            }
            return hasPermission;
        } catch (Exception e) {
            return false;
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
    public ResponseEntity<List<InventoryItem>> getAllActive(HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(applyDepartmentFilter(inventoryItemService.getAllActive()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Apply active-department filter for restricted users. Unrestricted users see
     * all.
     */
    private List<InventoryItem> applyDepartmentFilter(List<InventoryItem> items) {
        RbacContext ctx = RbacContext.get();
        if (ctx == null || ctx.isUnrestricted())
            return items;
        String activeDept = ctx.getActiveDepartmentId();
        if (activeDept == null)
            return items;
        return items.stream().filter(i -> activeDept.equals(i.getDepartmentId())).collect(Collectors.toList());
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getAll(@RequestParam(required = false) ItemType itemType,
            @RequestParam(required = false) Boolean isActive, @RequestParam(required = false) String projectName,
            HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            List<InventoryItem> items = applyDepartmentFilter(inventoryItemService.getAll());
            if (itemType != null)
                items = items.stream().filter(i -> i.getItemType().equals(itemType)).toList();
            if (isActive != null)
                items = items.stream().filter(i -> i.isActive() == isActive).toList();
            if (projectName != null)
                items = items.stream().filter(i -> projectName.equals(i.getProjectName())).toList();
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
            HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            ItemType type = null;
            if (itemType != null && !itemType.trim().isEmpty() && !itemType.equalsIgnoreCase("ALL")) {
                try {
                    type = ItemType.valueOf(itemType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }

            // Determine active department for restricted users
            RbacContext ctx = RbacContext.get();
            String activeDept = (ctx != null && !ctx.isUnrestricted()) ? ctx.getActiveDepartmentId() : null;

            List<InventoryItem> items = inventoryItemService.getPagedItems(limit, offset, sortBy, sortOrder, type,
                    isActive, search, activeDept);
            Long totalRecords = inventoryItemService.getPagedItemsCount(type, isActive, search, activeDept);

            int currentPage = (offset / limit) + 1;
            int totalPages = (int) Math.ceil((double) totalRecords / limit);

            Map<String, Object> response = new HashMap<>();
            response.put("items", items);
            response.put("totalRecords", totalRecords);
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("currentPage", currentPage);
            response.put("totalPages", totalPages);
            response.put("hasMore", offset + limit < totalRecords);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> getById(@PathVariable String id, HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (item == null)
                return ResponseEntity.notFound().build();
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted() && !ctx.isInActiveDepartment(item.getDepartmentId())) {
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
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(applyDepartmentFilter(inventoryItemService.getByItemType(itemType)));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByCategory(@PathVariable String category,
            HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(applyDepartmentFilter(inventoryItemService.getByCategory(category)));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/project/{projectName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getByProject(@PathVariable String projectName,
            HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            List<InventoryItem> items = inventoryItemService.getAll().stream()
                    .filter(item -> projectName.equals(item.getProjectName())).collect(Collectors.toList());
            return ResponseEntity.ok(applyDepartmentFilter(items));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> search(@RequestParam String query, HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(applyDepartmentFilter(inventoryItemService.searchByName(query)));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/low-stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> getLowStockItems(HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.ok(applyDepartmentFilter(inventoryItemService.getLowStockItems()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StockResponse> getTotalStock(@PathVariable String id, HttpServletRequest request) {
        try {
            if (!checkInventoryPermission(request, "READ", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            InventoryItem item = inventoryItemService.get(Long.valueOf(id));
            if (item == null)
                return ResponseEntity.notFound().build();
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted() && !ctx.isInActiveDepartment(item.getDepartmentId())) {
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
            if (!checkInventoryPermission(request, "CREATE", null)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            item.setSysUserId(sysUserId);

            // Anchor department_id server-side for restricted users — never trust client
            // payload
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted()) {
                item.setDepartmentId(ctx.getActiveDepartmentId());
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
            InventoryItem existing = inventoryItemService.get(Long.valueOf(id));
            if (existing == null)
                return ResponseEntity.notFound().build();

            // Verify the existing item belongs to the user's active department
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted() && !ctx.isInActiveDepartment(existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!checkInventoryPermission(request, "UPDATE", existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            item.setId(Long.valueOf(id));
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            item.setSysUserId(String.valueOf(usd.getSystemUserId()));
            // Preserve the original department — never allow client to move item to another
            // dept
            item.setDepartmentId(existing.getDepartmentId());

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
            InventoryItem existing = inventoryItemService.get(Long.valueOf(id));
            if (existing == null)
                return ResponseEntity.notFound().build();
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted() && !ctx.isInActiveDepartment(existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!checkInventoryPermission(request, "UPDATE", existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            inventoryItemService.deactivateItem(Long.valueOf(id), String.valueOf(usd.getSystemUserId()));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> activate(@PathVariable String id, HttpServletRequest request) {
        try {
            InventoryItem existing = inventoryItemService.get(Long.valueOf(id));
            if (existing == null)
                return ResponseEntity.notFound().build();
            RbacContext ctx = RbacContext.get();
            if (ctx != null && !ctx.isUnrestricted() && !ctx.isInActiveDepartment(existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!checkInventoryPermission(request, "UPDATE", existing.getDepartmentId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            inventoryItemService.activateItem(Long.valueOf(id), String.valueOf(usd.getSystemUserId()));
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
