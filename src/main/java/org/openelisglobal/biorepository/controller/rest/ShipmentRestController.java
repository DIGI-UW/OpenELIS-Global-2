package org.openelisglobal.biorepository.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.service.BioSampleService;
import org.openelisglobal.biorepository.service.ShipmentService;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.Shipment;
import org.openelisglobal.biorepository.valueholder.Shipment.ShipmentStatus;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * REST controller for Shipment operations in the Biorepository module.
 */
@RestController
@RequestMapping(value = "/rest/biorepository/shipment")
public class ShipmentRestController extends BaseRestController {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private BioSampleService bioSampleService;

    @Autowired
    private SystemUserService systemUserService;

    /**
     * Get all shipments with optional status filter.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Shipment>> getShipments(@RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "50") int limit) {

        List<Shipment> shipments;
        if (status != null) {
            shipments = shipmentService.getByStatus(status);
        } else {
            shipments = shipmentService.getRecentShipments(offset, limit);
        }

        return ResponseEntity.ok(shipments);
    }

    /**
     * Get a shipment by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Shipment> getShipment(@PathVariable("id") Integer id) {
        Shipment shipment = shipmentService.get(id);
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(shipment);
    }

    /**
     * Get a shipment by delivery reference.
     */
    @GetMapping(value = "/by-reference/{deliveryReference}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Shipment> getShipmentByReference(
            @PathVariable("deliveryReference") String deliveryReference) {
        Shipment shipment = shipmentService.getByDeliveryReference(deliveryReference);
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(shipment);
    }

    /**
     * Search shipments by sender name, organization, or delivery reference.
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Shipment>> searchShipments(@RequestParam String query,
            @RequestParam(required = false, defaultValue = "20") int limit) {

        List<Shipment> results = shipmentService.search(query, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Receive a new shipment.
     */
    @PostMapping(value = "/receive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveShipment(@RequestBody Shipment shipment, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found. Please log in again."));
        }

        try {
            // Set receiver
            shipment.setReceiver(systemUserService.get(sysUserId));
            shipment.setSysUserId(sysUserId);
            Shipment received = shipmentService.receiveShipment(shipment);

            return ResponseEntity.ok(Map.of("id", received.getId(), "deliveryReference",
                    received.getDeliveryReference() != null ? received.getDeliveryReference() : "", "status",
                    received.getStatus().name()));

        } catch (LIMSDuplicateRecordException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to receive shipment: " + e.getMessage()));
        }
    }

    /**
     * Update a shipment.
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateShipment(@PathVariable("id") Integer id, @RequestBody Shipment shipmentData,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);

        Shipment existing = shipmentService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // Update allowed fields
        if (shipmentData.getSenderName() != null) {
            existing.setSenderName(shipmentData.getSenderName());
        }
        if (shipmentData.getSenderOrganization() != null) {
            existing.setSenderOrganization(shipmentData.getSenderOrganization());
        }
        if (shipmentData.getPackagingCondition() != null) {
            existing.setPackagingCondition(shipmentData.getPackagingCondition());
        }
        if (shipmentData.getPackagingPhotoPath() != null) {
            existing.setPackagingPhotoPath(shipmentData.getPackagingPhotoPath());
        }
        if (shipmentData.getTransportTemperature() != null) {
            existing.setTransportTemperature(shipmentData.getTransportTemperature());
        }
        if (shipmentData.getExpectedSampleCount() != null) {
            existing.setExpectedSampleCount(shipmentData.getExpectedSampleCount());
        }
        if (shipmentData.getPackagingConditionNotes() != null) {
            existing.setPackagingConditionNotes(shipmentData.getPackagingConditionNotes());
        }

        existing.setSysUserId(sysUserId);

        try {
            Shipment updated = shipmentService.update(existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update shipment: " + e.getMessage()));
        }
    }

    /**
     * Complete a shipment (mark as COMPLETED).
     */
    @PostMapping(value = "/{id}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeShipment(@PathVariable("id") Integer id, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);

        try {
            Shipment shipment = shipmentService.get(id);
            if (shipment == null) {
                return ResponseEntity.notFound().build();
            }

            shipment.setSysUserId(sysUserId);
            Shipment completed = shipmentService.completeShipment(id);

            return ResponseEntity.ok(Map.of("id", completed.getId(), "status", completed.getStatus().name()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all samples for a shipment.
     */
    @GetMapping(value = "/{id}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BioSample>> getShipmentSamples(@PathVariable("id") Integer id) {
        Shipment shipment = shipmentService.get(id);
        if (shipment == null) {
            return ResponseEntity.notFound().build();
        }

        List<BioSample> samples = bioSampleService.getByShipmentId(id);
        return ResponseEntity.ok(samples);
    }

    /**
     * Get shipment statistics by status.
     */
    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getShipmentStats() {
        Map<String, Long> stats = Map.of("received", shipmentService.countByStatus(ShipmentStatus.RECEIVED),
                "processing", shipmentService.countByStatus(ShipmentStatus.PROCESSING), "completed",
                shipmentService.countByStatus(ShipmentStatus.COMPLETED));

        return ResponseEntity.ok(stats);
    }

    /**
     * Get shipments within a date range.
     */
    @GetMapping(value = "/by-date-range", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Shipment>> getShipmentsByDateRange(@RequestParam long startDate,
            @RequestParam long endDate) {

        List<Shipment> shipments = shipmentService.getByDateRange(new Timestamp(startDate), new Timestamp(endDate));

        return ResponseEntity.ok(shipments);
    }
}
