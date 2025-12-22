package org.openelisglobal.storage.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of SampleStorageService - Handles sample assignment and
 * movement
 */
@Service
@Transactional
public class SampleStorageServiceImpl implements SampleStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SampleStorageServiceImpl.class);

    @Autowired
    private SampleItemDAO sampleItemDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IStatusService statusService;

    @Override
    public CapacityWarning calculateCapacity(StorageRack rack) {
        // Calculate total capacity from boxes in this rack
        List<StorageBox> boxes = storageLocationService.getBoxesByRack(rack.getId());
        int totalCapacity = boxes.stream().mapToInt(box -> box.getCapacity() != null ? box.getCapacity() : 0).sum();

        if (totalCapacity == 0) {
            return null; // No boxes with capacity defined
        }

        int occupied = storageLocationService.countOccupied(rack.getId());
        int percentage = (occupied * 100) / totalCapacity;

        String warningMessage = null;
        if (percentage >= 100) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        } else if (percentage >= 90) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        } else if (percentage >= 80) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", rack.getLabel(),
                    percentage);
        }

        return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllSamplesWithAssignments() {
        // Get ALL sample items first, then LEFT JOIN with assignments
        List<SampleItem> allSampleItems = sampleItemDAO.getAllSampleItems();
        logger.info("getAllSamplesWithAssignments: Found {} total sample items", allSampleItems.size());

        // Get all assignments and build a map by sampleItemId for efficient lookup
        // SampleItem.id is String but assignment.sampleItemId is Integer (DB column is
        // numeric)
        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.getAll();
        java.util.Map<String, SampleStorageAssignment> assignmentsBySampleItemId = new java.util.HashMap<>();
        for (SampleStorageAssignment assignment : assignments) {
            if (assignment.getSampleItemId() != null) {
                // Convert Integer to String for map key (SampleItem.id is String)
                assignmentsBySampleItemId.put(assignment.getSampleItemId().toString(), assignment);
            }
        }
        logger.info("getAllSamplesWithAssignments: Found {} total assignments", assignments.size());

        List<Map<String, Object>> response = new java.util.ArrayList<>();

        for (SampleItem sampleItem : allSampleItems) {
            if (sampleItem == null || sampleItem.getId() == null) {
                continue;
            }

            Map<String, Object> map = new java.util.HashMap<>();
            // Numeric ID (String representation) - primary identifier
            map.put("id", sampleItem.getId());
            // @deprecated Use 'id' field instead. Kept for backward compatibility only.
            // This field is identical to 'id' and will be removed in a future release.
            map.put("sampleItemId", sampleItem.getId());
            // External ID - user-friendly identifier (e.g., "EXT-1765401458866")
            map.put("sampleItemExternalId", sampleItem.getExternalId() != null ? sampleItem.getExternalId() : "");

            // Get parent Sample accession number for context
            if (sampleItem.getSample() != null) {
                map.put("sampleAccessionNumber",
                        sampleItem.getSample().getAccessionNumber() != null
                                ? sampleItem.getSample().getAccessionNumber()
                                : "");
            } else {
                map.put("sampleAccessionNumber", "");
            }
            map.put("type",
                    sampleItem.getTypeOfSample() != null && sampleItem.getTypeOfSample().getDescription() != null
                            ? sampleItem.getTypeOfSample().getDescription()
                            : "");
            // Store actual status ID for filtering (OGC-150: supports all status types from
            // dropdown)
            // Frontend dropdown loads all status types and filters by ID
            // Default to "active" if no status ID (backward compatibility)
            map.put("status", sampleItem.getStatusId() != null ? sampleItem.getStatusId() : "active");

            // Check if this sample item has an assignment
            SampleStorageAssignment assignment = assignmentsBySampleItemId.get(sampleItem.getId());
            if (assignment != null && assignment.getLocationId() != null && assignment.getLocationType() != null) {
                // Build hierarchical path based on locationType
                String hierarchicalPath = buildHierarchicalPathForAssignment(assignment);

                map.put("location", hierarchicalPath != null ? hierarchicalPath : "");
                map.put("assignedBy", assignment.getAssignedByUserId());
                map.put("date", assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");
                // Include position coordinate and notes as separate fields for editing
                String posCoord = assignment.getPositionCoordinate() != null ? assignment.getPositionCoordinate() : "";
                String notesVal = assignment.getNotes() != null ? assignment.getNotes() : "";
                map.put("positionCoordinate", posCoord);
                map.put("notes", notesVal);

                // Debug: Log first 3 samples with assignments
                if (response.size() < 3) {
                    logger.info(
                            "DEBUG getAllSamplesWithAssignments - Sample #{}: ID={}, positionCoordinate='{}', notes='{}', mapKeys={}",
                            response.size() + 1, sampleItem.getId(), posCoord, notesVal, map.keySet());
                }
            } else {
                // No assignment - sample is unassigned
                map.put("location", "");
                map.put("assignedBy", null);
                map.put("date", "");
                map.put("positionCoordinate", "");
                map.put("notes", "");
            }

            response.add(map);
        }

        // Sort by location: assigned samples first (alphabetically by location), then
        // unassigned
        response.sort((a, b) -> {
            String locA = (String) a.get("location");
            String locB = (String) b.get("location");
            boolean aEmpty = locA == null || locA.isEmpty();
            boolean bEmpty = locB == null || locB.isEmpty();

            // Both empty - sort by sample ID
            if (aEmpty && bEmpty) {
                return String.valueOf(a.get("id")).compareTo(String.valueOf(b.get("id")));
            }
            // Empty locations go to the end
            if (aEmpty)
                return 1;
            if (bEmpty)
                return -1;
            // Both have locations - sort alphabetically
            return locA.compareTo(locB);
        });

        logger.info("getAllSamplesWithAssignments: Returning {} SampleItems (assigned and unassigned)",
                response.size());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSampleItemLocation(String sampleItemId) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            return new HashMap<>();
        }

        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);
        if (assignment == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sampleItemId", sampleItemId);

        String hierarchicalPath = buildHierarchicalPathForAssignment(assignment);
        result.put("location", hierarchicalPath != null ? hierarchicalPath : "");
        result.put("hierarchicalPath", hierarchicalPath != null ? hierarchicalPath : "");
        result.put("assignedBy", assignment.getAssignedByUserId());
        result.put("assignedDate", assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");
        result.put("positionCoordinate",
                assignment.getPositionCoordinate() != null ? assignment.getPositionCoordinate() : "");
        result.put("notes", assignment.getNotes() != null ? assignment.getNotes() : "");

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateAssignmentMetadata(String sampleItemId, String positionCoordinate, String notes) {
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            throw new LIMSRuntimeException("SampleItem ID is required");
        }
        SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);
        if (existingAssignment == null) {
            throw new LIMSRuntimeException("No storage assignment found for SampleItem: " + sampleItemId);
        }

        if (positionCoordinate != null) {
            if (positionCoordinate.trim().isEmpty()) {
                existingAssignment.setPositionCoordinate(null);
            } else {
                existingAssignment.setPositionCoordinate(positionCoordinate.trim());
            }
        }
        if (notes != null) {
            if (notes.trim().isEmpty()) {
                existingAssignment.setNotes(null);
            } else {
                existingAssignment.setNotes(notes.trim());
            }
        }

        sampleStorageAssignmentDAO.update(existingAssignment);

        Map<String, Object> response = new HashMap<>();
        response.put("assignmentId", existingAssignment.getId());
        response.put("sampleItemId", sampleItemId);
        response.put("positionCoordinate", existingAssignment.getPositionCoordinate());
        response.put("notes", existingAssignment.getNotes());
        response.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());

        String hierarchicalPath = buildHierarchicalPathForAssignment(existingAssignment);
        response.put("hierarchicalPath", hierarchicalPath);
        return response;
    }

    @Override
    @Transactional
    public Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes) {
        try {
            // Validate inputs
            if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
                throw new LIMSRuntimeException("SampleItem ID is required");
            }
            if (reason == null || reason.trim().isEmpty()) {
                throw new LIMSRuntimeException("Disposal reason is required");
            }
            if (method == null || method.trim().isEmpty()) {
                throw new LIMSRuntimeException("Disposal method is required");
            }

            // Resolve SampleItem (handles internal ID, accession number, or external ID)
            SampleItem sampleItem = resolveSampleItem(sampleItemId);

            // Check if already disposed
            if (statusService.matches(sampleItem.getStatusId(),
                    org.openelisglobal.common.services.StatusService.SampleStatus.Disposed)) {
                throw new LIMSRuntimeException("SampleItem is already disposed");
            }

            // Find existing assignment to get previous location
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                    .findBySampleItemId(sampleItem.getId());
            String previousLocation = null;
            Integer previousLocationId = null;
            String previousLocationType = null;
            String previousPositionCoordinate = null;

            if (existingAssignment != null) {
                previousLocationId = existingAssignment.getLocationId();
                previousLocationType = existingAssignment.getLocationType();
                previousPositionCoordinate = existingAssignment.getPositionCoordinate();

                // Build hierarchical path for audit log
                if (previousLocationId != null && previousLocationType != null) {
                    Object locationEntity = null;
                    switch (previousLocationType) {
                    case "box":
                        locationEntity = storageLocationService.get(previousLocationId, StorageBox.class);
                        break;
                    case "rack":
                        locationEntity = storageLocationService.get(previousLocationId, StorageRack.class);
                        break;
                    case "shelf":
                        locationEntity = storageLocationService.get(previousLocationId, StorageShelf.class);
                        break;
                    case "device":
                        locationEntity = storageLocationService.get(previousLocationId, StorageDevice.class);
                        break;
                    }
                    if (locationEntity != null) {
                        previousLocation = buildHierarchicalPathForEntity(locationEntity, previousLocationType,
                                previousPositionCoordinate);
                    }
                }

                // Clear the location fields (preserve assignment for audit trail)
                existingAssignment.setLocationId(null);
                existingAssignment.setLocationType(null);
                existingAssignment.setPositionCoordinate(null);
                sampleStorageAssignmentDAO.update(existingAssignment);
            }

            // Update SampleItem status to "SampleDisposed"
            String disposedStatusId = statusService
                    .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Disposed);
            sampleItem.setStatusId(disposedStatusId);
            sampleItemDAO.update(sampleItem);

            // Create audit movement record for disposal
            // Only create if there was a previous location (constraint requires at least
            // one location)
            Integer movementIdInt = null;
            if (previousLocationId != null && previousLocationType != null) {
                SampleStorageMovement movement = new SampleStorageMovement();
                movement.setSampleItem(sampleItem);
                movement.setPreviousLocationId(previousLocationId);
                movement.setPreviousLocationType(previousLocationType);
                movement.setPreviousPositionCoordinate(previousPositionCoordinate);
                // For disposal, new_location is NULL (no new location)
                movement.setNewLocationId(null);
                movement.setNewLocationType(null);
                movement.setNewPositionCoordinate(null);
                movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
                movement.setReason(
                        "Disposal: " + reason + " | Method: " + method + (notes != null ? " | Notes: " + notes : ""));
                movement.setMovedByUserId(1); // Default to system user

                movementIdInt = sampleStorageMovementDAO.insert(movement);
            }
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

            // Log successful disposal
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} disposed successfully. Movement ID: {}", sampleItem.getId(), movementId);
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("disposalId", movementId);
            response.put("sampleItemId", sampleItem.getId());
            response.put("status", "disposed");
            response.put("previousLocation", previousLocation);
            response.put("disposedDate", new Timestamp(System.currentTimeMillis()).toString());
            response.put("reason", reason);
            response.put("method", method);
            if (notes != null) {
                response.put("notes", notes);
            }

            return response;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Sample was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    /**
     * Build hierarchical path for an assignment based on its locationType.
     */
    private String buildHierarchicalPathForAssignment(SampleStorageAssignment assignment) {
        if (assignment == null || assignment.getLocationId() == null || assignment.getLocationType() == null) {
            return null;
        }

        String hierarchicalPath = null;
        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (assignment.getLocationType()) {
        case "device":
            device = (StorageDevice) storageLocationService.get(assignment.getLocationId(), StorageDevice.class);
            if (device != null) {
                room = device.getParentRoom();
                if (room != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        case "shelf":
            shelf = (StorageShelf) storageLocationService.get(assignment.getLocationId(), StorageShelf.class);
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
                if (room != null && device != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName() + " > " + shelf.getLabel();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        case "rack":
            rack = (StorageRack) storageLocationService.get(assignment.getLocationId(), StorageRack.class);
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
                if (room != null && device != null && shelf != null) {
                    hierarchicalPath = room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > "
                            + rack.getLabel();
                    if (assignment.getPositionCoordinate() != null
                            && !assignment.getPositionCoordinate().trim().isEmpty()) {
                        hierarchicalPath += " > " + assignment.getPositionCoordinate();
                    }
                }
            }
            break;
        }

        return hierarchicalPath;
    }

    /**
     * Build hierarchical path from already-initialized entities. This method
     * assumes all entities are already loaded (not proxies).
     */
    private String buildPathFromEntities(StorageBox box, StorageRack rack, StorageShelf shelf, StorageDevice device,
            StorageRoom room) {
        if (room != null && device != null && shelf != null) {
            return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                    + " > " + box.getLabel();
        } else if (device != null && shelf != null) {
            return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel() + " > " + box.getLabel();
        } else if (shelf != null) {
            return shelf.getLabel() + " > " + rack.getLabel() + " > " + box.getLabel();
        } else {
            return rack.getLabel() + " > " + box.getLabel();
        }
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> assignSampleItemWithLocation(String sampleItemId, String locationId,
            String locationType, String positionCoordinate, String notes) {
        try {
            // Validate inputs
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location ID is required");
            }
            if (locationType == null || locationType.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location type is required");
            }

            // Validate locationType is valid enum
            if (!locationType.equals("device") && !locationType.equals("shelf") && !locationType.equals("rack")
                    && !locationType.equals("box")) {
                throw new LIMSRuntimeException("Invalid location type: " + locationType
                        + ". Must be one of: 'device', 'shelf', 'rack', 'box'");
            }

            // Resolve SampleItem: accept either SampleItem ID or accession number
            SampleItem sampleItem = resolveSampleItem(sampleItemId);

            // Prevent duplicate assignments of the same SampleItem (must move first)
            SampleStorageAssignment existingAssignmentForSample = sampleStorageAssignmentDAO
                    .findBySampleItemId(sampleItem.getId());
            if (existingAssignmentForSample != null) {
                String existingType = existingAssignmentForSample.getLocationType();
                Integer existingLocId = existingAssignmentForSample.getLocationId();
                Object existingLocation = null;

                if (existingType != null && existingLocId != null) {
                    switch (existingType) {
                    case "box":
                        existingLocation = storageLocationService.get(existingLocId, StorageBox.class);
                        break;
                    case "rack":
                        existingLocation = storageLocationService.get(existingLocId, StorageRack.class);
                        break;
                    case "shelf":
                        existingLocation = storageLocationService.get(existingLocId, StorageShelf.class);
                        break;
                    case "device":
                        existingLocation = storageLocationService.get(existingLocId, StorageDevice.class);
                        break;
                    default:
                        break;
                    }
                }

                String existingPath = null;
                if (existingLocation != null && existingType != null) {
                    existingPath = buildHierarchicalPathForEntity(existingLocation, existingType,
                            existingAssignmentForSample.getPositionCoordinate());
                }

                StringBuilder msg = new StringBuilder("Sample is already assigned");
                if (existingPath != null && !existingPath.isEmpty()) {
                    msg.append(" to ").append(existingPath);
                } else if (existingType != null && existingLocId != null) {
                    msg.append(" to ").append(existingType).append(" ").append(existingLocId);
                }
                if (existingAssignmentForSample.getPositionCoordinate() != null
                        && !existingAssignmentForSample.getPositionCoordinate().trim().isEmpty()) {
                    msg.append(" at position ").append(existingAssignmentForSample.getPositionCoordinate().trim());
                }
                msg.append(". Please move the sample before assigning it again.");
                throw new LIMSRuntimeException(msg.toString());
            }

            // Load location entity based on locationType
            Integer locationIdInt = Integer.parseInt(locationId);
            Object locationEntity = null;
            StorageDevice device = null;
            StorageShelf shelf = null;
            StorageRack rack = null;
            StorageBox box = null;

            switch (locationType) {
            case "device":
                device = (StorageDevice) storageLocationService.get(locationIdInt, StorageDevice.class);
                if (device == null) {
                    throw new LIMSRuntimeException("Device not found: " + locationId);
                }
                locationEntity = device;
                break;
            case "shelf":
                shelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (shelf == null) {
                    throw new LIMSRuntimeException("Shelf not found: " + locationId);
                }
                locationEntity = shelf;
                break;
            case "rack":
                rack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (rack == null) {
                    throw new LIMSRuntimeException("Rack not found: " + locationId);
                }
                locationEntity = rack;
                break;
            case "box":
                box = (StorageBox) storageLocationService.get(locationIdInt, StorageBox.class);
                if (box == null) {
                    throw new LIMSRuntimeException("Box not found: " + locationId);
                }
                locationEntity = box;
                rack = box.getParentRack();
                break;
            }

            // Validate location has minimum 2 levels (room + device per FR-033a)
            if (device != null) {
                if (device.getParentRoom() == null) {
                    throw new LIMSRuntimeException("Device must have a parent room (minimum 2 levels: room + device)");
                }
            } else if (shelf != null) {
                device = shelf.getParentDevice();
                if (device == null || device.getParentRoom() == null) {
                    throw new LIMSRuntimeException(
                            "Shelf must have a parent device with a parent room (minimum 2 levels: room + device)");
                }
            } else if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf == null) {
                    throw new LIMSRuntimeException("Rack must have a parent shelf");
                }
                device = shelf.getParentDevice();
                if (device == null || device.getParentRoom() == null) {
                    throw new LIMSRuntimeException(
                            "Rack must have a parent shelf with a parent device and room (minimum 2 levels: room + device)");
                }
            }

            // Validate location is active (check entire hierarchy)
            if (!validateLocationActiveForEntity(locationEntity, locationType)) {
                throw new LIMSRuntimeException("Cannot assign to inactive location");
            }

            // Determine effective coordinate
            String effectiveCoordinate = positionCoordinate;
            if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                    && box != null && box.getLabel() != null) {
                effectiveCoordinate = box.getLabel();
            }

            // Validate coordinate is not already occupied (for box assignments with
            // coordinates)
            if ("box".equals(locationType) && effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                        .findByBoxAndCoordinate(locationIdInt, effectiveCoordinate.trim());
                if (existingAssignment != null) {
                    throw new LIMSRuntimeException(String.format(
                            "Position %s is already occupied by another sample. Please select a different position.",
                            effectiveCoordinate.trim()));
                }
            }

            // Log assignment details for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Assigning SampleItem {} to: locationId={}, locationType={}, positionCoordinate={}",
                        sampleItemId, locationIdInt, locationType, effectiveCoordinate);
            }

            // Create SampleStorageAssignment - always use locationId + locationType
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setSampleItem(sampleItem);
            assignment.setLocationId(locationIdInt);
            assignment.setLocationType(locationType);
            if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                assignment.setPositionCoordinate(effectiveCoordinate.trim());
            }
            assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
            assignment.setNotes(notes);
            assignment.setAssignedByUserId(1); // Default to system user for tests

            Integer assignmentIdInt = sampleStorageAssignmentDAO.insert(assignment);
            String assignmentId = assignmentIdInt != null ? assignmentIdInt.toString() : null;

            // Log successful assignment creation
            if (logger.isDebugEnabled()) {
                logger.debug("Created assignment for SampleItem {}: assignmentId={}, positionCoordinate={}",
                        sampleItemId, assignmentId, assignment.getPositionCoordinate());
            }

            // Build hierarchical path
            String hierarchicalPath = buildHierarchicalPathForEntity(locationEntity, locationType, effectiveCoordinate);

            // Check shelf capacity if applicable (informational warning only)
            String shelfCapacityWarning = null;
            if (locationType.equals("shelf") && shelf != null) {
                shelfCapacityWarning = checkShelfCapacity(shelf);
            } else if (locationType.equals("rack") && rack != null && rack.getParentShelf() != null) {
                shelfCapacityWarning = checkShelfCapacity(rack.getParentShelf());
            }

            // Create audit log entry with flexible assignment model
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSampleItem(sampleItem);

            // Initial assignment - no previous location
            movement.setPreviousLocationId(null);
            movement.setPreviousLocationType(null);
            movement.setPreviousPositionCoordinate(null);

            // Set new location (target location)
            movement.setNewLocationId(locationIdInt);
            movement.setNewLocationType(locationType);
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                movement.setNewPositionCoordinate(positionCoordinate.trim());
            } else {
                movement.setNewPositionCoordinate(null);
            }

            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(notes);
            movement.setMovedByUserId(1); // Default to system user for tests

            // Log movement audit record for debugging
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Creating movement audit for SampleItem {}: new locationId={}, locationType={}, positionCoordinate={}",
                        sampleItemId, locationIdInt, locationType, positionCoordinate);
            }

            sampleStorageMovementDAO.insert(movement);

            // Log successful assignment
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} assigned successfully. Assignment ID: {}", sampleItemId, assignmentId);
            }

            // Prepare response data
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("assignmentId", assignmentId);
            response.put("hierarchicalPath", hierarchicalPath != null ? hierarchicalPath : "Unknown");
            response.put("assignedDate", new Timestamp(System.currentTimeMillis()).toString());
            if (shelfCapacityWarning != null) {
                response.put("shelfCapacityWarning", shelfCapacityWarning);
            }

            return response;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Location was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    @Override
    @Transactional
    public String moveSampleItemWithLocation(String sampleItemId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes) {
        try {
            // Validate inputs
            if (locationId == null || locationId.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location ID is required");
            }
            if (locationType == null || locationType.trim().isEmpty()) {
                throw new LIMSRuntimeException("Location type is required");
            }

            // Validate locationType is valid enum
            if (!locationType.equals("device") && !locationType.equals("shelf") && !locationType.equals("rack")
                    && !locationType.equals("box")) {
                throw new LIMSRuntimeException("Invalid location type: " + locationType
                        + ". Must be one of: 'device', 'shelf', 'rack', 'box'");
            }

            // Resolve SampleItem: accept either accession number or external ID
            SampleItem sampleItem = resolveSampleItem(sampleItemId);
            // Get the actual numeric ID from the resolved SampleItem for database lookups
            String resolvedSampleItemId = sampleItem.getId();

            // Load target location entity based on locationType
            Integer locationIdInt = Integer.parseInt(locationId);
            Object targetLocationEntity = null;
            StorageDevice targetDevice = null;
            StorageShelf targetShelf = null;
            StorageRack targetRack = null;
            StorageBox targetBox = null;

            switch (locationType) {
            case "device":
                targetDevice = (StorageDevice) storageLocationService.get(locationIdInt, StorageDevice.class);
                if (targetDevice == null) {
                    throw new LIMSRuntimeException("Target device not found: " + locationId);
                }
                targetLocationEntity = targetDevice;
                break;
            case "shelf":
                targetShelf = (StorageShelf) storageLocationService.get(locationIdInt, StorageShelf.class);
                if (targetShelf == null) {
                    throw new LIMSRuntimeException("Target shelf not found: " + locationId);
                }
                targetLocationEntity = targetShelf;
                break;
            case "rack":
                targetRack = (StorageRack) storageLocationService.get(locationIdInt, StorageRack.class);
                if (targetRack == null) {
                    throw new LIMSRuntimeException("Target rack not found: " + locationId);
                }
                targetLocationEntity = targetRack;
                break;
            case "box":
                targetBox = (StorageBox) storageLocationService.get(locationIdInt, StorageBox.class);
                if (targetBox == null) {
                    throw new LIMSRuntimeException("Target box not found: " + locationId);
                }
                targetLocationEntity = targetBox;
                targetRack = targetBox.getParentRack();
                break;
            }

            // Validate target location has minimum 2 levels (room + device per FR-033a)
            if (targetDevice != null) {
                if (targetDevice.getParentRoom() == null) {
                    throw new LIMSRuntimeException(
                            "Target device must have a parent room (minimum 2 levels: room + device)");
                }
            } else if (targetShelf != null) {
                targetDevice = targetShelf.getParentDevice();
                if (targetDevice == null || targetDevice.getParentRoom() == null) {
                    throw new LIMSRuntimeException(
                            "Target shelf must have a parent device with a parent room (minimum 2 levels: room + device)");
                }
            } else if (targetRack != null) {
                targetShelf = targetRack.getParentShelf();
                if (targetShelf == null) {
                    throw new LIMSRuntimeException("Target rack must have a parent shelf");
                }
                targetDevice = targetShelf.getParentDevice();
                if (targetDevice == null || targetDevice.getParentRoom() == null) {
                    throw new LIMSRuntimeException(
                            "Target rack must have a parent shelf with a parent device and room (minimum 2 levels: room + device)");
                }
            }

            // Validate target location is active
            if (!validateLocationActiveForEntity(targetLocationEntity, locationType)) {
                throw new LIMSRuntimeException("Cannot move to inactive location");
            }
            // No occupancy tracking - position is just a text field

            // Find existing assignment for SampleItem (using resolved numeric ID)
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO
                    .findBySampleItemId(resolvedSampleItemId);

            // Store previous location details BEFORE updating (for movement audit log)
            Integer previousLocationId = null;
            String previousLocationType = null;
            String previousPositionCoordinate = null;

            if (existingAssignment != null) {
                // Store previous values before updating
                previousLocationId = existingAssignment.getLocationId();
                previousLocationType = existingAssignment.getLocationType();
                previousPositionCoordinate = existingAssignment.getPositionCoordinate();

                // Log previous state for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Moving SampleItem {} from: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, previousLocationId, previousLocationType, previousPositionCoordinate);
                }

                // Update existing assignment - always use locationId + locationType
                existingAssignment.setLocationId(locationIdInt);
                existingAssignment.setLocationType(locationType);
                String effectiveCoordinate = positionCoordinate;
                if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                        && targetBox != null && targetBox.getLabel() != null) {
                    effectiveCoordinate = targetBox.getLabel();
                }
                if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                    existingAssignment.setPositionCoordinate(effectiveCoordinate.trim());
                } else {
                    existingAssignment.setPositionCoordinate(null);
                }
                existingAssignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (reason != null) {
                    existingAssignment.setNotes(reason);
                }
                sampleStorageAssignmentDAO.update(existingAssignment);

                // Log new state for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Updated assignment for SampleItem {}: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, locationIdInt, locationType, existingAssignment.getPositionCoordinate());
                }
            } else {
                // Create new assignment (SampleItem was not previously assigned) - always use
                // locationId + locationType
                String effectiveCoordinate = positionCoordinate;
                if ("box".equals(locationType) && (effectiveCoordinate == null || effectiveCoordinate.trim().isEmpty())
                        && targetBox != null && targetBox.getLabel() != null) {
                    effectiveCoordinate = targetBox.getLabel();
                }
                SampleStorageAssignment assignment = new SampleStorageAssignment();
                assignment.setSampleItem(sampleItem);
                assignment.setLocationId(locationIdInt);
                assignment.setLocationType(locationType);
                if (effectiveCoordinate != null && !effectiveCoordinate.trim().isEmpty()) {
                    assignment.setPositionCoordinate(effectiveCoordinate.trim());
                }
                assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (reason != null) {
                    assignment.setNotes(reason);
                }
                assignment.setAssignedByUserId(1); // Default to system user for tests
                sampleStorageAssignmentDAO.insert(assignment);

                // Log initial assignment for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Initial assignment for SampleItem {}: locationId={}, locationType={}, positionCoordinate={}",
                            sampleItemId, locationIdInt, locationType, assignment.getPositionCoordinate());
                }
            }

            // Create audit log entry with flexible assignment model
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSampleItem(sampleItem);

            // Set previous location (from stored values, not from updated assignment)
            if (previousLocationId != null && previousLocationType != null) {
                movement.setPreviousLocationId(previousLocationId);
                movement.setPreviousLocationType(previousLocationType);
                movement.setPreviousPositionCoordinate(previousPositionCoordinate);

                // Log movement audit record for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Movement audit - previous: locationId={}, locationType={}, positionCoordinate={}",
                            previousLocationId, previousLocationType, previousPositionCoordinate);
                }
            } else {
                // Initial assignment - no previous location
                movement.setPreviousLocationId(null);
                movement.setPreviousLocationType(null);
                movement.setPreviousPositionCoordinate(null);

                // Log initial assignment audit record for debugging
                if (logger.isDebugEnabled()) {
                    logger.debug("Movement audit - initial assignment (no previous location)");
                }
            }

            // Set new location (target location)
            movement.setNewLocationId(locationIdInt);
            movement.setNewLocationType(locationType);
            String newPositionCoordinateValue = null;
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                newPositionCoordinateValue = positionCoordinate.trim();
                movement.setNewPositionCoordinate(newPositionCoordinateValue);
            } else {
                movement.setNewPositionCoordinate(null);
            }

            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(reason);
            movement.setMovedByUserId(1); // Default to system user for tests

            // Log new location for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Movement audit - new: locationId={}, locationType={}, positionCoordinate={}",
                        locationIdInt, locationType, newPositionCoordinateValue);
            }

            Integer movementIdInt = sampleStorageMovementDAO.insert(movement);
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

            // Log successful movement creation
            if (logger.isInfoEnabled()) {
                logger.info("SampleItem {} moved successfully. Movement ID: {}", sampleItemId, movementId);
            }

            return movementId;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Location was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    /**
     * Validate that a location entity is active (check entire hierarchy)
     */
    private boolean validateLocationActiveForEntity(Object locationEntity, String locationType) {
        if (locationEntity == null) {
            return false;
        }

        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (locationType) {
        case "device":
            device = (StorageDevice) locationEntity;
            room = device.getParentRoom();
            break;
        case "shelf":
            shelf = (StorageShelf) locationEntity;
            device = shelf.getParentDevice();
            if (device != null) {
                room = device.getParentRoom();
            }
            break;
        case "rack":
            rack = (StorageRack) locationEntity;
            shelf = rack.getParentShelf();
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
            }
            break;
        case "box":
            StorageBox box = (StorageBox) locationEntity;
            rack = box.getParentRack();
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
            }
            break;
        default:
            break;
        }

        // Validate minimum 2 levels (room + device)
        if (room == null || device == null) {
            return false;
        }

        // Check room and device are active
        if (room.getActive() == null || !room.getActive()) {
            return false;
        }
        if (device.getActive() == null || !device.getActive()) {
            return false;
        }

        // Check optional parents are active if they exist
        if (shelf != null && (shelf.getActive() == null || !shelf.getActive())) {
            return false;
        }
        if (rack != null && (rack.getActive() == null || !rack.getActive())) {
            return false;
        }

        return true;
    }

    /**
     * Build hierarchical path for a location entity (device, shelf, rack, or box)
     */
    private String buildHierarchicalPathForEntity(Object locationEntity, String locationType,
            String positionCoordinate) {
        if (locationEntity == null) {
            return "Unknown Location";
        }

        StorageRoom room = null;
        StorageDevice device = null;
        StorageShelf shelf = null;
        StorageRack rack = null;

        switch (locationType) {
        case "device":
            device = (StorageDevice) locationEntity;
            room = device.getParentRoom();
            if (room != null && device != null) {
                return room.getName() + " > " + device.getName()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null) {
                return device.getName() + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                        ? " > " + positionCoordinate
                        : "");
            }
            break;
        case "shelf":
            shelf = (StorageShelf) locationEntity;
            device = shelf.getParentDevice();
            if (device != null) {
                room = device.getParentRoom();
            }
            if (room != null && device != null && shelf != null) {
                return room.getName() + " > " + device.getName() + " > " + shelf.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null && shelf != null) {
                return device.getName() + " > " + shelf.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            }
            break;
        case "rack":
            rack = (StorageRack) locationEntity;
            shelf = rack.getParentShelf();
            if (shelf != null) {
                device = shelf.getParentDevice();
                if (device != null) {
                    room = device.getParentRoom();
                }
            }
            if (room != null && device != null && shelf != null && rack != null) {
                return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            } else if (device != null && shelf != null && rack != null) {
                return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                        + (positionCoordinate != null && !positionCoordinate.trim().isEmpty()
                                ? " > " + positionCoordinate
                                : "");
            }
            break;
        case "box":
            StorageBox box = (StorageBox) locationEntity;
            rack = box.getParentRack();
            if (rack != null) {
                shelf = rack.getParentShelf();
                if (shelf != null) {
                    device = shelf.getParentDevice();
                    if (device != null) {
                        room = device.getParentRoom();
                    }
                }
            }
            String coord = positionCoordinate != null && !positionCoordinate.trim().isEmpty() ? positionCoordinate
                    : box.getLabel();
            StringBuilder builder = new StringBuilder();
            if (room != null) {
                builder.append(room.getName());
            }
            if (device != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(device.getName());
            }
            if (shelf != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(shelf.getLabel());
            }
            if (rack != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(rack.getLabel());
            }
            if (coord != null) {
                if (builder.length() > 0) {
                    builder.append(" > ");
                }
                builder.append(coord);
            }
            return builder.length() > 0 ? builder.toString() : "Unknown Location";
        default:
            break;
        }

        return "Unknown Location";
    }

    /**
     * Check shelf capacity and return warning message if applicable (informational
     * only)
     */
    private String checkShelfCapacity(StorageShelf shelf) {
        if (shelf == null || shelf.getCapacityLimit() == null || shelf.getCapacityLimit() <= 0) {
            return null;
        }

        int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
        int capacityLimit = shelf.getCapacityLimit();
        int percentage = (occupied * 100) / capacityLimit;

        if (percentage >= 100) {
            return String.format(
                    "Shelf %s is at or over capacity (%d/%d positions, %d%%). Assignment allowed but shelf is over-occupied.",
                    shelf.getLabel(), occupied, capacityLimit, percentage);
        } else if (percentage >= 90) {
            return String.format("Shelf %s is near capacity (%d/%d positions, %d%%).", shelf.getLabel(), occupied,
                    capacityLimit, percentage);
        }

        return null;
    }

    /**
     * Resolve SampleItem from identifier (internal ID, accession number, or
     * external reference)
     * 
     * @param identifier Internal SampleItem ID, accession number, or external
     *                   reference
     * @return SampleItem entity
     * @throws LIMSRuntimeException if SampleItem not found or multiple SampleItems
     *                              match
     */
    private SampleItem resolveSampleItem(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new LIMSRuntimeException("Sample identifier is required");
        }

        String trimmedId = identifier.trim();

        // Step 0: Try numeric ID lookup (direct SampleItem by internal ID)
        // This handles cases where frontend sends the database ID
        // IMPORTANT: Only attempt this if the identifier is purely numeric, because
        // sampleItemService.get() is @Transactional and throws ObjectNotFoundException
        // when not found. If an exception is thrown inside a nested @Transactional
        // method,
        // it marks the outer transaction for rollback even if the exception is caught.
        if (trimmedId.matches("\\d+")) {
            try {
                SampleItem sampleItemById = sampleItemService.get(trimmedId);
                if (sampleItemById != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found SampleItem by numeric ID: {}", trimmedId);
                    }
                    return sampleItemById;
                }
            } catch (Exception e) {
                // Not found by numeric ID, continue to other lookup methods
                if (logger.isDebugEnabled()) {
                    logger.debug("SampleItem not found by numeric ID '{}', trying other methods", trimmedId);
                }
            }
        }

        // Step 1: Try accession number lookup (Sample → SampleItems)
        Sample sample = sampleService.getSampleByAccessionNumber(trimmedId);
        if (sample != null) {
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            if (sampleItems != null && !sampleItems.isEmpty()) {
                if (sampleItems.size() == 1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found SampleItem by accession number: {}", trimmedId);
                    }
                    return sampleItems.get(0);
                } else {
                    throw new LIMSRuntimeException(String.format(
                            "Sample with accession number '%s' has %d SampleItems. Please provide the external reference number to identify the specific specimen.",
                            trimmedId, sampleItems.size()));
                }
            }
        }

        // Step 2: Try external reference lookup (direct SampleItem lookup)
        List<SampleItem> sampleItemsByExtId = sampleItemService.getSampleItemsByExternalID(trimmedId);
        if (sampleItemsByExtId != null && !sampleItemsByExtId.isEmpty()) {
            if (sampleItemsByExtId.size() == 1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found SampleItem by external reference: {}", trimmedId);
                }
                return sampleItemsByExtId.get(0);
            } else {
                throw new LIMSRuntimeException(String.format(
                        "Multiple SampleItems found with external reference '%s'. This should not happen - external references should be unique.",
                        trimmedId));
            }
        }

        // Not found by any method
        throw new LIMSRuntimeException(String.format(
                "Sample not found with identifier '%s'. Please check the accession number or external reference number.",
                trimmedId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SampleStorageAssignment> getSampleAssignments(Pageable pageable) {
        return sampleStorageAssignmentDAO.findAll(pageable);
    }
}
