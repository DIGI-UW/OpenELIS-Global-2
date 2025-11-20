package org.openelisglobal.inventory.controller.rest;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.inventory.action.InventoryUtility;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.inventory.form.InventoryKitItemForm;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLocationService;
import org.openelisglobal.inventory.service.InventoryReceiptService;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.spring.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Inventory Management Handles CRUD operations for test
 * kits (inventory items)
 */
@RestController
@RequestMapping("/rest/inventory")
public class InventoryRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryRestController.class);

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryLocationService inventoryLocationService;

    @Autowired
    private InventoryReceiptService inventoryReceiptService;

    @Autowired
    private OrganizationService organizationService;

    /**
     * Get all inventory items (test kits) GET /rest/inventory
     */
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllInventoryItems(
            @RequestParam(required = false) Boolean activeOnly) {
        try {
            InventoryUtility utility = SpringContext.getBean(InventoryUtility.class);
            List<InventoryKitItem> items;

            if (activeOnly != null && activeOnly) {
                items = utility.getExistingActiveInventory();
            } else {
                items = utility.getExistingInventory();
            }

            List<Map<String, Object>> response = new ArrayList<>();
            for (InventoryKitItem item : items) {
                response.add(inventoryKitItemToMap(item));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting inventory items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get inventory item by ID GET /rest/inventory/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getInventoryItemById(@PathVariable String id) {
        try {
            InventoryUtility utility = SpringContext.getBean(InventoryUtility.class);
            List<InventoryKitItem> items = utility.getExistingInventory();

            InventoryKitItem found = items.stream().filter(item -> id.equals(item.getInventoryItemId())).findFirst()
                    .orElse(null);

            if (found == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(inventoryKitItemToMap(found));
        } catch (Exception e) {
            logger.error("Error getting inventory item by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new inventory item POST /rest/inventory
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createInventoryItem(@Valid @RequestBody InventoryKitItemForm form) {
        try {
            // Validate kit name is unique
            List<InventoryItem> existingItems = inventoryItemService.getAllInventoryItems();
            for (InventoryItem item : existingItems) {
                if (form.getKitName().equals(item.getName())) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Duplicate kit name");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }

            // Create InventoryItem
            InventoryItem item = new InventoryItem();
            item.setName(form.getKitName());
            item.setDescription(form.getType());
            item.setIsActive("Y");
            item.setSysUserId(getSysUserId());

            inventoryItemService.insert(item);
            String itemId = item.getId();

            // Create InventoryLocation
            InventoryLocation location = new InventoryLocation();
            location.setInventoryItemId(itemId);
            location.setLotNumber(form.getLotNumber());
            if (form.getExpirationDate() != null && !form.getExpirationDate().isEmpty()) {
                location.setExpirationDate(DateUtil.convertStringDateToTruncatedTimestamp(form.getExpirationDate()));
            }
            location.setSysUserId(getSysUserId());

            inventoryLocationService.insert(location);

            // Create InventoryReceipt
            InventoryReceipt receipt = new InventoryReceipt();
            receipt.setInventoryItemId(itemId);
            if (form.getReceiveDate() != null && !form.getReceiveDate().isEmpty()) {
                receipt.setReceivedDate(DateUtil.convertStringDateToTruncatedTimestamp(form.getReceiveDate()));
            }
            if (form.getOrganizationId() != null && !form.getOrganizationId().isEmpty()) {
                Organization org = new Organization();
                org.setId(form.getOrganizationId());
                organizationService.getData(org);
                receipt.setOrganization(org);
            }
            receipt.setSysUserId(getSysUserId());

            inventoryReceiptService.insert(receipt);

            // Return created item
            InventoryUtility utility = SpringContext.getBean(InventoryUtility.class);
            List<InventoryKitItem> items = utility.getExistingInventory();
            InventoryKitItem created = items.stream().filter(i -> itemId.equals(i.getInventoryItemId())).findFirst()
                    .orElse(null);

            if (created != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(inventoryKitItemToMap(created));
            } else {
                return ResponseEntity.status(HttpStatus.CREATED).build();
            }
        } catch (Exception e) {
            logger.error("Error creating inventory item", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Update inventory item PUT /rest/inventory/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateInventoryItem(@PathVariable String id,
            @Valid @RequestBody InventoryKitItemForm form) {
        try {
            InventoryItem item = inventoryItemService.get(id);
            if (item == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Update InventoryItem
            item.setName(form.getKitName());
            item.setDescription(form.getType());
            item.setIsActive(form.getIsActive() != null && form.getIsActive() ? "Y" : "N");
            item.setSysUserId(getSysUserId());

            inventoryItemService.update(item);

            // Update InventoryLocation
            List<InventoryLocation> locations = inventoryLocationService.getAll();
            InventoryLocation location = locations.stream().filter(loc -> id.equals(loc.getInventoryItemId()))
                    .findFirst().orElse(null);

            if (location != null) {
                location.setLotNumber(form.getLotNumber());
                if (form.getExpirationDate() != null && !form.getExpirationDate().isEmpty()) {
                    location.setExpirationDate(
                            DateUtil.convertStringDateToTruncatedTimestamp(form.getExpirationDate()));
                }
                location.setSysUserId(getSysUserId());
                inventoryLocationService.update(location);
            }

            // Update InventoryReceipt
            InventoryReceipt receipt = inventoryReceiptService.getInventoryReceiptByInventoryItemId(id);
            if (receipt != null) {
                if (form.getReceiveDate() != null && !form.getReceiveDate().isEmpty()) {
                    receipt.setReceivedDate(DateUtil.convertStringDateToTruncatedTimestamp(form.getReceiveDate()));
                }
                if (form.getOrganizationId() != null && !form.getOrganizationId().isEmpty()) {
                    Organization org = new Organization();
                    org.setId(form.getOrganizationId());
                    organizationService.getData(org);
                    receipt.setOrganization(org);
                }
                receipt.setSysUserId(getSysUserId());
                inventoryReceiptService.update(receipt);
            }

            // Return updated item
            InventoryUtility utility = SpringContext.getBean(InventoryUtility.class);
            List<InventoryKitItem> items = utility.getExistingInventory();
            InventoryKitItem updated = items.stream().filter(i -> id.equals(i.getInventoryItemId())).findFirst()
                    .orElse(null);

            if (updated != null) {
                return ResponseEntity.ok(inventoryKitItemToMap(updated));
            } else {
                return ResponseEntity.ok().build();
            }
        } catch (Exception e) {
            logger.error("Error updating inventory item", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get kit types (HIV, SYPHILIS) GET /rest/inventory/kit-types
     */
    @GetMapping("/kit-types")
    public ResponseEntity<List<String>> getKitTypes() {
        List<String> types = new ArrayList<>();
        types.add(InventoryUtility.HIV);
        types.add(InventoryUtility.SYPHILIS);
        return ResponseEntity.ok(types);
    }

    /**
     * Get sources (organizations with type TestKitVender) GET
     * /rest/inventory/sources
     */
    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, Object>>> getSources() {
        try {
            List<Organization> organizations = organizationService.getOrganizationsByTypeName("organizationName",
                    "TestKitVender");

            List<Map<String, Object>> response = new ArrayList<>();
            for (Organization org : organizations) {
                Map<String, Object> orgMap = new HashMap<>();
                orgMap.put("id", org.getId());
                orgMap.put("name", org.getOrganizationName());
                response.add(orgMap);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting sources", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> inventoryKitItemToMap(InventoryKitItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getInventoryItemId());
        map.put("inventoryItemId", item.getInventoryItemId());
        map.put("inventoryLocationId", item.getInventoryLocationId());
        map.put("inventoryReceiptId", item.getInventoryReceiptId());
        map.put("kitName", item.getKitName());
        map.put("type", item.getType());
        map.put("receiveDate", item.getReceiveDate());
        map.put("expirationDate", item.getExpirationDate());
        map.put("lotNumber", item.getLotNumber());
        map.put("source", item.getSource());
        map.put("organizationId", item.getOrganizationId());
        map.put("isActive", item.getIsActive());
        return map;
    }

    private String getSysUserId() {
        // TODO: Get from security context
        return "1";
    }
}
