package org.openelisglobal.storage.controller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.form.SampleAssignmentForm;
import org.openelisglobal.storage.form.SampleDisposalForm;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private SampleItemDAO sampleItemDAO;

    @Autowired
    private StorageDashboardService storageDashboardService;

    @Autowired
    private IStatusService statusService;

    /**
     * Get all SampleItems with storage assignments GET /rest/storage/sample-items
     * Supports filtering by location and status (FR-065) Supports pagination
     * (OGC-150)
     * 
     * Response fields: - id: Numeric ID (String representation) - primary
     * identifier - sampleItemId: @deprecated Use 'id' field instead. Kept for
     * backward compatibility. - sampleItemExternalId: External ID - user-friendly
     * identifier (e.g., "EXT-1765401458866") - sampleAccessionNumber: Parent Sample
     * accession number - status: Current status ("active" or "disposed") -
     * location: Hierarchical location path (e.g., "Main Lab > Freezer 1 > Shelf A")
     * 
     * @param countOnly If "true", returns metrics only
     * @param location  Optional location filter (hierarchical path substring)
     * @param status    Optional status filter (active, disposed, etc.)
     * @param page      Page number (0-based, default: 0)
     * @param size      Page size (allowed: 25, 50, 100; default: 25)
     */
    @GetMapping("")
    public ResponseEntity<?> getSampleItems(@RequestParam(required = false) String countOnly,
            @RequestParam(required = false) String location, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        try {
            logger.info("OGC-150 getSampleItems request: countOnly={}, location={}, status={}, page={}, size={}",
                    countOnly, location, status, page, size);
            // OGC-150: Validate pagination parameters
            if (page < 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Page number must be >= 0");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if (!Arrays.asList(5, 25, 50, 100).contains(size)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid page size. Allowed values: 5, 25, 50, 100");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            if ("true".equals(countOnly)) {
                // Return count metrics only
                List<SampleStorageAssignment> allAssignments = sampleStorageAssignmentDAO.getAll();

                long totalSampleItems = allAssignments.size();
                long active = 0;
                long disposed = 0;

                // Load SampleItem for each assignment to check status
                // sampleItem is now @Transient, so we need to load it manually
                // assignment.getSampleItemId() is Integer, sampleItemDAO.get() expects String
                for (SampleStorageAssignment assignment : allAssignments) {
                    if (assignment.getSampleItemId() != null) {
                        String sampleItemIdStr = assignment.getSampleItemId().toString();
                        Optional<SampleItem> sampleItemOpt = sampleItemDAO.get(sampleItemIdStr);
                        if (sampleItemOpt.isPresent()) {
                            SampleItem sampleItem = sampleItemOpt.get();
                            if (sampleItem.getStatusId() == null
                                    || !statusService.matches(sampleItem.getStatusId(), SampleStatus.Disposed)) {
                                active++;
                            } else {
                                disposed++;
                            }
                        }
                    }
                }

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
            } else if (StringUtils.hasText(location) || StringUtils.hasText(status)) {
                // Filter branch: wrap filtered results with pagination metadata to keep
                // response
                // consistent
                List<Map<String, Object>> filtered = storageDashboardService.filterSamples(location, status);

                int total = filtered.size();
                int fromIndex = Math.min(page * size, total);
                int toIndex = Math.min(fromIndex + size, total);
                List<Map<String, Object>> pageContent = filtered.subList(fromIndex, toIndex);

                Map<String, Object> response = new HashMap<>();
                response.put("items", pageContent);
                response.put("currentPage", page);
                response.put("totalPages", (int) Math.ceil(total / (double) size));
                response.put("totalItems", total);
                response.put("pageSize", size);

                logger.info("OGC-150 filter branch: filtered={}, pageContent={}, page={}, size={}, total={}", total,
                        pageContent.size(), page, size, total);
                return ResponseEntity.ok(response);
            } else {
                // OGC-150: No filters - reuse existing filterSamples logic and paginate the
                // result to
                // ensure consistent DTO shape (maps with sample fields populated)
                List<Map<String, Object>> all = storageDashboardService.filterSamples(null, null);
                int total = all.size();
                int fromIndex = Math.min(page * size, total);
                int toIndex = Math.min(fromIndex + size, total);
                List<Map<String, Object>> pageContent = all.subList(fromIndex, toIndex);

                Map<String, Object> response = new HashMap<>();
                response.put("items", pageContent);
                response.put("currentPage", page);
                response.put("totalPages", (int) Math.ceil(total / (double) size));
                response.put("totalItems", total);
                response.put("pageSize", size);

                logger.info("OGC-150 page branch (map-based): page={} size={} total={} contentSize={}", page, size,
                        total, pageContent.size());

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error getting SampleItems", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get storage location for a specific SampleItem by ID GET
     * /rest/storage/sample-items/{sampleItemId}
     */
    @GetMapping("/{sampleItemId}")
    public ResponseEntity<Map<String, Object>> getSampleItemLocation(@PathVariable String sampleItemId) {
        try {
            if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> location = sampleStorageService.getSampleItemLocation(sampleItemId);
            if (location == null || location.isEmpty()) {
                // Return empty location (not assigned yet)
                Map<String, Object> response = new HashMap<>();
                response.put("sampleItemId", sampleItemId);
                response.put("location", "");
                response.put("hierarchicalPath", "");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            logger.error("Error getting location for SampleItem: " + sampleItemId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Assign SampleItem to storage position POST /rest/storage/sample-items/assign
     * 
     * Accepts: External ID, accession number, or numeric ID (flexible identifier
     * resolution via resolveSampleItem())
     * 
     * @param form SampleAssignmentForm containing sampleItemId (flexible
     *             identifier), locationId, locationType, etc.
     * @return Assignment details including hierarchical location path
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
            logger.error("Error during sample assignment: " + e.getMessage(), e);
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
                    form.getLocationId(), form.getLocationType(), form.getPositionCoordinate(), form.getReason(),
                    form.getNotes());

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

    /**
     * Update position and/or notes for existing assignment without changing
     * location PATCH /rest/storage/sample-items/{sampleItemId}
     */
    @PatchMapping("/{sampleItemId}")
    public ResponseEntity<Map<String, Object>> updateAssignmentMetadata(@PathVariable String sampleItemId,
            @RequestBody Map<String, String> updates) {
        try {
            // Validate sampleItemId
            if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("message", "SampleItem ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Extract position and notes from request body (null means don't update, empty
            // string means clear)
            String positionCoordinate = updates.get("positionCoordinate");
            String notes = updates.get("notes");

            // Log incoming request for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Updating metadata for SampleItem {}: positionCoordinate={}, notes={}", sampleItemId,
                        positionCoordinate, notes != null ? "provided" : "null");
            }

            // Service layer handles update
            Map<String, Object> response = sampleStorageService.updateAssignmentMetadata(sampleItemId,
                    positionCoordinate, notes);

            // Log successful update
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} metadata updated successfully", sampleItemId);
            }

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (org.openelisglobal.common.exception.LIMSRuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            logger.error("Error updating SampleItem metadata", e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during update: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Dispose SampleItem POST /rest/storage/sample-items/dispose Marks sample as
     * disposed and clears storage location
     * 
     * Accepts: External ID, accession number, or numeric ID (flexible identifier
     * resolution via resolveSampleItem()) Response includes sampleItemId (numeric
     * ID) for consistency with other endpoints.
     * 
     * @param form SampleDisposalForm containing sampleItemId (flexible identifier),
     *             reason, method, notes
     * @return Disposal details including previous location and disposal timestamp
     */
    @PostMapping("/dispose")
    public ResponseEntity<Map<String, Object>> disposeSampleItem(@Valid @RequestBody SampleDisposalForm form) {
        try {
            // Log incoming request for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Disposing SampleItem {}: reason={}, method={}", form.getSampleItemId(), form.getReason(),
                        form.getMethod());
            }

            // Service layer handles all business logic
            Map<String, Object> response = sampleStorageService.disposeSampleItem(form.getSampleItemId(),
                    form.getReason(), form.getMethod(), form.getNotes());

            // Log successful disposal
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} disposed successfully", form.getSampleItemId());
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
            logger.error("Error disposing SampleItem", e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "An error occurred during disposal: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
