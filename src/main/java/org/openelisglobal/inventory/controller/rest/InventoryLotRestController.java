package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.util.List;
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
import org.openelisglobal.storage.service.SampleStorageService;
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

    @Autowired
    private SampleStorageService sampleStorageService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryLot>> getAll() {
        try {
            List<InventoryLot> lots = inventoryLotService.getAll();
            attachLocations(lots);
            return ResponseEntity.ok(lots);
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
            java.util.Map<String, Object> location = sampleStorageService.getInventoryLotLocation(id);
            // getInventoryLotLocation always returns a Map (empty, not null) when
            // unassigned; normalize to null here so Jackson's Include.NON_NULL omits
            // "location" entirely, consistent with the bulk lookup used by getAll().
            lot.setLocation(location.isEmpty() ? null : location);
            return ResponseEntity.ok(lot);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Bulk-attach each lot's current storage location (OGC-657), avoiding an N+1
     * lookup against sample_storage_assignment for dashboard-sized lot lists.
     */
    private void attachLocations(List<InventoryLot> lots) {
        if (lots == null || lots.isEmpty()) {
            return;
        }
        List<Long> lotIds = lots.stream().map(InventoryLot::getId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        java.util.Map<String, java.util.Map<String, Object>> locationsByLotId = sampleStorageService
                .getLocationsForInventoryLots(lotIds);
        for (InventoryLot lot : lots) {
            if (lot.getId() != null) {
                lot.setLocation(locationsByLotId.get(lot.getId().toString()));
            }
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

}
