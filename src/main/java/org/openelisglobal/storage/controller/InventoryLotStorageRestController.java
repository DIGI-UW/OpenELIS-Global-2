package org.openelisglobal.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.storage.form.InventoryLotAssignmentForm;
import org.openelisglobal.storage.form.InventoryLotMovementForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for InventoryLot storage operations (OGC-657). Mirrors
 * {@link SampleStorageRestController}'s assign/move/movements shape, reusing
 * the same {@link SampleStorageService} occupant-generalized methods so lots
 * share one audit trail and one picker with samples.
 */
@RestController
@RequestMapping("/rest/storage/inventory-lots")
public class InventoryLotStorageRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryLotStorageRestController.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    /**
     * List every InventoryLot with a storage assignment, the lot counterpart of GET
     * /rest/storage/sample-items. Backs the Storage Management lots view.
     */
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getInventoryLots() {
        try {
            return ResponseEntity.ok(sampleStorageService.getAllInventoryLotsWithAssignments());
        } catch (Exception e) {
            logger.error("Error listing inventory lots with assignments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an assignment's position and notes without moving the lot, the lot
     * counterpart of PATCH /rest/storage/sample-items/{sampleItemId}.
     */
    @PatchMapping("/{inventoryLotId}")
    public ResponseEntity<Map<String, Object>> updateAssignmentMetadata(@PathVariable String inventoryLotId,
            @RequestBody Map<String, String> updates) {
        try {
            Map<String, Object> result = sampleStorageService.updateInventoryLotAssignmentMetadata(inventoryLotId,
                    updates.get("positionCoordinate"), updates.get("notes"));
            return ResponseEntity.ok(result);
        } catch (LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating inventory lot assignment metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get storage location for a specific InventoryLot GET
     * /rest/storage/inventory-lots/{inventoryLotId}
     */
    @GetMapping("/{inventoryLotId}")
    public ResponseEntity<Map<String, Object>> getInventoryLotLocation(@PathVariable String inventoryLotId) {
        try {
            Map<String, Object> location = sampleStorageService.getInventoryLotLocation(inventoryLotId);
            if (location == null || location.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("inventoryLotId", inventoryLotId);
                response.put("location", "");
                response.put("hierarchicalPath", "");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(location);
        } catch (LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error getting location for InventoryLot: " + inventoryLotId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * List movement-audit rows for an InventoryLot GET
     * /rest/storage/inventory-lots/{inventoryLotId}/movements
     */
    @GetMapping("/{inventoryLotId}/movements")
    public ResponseEntity<List<Map<String, Object>>> getInventoryLotMovements(@PathVariable String inventoryLotId) {
        try {
            return ResponseEntity.ok(sampleStorageService.getInventoryLotMovementsWithUserNames(inventoryLotId));
        } catch (LIMSRuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            logger.error("Error getting movements for InventoryLot: " + inventoryLotId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign an InventoryLot to a storage position POST
     * /rest/storage/inventory-lots/assign
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignInventoryLot(@Valid @RequestBody InventoryLotAssignmentForm form,
            HttpServletRequest request) {
        try {
            String sysUserId = ControllerUtills.getSysUserId(request);
            Map<String, Object> response = sampleStorageService.assignInventoryLotWithLocation(form.getInventoryLotId(),
                    form.getLocationId(), form.getLocationType(), form.getPositionCoordinate(), form.getNotes(),
                    sysUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (LIMSRuntimeException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error during inventory lot assignment: " + e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during assignment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Move an InventoryLot to a new storage position POST
     * /rest/storage/inventory-lots/move
     */
    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> moveInventoryLot(@Valid @RequestBody InventoryLotMovementForm form,
            HttpServletRequest request) {
        try {
            String sysUserId = ControllerUtills.getSysUserId(request);
            String movementId = sampleStorageService.moveInventoryLotWithLocation(form.getInventoryLotId(),
                    form.getLocationId(), form.getLocationType(), form.getPositionCoordinate(), form.getReason(),
                    form.getNotes(), sysUserId);

            Map<String, Object> updatedLocation = sampleStorageService
                    .getInventoryLotLocation(form.getInventoryLotId());

            Map<String, Object> response = new HashMap<>();
            response.put("movementId", movementId);
            response.put("newHierarchicalPath", updatedLocation.getOrDefault("hierarchicalPath", "Unknown"));
            response.put("movedDate", new java.sql.Timestamp(System.currentTimeMillis()).toString());
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException | IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error moving InventoryLot", e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during movement: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
