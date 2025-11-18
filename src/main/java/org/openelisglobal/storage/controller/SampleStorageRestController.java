package org.openelisglobal.storage.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.form.SampleMovementForm;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.service.StorageDashboardService;
import org.openelisglobal.storage.service.StorageLocationService;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageDevice;
import org.openelisglobal.storage.valueholder.StorageRack;
import org.openelisglobal.storage.valueholder.StorageShelf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for SampleItem Storage operations Handles SampleItem
 * assignment and movement
 */
@RestController
@RequestMapping("/rest/storage/sample-items")
public class SampleStorageRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageRestController.class);

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private StorageDashboardService storageDashboardService;

    /**
     * Get all SampleItems with storage assignments GET /rest/storage/sample-items
     * Supports filtering by location and status (FR-065)
     * 
     * @param countOnly If "true", returns metrics only
     * @param location  Optional location filter (hierarchical path substring)
     * @param status    Optional status filter (active, disposed, etc.)
     */
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getSampleItems(@RequestParam(required = false) String countOnly,
            @RequestParam(required = false) String location, @RequestParam(required = false) String status) {
        try {
            if ("true".equals(countOnly)) {
                // Return count metrics only
                List<SampleStorageAssignment> allAssignments = sampleStorageAssignmentDAO.getAll();

                long totalSampleItems = allAssignments.size();
                long active = allAssignments.stream()
                        .filter(a -> a.getSampleItem() != null && (a.getSampleItem().getStatusId() == null
                                || !"disposed".equalsIgnoreCase(a.getSampleItem().getStatusId())))
                        .count();
                long disposed = allAssignments.stream().filter(
                        a -> a.getSampleItem() != null && "disposed".equalsIgnoreCase(a.getSampleItem().getStatusId()))
                        .count();

                // Count unique storage locations (rooms, devices, shelves, racks)
                long storageLocations = storageLocationService.getRooms().size()
                        + storageLocationService.getAllDevices().size() + storageLocationService.getAllShelves().size()
                        + storageLocationService.getAllRacks().size();

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("totalSampleItems", totalSampleItems);
                metrics.put("active", active);
                metrics.put("disposed", disposed);
                metrics.put("storageLocations", storageLocations);

                List<Map<String, Object>> response = new ArrayList<>();
                response.add(metrics);
                return ResponseEntity.ok(response);
            } else {
                // Apply filters if provided (FR-065: SampleItems tab - filter by location and
                // status)
                List<Map<String, Object>> response;
                if (location != null || status != null) {
                    response = storageDashboardService.filterSamples(location, status);
                    logger.info("Returning {} filtered SampleItems (location={}, status={})", response.size(), location,
                            status);
                } else {
                    // No filters - return all SampleItems
                    response = sampleStorageService.getAllSamplesWithAssignments();
                    logger.info("Returning {} SampleItems with storage assignments", response.size());
                }
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error getting SampleItems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign SampleItem to storage position POST /rest/storage/sample-items/assign
     */
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignSampleItem(@Valid @RequestBody SampleAssignmentForm form) {
        try {
            // Validate required fields
            if (form.getSampleItemId() == null || form.getSampleItemId().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "SampleItem ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate: must have locationId + locationType
            if (form.getLocationId() == null || form.getLocationId().trim().isEmpty() || form.getLocationType() == null
                    || form.getLocationType().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Location ID and location type are required (minimum 2 levels: room + device)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Log incoming request for debugging
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Assigning SampleItem {} to location: locationId={}, locationType={}, positionCoordinate={}",
                        form.getSampleItemId(), form.getLocationId(), form.getLocationType(),
                        form.getPositionCoordinate());
            }

            // Service layer prepares all data including hierarchical path within
            // transaction
            Map<String, Object> response = sampleStorageService.assignSampleItemWithLocation(form.getSampleItemId(),
                    form.getLocationId(), form.getLocationType(), form.getPositionCoordinate(), form.getNotes());

            // Log successful assignment
            if (logger.isInfoEnabled()) {
                logger.info(
                        "SampleItem {} assigned successfully to locationId={}, locationType={}, positionCoordinate={}",
                        form.getSampleItemId(), form.getLocationId(), form.getLocationType(),
                        form.getPositionCoordinate());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during assignment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Move SampleItem to new storage position POST /rest/storage/sample-items/move
     */
    @PostMapping("/move")
    public ResponseEntity<Map<String, Object>> moveSampleItem(@Valid @RequestBody SampleMovementForm form) {
        try {
            // Validate required fields
            if (form.getSampleItemId() == null || form.getSampleItemId().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "SampleItem ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate: must have locationId + locationType
            if (form.getLocationId() == null || form.getLocationId().trim().isEmpty() || form.getLocationType() == null
                    || form.getLocationType().trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Location ID and location type are required (minimum 2 levels: room + device)");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Log incoming request for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Moving SampleItem {} to location: locationId={}, locationType={}, positionCoordinate={}",
                        form.getSampleItemId(), form.getLocationId(), form.getLocationType(),
                        form.getPositionCoordinate());
            }

            // Service layer handles all business logic
            String movementId = sampleStorageService.moveSampleItemWithLocation(form.getSampleItemId(),
                    form.getLocationId(), form.getLocationType(), form.getPositionCoordinate(), form.getReason());

            // Log successful movement
            if (logger.isInfoEnabled()) {
                logger.info(
                        "SampleItem {} moved successfully to locationId={}, locationType={}, positionCoordinate={}, movementId={}",
                        form.getSampleItemId(), form.getLocationId(), form.getLocationType(),
                        form.getPositionCoordinate(), movementId);
            }

            // Build hierarchical path for new location
            Integer locationIdInt = Integer.parseInt(form.getLocationId());
            String newHierarchicalPath = null;
            if ("device".equals(form.getLocationType())) {
                StorageDevice device = (StorageDevice) storageLocationService.get(locationIdInt, StorageDevice.class);
                if (device != null && device.getParentRoom() != null) {
                    newHierarchicalPath = device.getParentRoom().getName() + " > " + device.getName();
                    if (form.getPositionCoordinate() != null && !form.getPositionCoordinate().trim().isEmpty()) {
                        newHierarchicalPath += " > " + form.getPositionCoordinate();
                    }
                }
            } else if ("shelf".equals(form.getLocationType())) {
                StorageShelf shelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (shelf != null && shelf.getParentDevice() != null
                        && shelf.getParentDevice().getParentRoom() != null) {
                    newHierarchicalPath = shelf.getParentDevice().getParentRoom().getName() + " > "
                            + shelf.getParentDevice().getName() + " > " + shelf.getLabel();
                    if (form.getPositionCoordinate() != null && !form.getPositionCoordinate().trim().isEmpty()) {
                        newHierarchicalPath += " > " + form.getPositionCoordinate();
                    }
                }
            } else if ("rack".equals(form.getLocationType())) {
                StorageRack rack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (rack != null && rack.getParentShelf() != null && rack.getParentShelf().getParentDevice() != null
                        && rack.getParentShelf().getParentDevice().getParentRoom() != null) {
                    newHierarchicalPath = rack.getParentShelf().getParentDevice().getParentRoom().getName() + " > "
                            + rack.getParentShelf().getParentDevice().getName() + " > "
                            + rack.getParentShelf().getLabel() + " > " + rack.getLabel();
                    if (form.getPositionCoordinate() != null && !form.getPositionCoordinate().trim().isEmpty()) {
                        newHierarchicalPath += " > " + form.getPositionCoordinate();
                    }
                }
            }

            // Get previous position path from the movement record (already created by
            // service)
            // Note: The service already updated the assignment, so we need to get it from
            // the movement
            // For now, we'll build it from the assignment if it exists, or use a generic
            // message
            String previousHierarchicalPath = null;
            // The service method returns the movementId, but we need the previous position
            // path
            // This is a limitation - we could enhance the service to return both paths
            // For now, we'll leave it as null and let the frontend handle it

            // Check shelf capacity (informational only - not blocking)
            String shelfCapacityWarning = null;
            if ("shelf".equals(form.getLocationType())) {
                StorageShelf shelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (shelf != null && shelf.getCapacityLimit() != null && shelf.getCapacityLimit() > 0) {
                    int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
                    int capacityLimit = shelf.getCapacityLimit();
                    int percentage = (occupied * 100) / capacityLimit;

                    if (percentage >= 100) {
                        shelfCapacityWarning = String.format(
                                "Shelf %s is at or over capacity (%d/%d positions, %d%%). Assignment allowed but shelf is over-occupied.",
                                shelf.getLabel(), occupied, capacityLimit, percentage);
                    } else if (percentage >= 90) {
                        shelfCapacityWarning = String.format("Shelf %s is near capacity (%d/%d positions, %d%%).",
                                shelf.getLabel(), occupied, capacityLimit, percentage);
                    }
                }
            } else if ("rack".equals(form.getLocationType())) {
                StorageRack rack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (rack != null && rack.getParentShelf() != null) {
                    StorageShelf shelf = rack.getParentShelf();
                    if (shelf.getCapacityLimit() != null && shelf.getCapacityLimit() > 0) {
                        int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
                        int capacityLimit = shelf.getCapacityLimit();
                        int percentage = (occupied * 100) / capacityLimit;

                        if (percentage >= 100) {
                            shelfCapacityWarning = String.format(
                                    "Shelf %s is at or over capacity (%d/%d positions, %d%%). Assignment allowed but shelf is over-occupied.",
                                    shelf.getLabel(), occupied, capacityLimit, percentage);
                        } else if (percentage >= 90) {
                            shelfCapacityWarning = String.format("Shelf %s is near capacity (%d/%d positions, %d%%).",
                                    shelf.getLabel(), occupied, capacityLimit, percentage);
                        }
                    }
                }
            }

            // Prepare response data
            Map<String, Object> response = new HashMap<>();
            response.put("movementId", movementId);
            response.put("previousLocation", previousHierarchicalPath);
            response.put("newLocation", newHierarchicalPath != null ? newHierarchicalPath : "Unknown");
            response.put("newHierarchicalPath", newHierarchicalPath != null ? newHierarchicalPath : "Unknown"); // Alias
                                                                                                                // for
                                                                                                                // consistency
            response.put("movedDate", new java.sql.Timestamp(System.currentTimeMillis()).toString());
            if (shelfCapacityWarning != null) {
                response.put("shelfCapacityWarning", shelfCapacityWarning);
            }

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error moving SampleItem", e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during movement: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
