package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
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
@RequestMapping("/rest/inventory/lots")
public class InventoryLotRestController extends BaseRestController {

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getAll() {
        try {
            List<InventoryLot> lots = inventoryLotService.getAll();
            // Eagerly fetch inventoryItem to avoid lazy loading issues during JSON
            // serialization
            lots.forEach(lot -> {
                if (lot.getInventoryItem() != null) {
                    lot.getInventoryItem().getName(); // Force Hibernate to load the entity
                }
            });
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get paginated lots with filtering and sorting
     *
     * @param limit     Maximum number of results per page (default: 20, max: 1000)
     * @param offset    Number of results to skip (default: 0)
     * @param sortBy    Field to sort by (default: expirationDate)
     * @param sortOrder Sort direction: "asc" or "desc" (default: asc)
     * @param itemType  Filter by item type: REAGENT, RDT, CARTRIDGE, HIV_KIT,
     *                  SYPHILIS_KIT
     * @param status    Filter by lot status: ACTIVE, IN_USE, EXPIRED, CONSUMED,
     *                  DISPOSED, QUARANTINED
     * @param search    Search term for lot number or item name
     * @return Paginated response with lots and metadata
     */
    @GetMapping(value = "/paged", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getPagedLots(@RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "expirationDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder, @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String status, @RequestParam(required = false) String search) {
        try {
            // Parse status parameter
            LotStatus lotStatus = null;
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                try {
                    lotStatus = LotStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }

            // Get paginated lots (eagerly loaded with inventoryItem)
            List<InventoryLot> lots = inventoryLotService.getPagedLots(limit, offset, sortBy, sortOrder, itemType,
                    lotStatus, search);

            // Get total count for pagination metadata
            Long totalRecords = inventoryLotService.getPagedLotsCount(itemType, lotStatus, search);

            // Calculate pagination metadata
            int currentPage = (offset / limit) + 1;
            int totalPages = (int) Math.ceil((double) totalRecords / limit);
            boolean hasMore = offset + limit < totalRecords;

            // Build response following the existing pattern from
            // InventoryAuditLogRestController
            Map<String, Object> response = new HashMap<>();
            response.put("lots", lots);
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
    public ResponseEntity<InventoryLot> getById(@PathVariable String id) {
        try {
            InventoryLot lot = inventoryLotService.get(Long.valueOf(id));
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(lot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/item/{itemId}/available", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getAvailableLotsFEFO(@PathVariable String itemId) {
        try {
            List<InventoryLot> lots = inventoryLotService.getAvailableLotsByItemFEFO(Long.valueOf(itemId));
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/item/{itemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getByItemId(@PathVariable String itemId) {
        try {
            List<InventoryLot> lots = inventoryLotService.getByInventoryItemId(Long.valueOf(itemId));
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/expiring", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getExpiringLots(@RequestParam(defaultValue = "30") int days) {
        try {
            List<InventoryLot> lots = inventoryLotService.getExpiringLots(days);
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getExpiredActiveLots() {
        try {
            List<InventoryLot> lots = inventoryLotService.getExpiredActiveLots();
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/lot-number/{lotNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> getByLotNumber(@PathVariable String lotNumber) {
        try {
            InventoryLot lot = inventoryLotService.getByLotNumber(lotNumber);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(lot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/item/{itemId}/total-quantity", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuantityResponse> getTotalQuantity(@PathVariable String itemId) {
        try {
            Double quantity = inventoryLotService.getTotalCurrentQuantity(Long.valueOf(itemId));
            return ResponseEntity.ok(new QuantityResponse(quantity));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> create(@Valid @RequestBody InventoryLot lot, HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            lot.setSysUserId(sysUserId);

            // Generate FHIR UUID if not provided
            if (lot.getFhirUuid() == null) {
                lot.setFhirUuid(java.util.UUID.randomUUID());
            }

            // Fetch managed InventoryItem entity to avoid transient instance error
            if (lot.getInventoryItem() != null && lot.getInventoryItem().getId() != null) {
                Long itemId = lot.getInventoryItem().getId();
                InventoryItem managedItem = inventoryItemService.get(itemId);
                if (managedItem == null) {
                    return ResponseEntity.badRequest().build();
                }
                lot.setInventoryItem(managedItem);
            }

            InventoryLot savedLot = inventoryLotService.save(lot);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> update(@PathVariable String id, @Valid @RequestBody InventoryLot lot,
            HttpServletRequest request) {
        try {
            InventoryLot existingLot = inventoryLotService.get(Long.valueOf(id));
            if (existingLot == null) {
                return ResponseEntity.notFound().build();
            }

            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());
            lot.setId(Long.valueOf(id));
            lot.setSysUserId(sysUserId);

            // Preserve fhirUuid from existing lot (immutable field)
            if (lot.getFhirUuid() == null) {
                lot.setFhirUuid(existingLot.getFhirUuid());
            }

            // Fetch managed InventoryItem entity to avoid transient instance error
            if (lot.getInventoryItem() != null && lot.getInventoryItem().getId() != null) {
                Long itemId = lot.getInventoryItem().getId();
                InventoryItem managedItem = inventoryItemService.get(itemId);
                if (managedItem == null) {
                    return ResponseEntity.badRequest().build();
                }
                lot.setInventoryItem(managedItem);
            }

            InventoryLot updatedLot = inventoryLotService.update(lot);
            return ResponseEntity.ok(updatedLot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/open", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> openLot(@PathVariable String id,
            @RequestBody(required = false) OpenLotRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            Timestamp openedDate = request != null && request.getOpenedDate() != null ? request.getOpenedDate()
                    : new Timestamp(System.currentTimeMillis());

            InventoryLot lot = inventoryLotService.openLot(Long.valueOf(id), openedDate, sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/qc-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> updateQCStatus(@PathVariable String id, @RequestBody QCStatusRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            InventoryLot lot = inventoryLotService.updateQCStatus(Long.valueOf(id), request.getQcStatus(),
                    request.getNotes(), sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> updateStatus(@PathVariable String id, @RequestBody StatusRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            InventoryLot lot = inventoryLotService.updateLotStatus(Long.valueOf(id), request.getStatus(), sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/adjust", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> adjustQuantity(@PathVariable String id,
            @RequestBody AdjustQuantityRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            InventoryLot lot = inventoryLotService.adjustLotQuantity(Long.valueOf(id), request.getNewQuantity(),
                    request.getReason(), sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/dispose", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> disposeLot(@PathVariable String id,
            @RequestBody(required = false) DisposeRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            String reason = request != null ? request.getReason() : null;
            String notes = request != null ? request.getNotes() : null;
            InventoryLot lot = inventoryLotService.disposeLot(Long.valueOf(id), reason, notes, sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/batch-dispose", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchDisposeResponse> batchDispose(@RequestBody BatchDisposeRequest request,
            HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            int successCount = 0;
            int failedCount = 0;
            StringBuilder errors = new StringBuilder();

            for (Long lotId : request.getLotIds()) {
                try {
                    inventoryLotService.disposeLot(lotId, request.getReason(), request.getNotes(), sysUserId);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    errors.append("Lot ID ").append(lotId).append(": ").append(e.getMessage()).append("; ");
                    LogEvent.logWarn(this.getClass().getSimpleName(), "batchDispose",
                            "Failed to dispose lot " + lotId + ": " + e.getMessage());
                }
            }

            BatchDisposeResponse response = new BatchDisposeResponse(successCount, failedCount,
                    errors.length() > 0 ? errors.toString() : null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/process-expired", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessExpiredResponse> processExpired() {
        try {
            int count = inventoryLotService.processExpiredLots();
            return ResponseEntity.ok(new ProcessExpiredResponse(count));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update lot storage location using the unified storage hierarchy. This
     * endpoint replaces the legacy inventory_storage_location with the same storage
     * hierarchy used by sample storage (room > device > shelf > rack > box).
     */
    @PutMapping(value = "/{id}/storage-location", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryLot> updateStorageLocation(@PathVariable String id,
            @RequestBody StorageLocationRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            InventoryLot lot = inventoryLotService.updateStorageLocation(Long.valueOf(id), request.getLocationId(),
                    request.getLocationType(), request.getPositionCoordinate(), request.getStoragePath(), sysUserId);
            return ResponseEntity.ok(lot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get lots by unified storage location (room, device, shelf, rack, or box).
     */
    @GetMapping(value = "/unified-location", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getByUnifiedLocation(@RequestParam Integer locationId,
            @RequestParam String locationType) {
        try {
            List<InventoryLot> lots = inventoryLotService.getByUnifiedLocation(locationId, locationType);
            return ResponseEntity.ok(lots);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class OpenLotRequest {
        private Timestamp openedDate;
    }

    @Setter
    @Getter
    public static class QCStatusRequest {
        private QCStatus qcStatus;
        private String notes;
    }

    @Setter
    @Getter
    public static class StatusRequest {
        private LotStatus status;
    }

    @Setter
    @Getter
    public static class AdjustQuantityRequest {
        private Double newQuantity;
        private String reason;
    }

    @Setter
    @Getter
    public static class DisposeRequest {
        private String reason;
        private String notes;
    }

    @Setter
    @Getter
    public static class QuantityResponse {
        private Double quantity;

        public QuantityResponse(Double quantity) {
            this.quantity = quantity;
        }
    }

    @Setter
    @Getter
    public static class ProcessExpiredResponse {
        private Integer lotsUpdated;

        public ProcessExpiredResponse(Integer lotsUpdated) {
            this.lotsUpdated = lotsUpdated;
        }
    }

    @Setter
    @Getter
    public static class BatchDisposeRequest {
        private List<Long> lotIds;
        private String reason;
        private String notes;
    }

    @Setter
    @Getter
    public static class BatchDisposeResponse {
        private Integer successCount;
        private Integer failedCount;
        private String errors;

        public BatchDisposeResponse(Integer successCount, Integer failedCount, String errors) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }
    }

    /**
     * Request DTO for updating storage location using the unified storage
     * hierarchy. Supports assignment at any level: room, device, shelf, rack, box.
     */
    @Setter
    @Getter
    public static class StorageLocationRequest {
        /** The location ID (storage_room.id, storage_device.id, etc.) */
        private Integer locationId;
        /**
         * The type of location: 'room', 'device', 'shelf', 'rack', 'box', or 'general'
         */
        private String locationType;
        /** Optional position within the location (e.g., well coordinate in a box) */
        private String positionCoordinate;
        /** The hierarchical path string (e.g., "Room A > Freezer 1 > Shelf 2") */
        private String storagePath;

    }

}
