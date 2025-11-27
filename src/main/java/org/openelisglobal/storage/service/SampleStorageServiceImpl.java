package org.openelisglobal.storage.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sampleitem.dao.SampleItemDAO;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.dao.*;
import org.openelisglobal.storage.valueholder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Autowired
    private StorageLocationService storageLocationService;

    @Override
    public CapacityWarning calculateCapacity(StorageRack rack) {
        int totalCapacity = rack.getCapacity();
        if (totalCapacity == 0) {
            return null; // No grid defined
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
        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.getAll();
        java.util.Map<String, SampleStorageAssignment> assignmentsBySampleItemId = new java.util.HashMap<>();
        for (SampleStorageAssignment assignment : assignments) {
            if (assignment.getSampleItem() != null && assignment.getSampleItem().getId() != null) {
                assignmentsBySampleItemId.put(assignment.getSampleItem().getId(), assignment);
            }
        }
        logger.info("getAllSamplesWithAssignments: Found {} total assignments", assignments.size());

        List<Map<String, Object>> response = new java.util.ArrayList<>();

        for (SampleItem sampleItem : allSampleItems) {
            if (sampleItem == null || sampleItem.getId() == null) {
                continue;
            }

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", sampleItem.getId());
            map.put("sampleItemId", sampleItem.getId());
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

        // DEBUG: Print first map keys and sample 10001 data
        if (!response.isEmpty()) {
            System.out.println("====== DEBUG getAllSamplesWithAssignments ======");
            System.out.println("First map ID: " + response.get(0).get("id"));
            System.out.println("First map positionCoordinate: '" + response.get(0).get("positionCoordinate") + "'");

            // Find and log sample 10001 specifically
            for (Map<String, Object> map : response) {
                if ("10001".equals(String.valueOf(map.get("id")))) {
                    System.out.println("--- Sample 10001 ---");
                    System.out.println("ID: " + map.get("id"));
                    System.out.println("location: " + map.get("location"));
                    System.out.println("positionCoordinate: '" + map.get("positionCoordinate") + "'");
                    System.out.println("notes: '" + map.get("notes") + "'");
                    break;
                }
            }
            System.out.println("Total samples: " + response.size());
            System.out.println("===============================================");
        }

        return response;
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
    private String buildPathFromEntities(StoragePosition position, StorageRack rack, StorageShelf shelf,
            StorageDevice device, StorageRoom room) {
        if (room != null && device != null && shelf != null) {
            return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel()
                    + " > Position " + position.getCoordinate();
        } else if (device != null && shelf != null) {
            return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel() + " > Position "
                    + position.getCoordinate();
        } else if (shelf != null) {
            return shelf.getLabel() + " > " + rack.getLabel() + " > Position " + position.getCoordinate();
        } else {
            return rack.getLabel() + " > Position " + position.getCoordinate();
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
            if (!locationType.equals("device") && !locationType.equals("shelf") && !locationType.equals("rack")) {
                throw new LIMSRuntimeException(
                        "Invalid location type: " + locationType + ". Must be one of: 'device', 'shelf', 'rack'");
            }

            // Validate SampleItem exists
            SampleItem sampleItem = sampleItemDAO.get(sampleItemId)
                    .orElseThrow(() -> new LIMSRuntimeException("SampleItem not found: " + sampleItemId));

            // Load location entity based on locationType
            Integer locationIdInt = Integer.parseInt(locationId);
            Object locationEntity = null;
            StorageDevice device = null;
            StorageShelf shelf = null;
            StorageRack rack = null;

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
            // No occupancy tracking - position is just a text field

            // Log assignment details for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Assigning SampleItem {} to: locationId={}, locationType={}, positionCoordinate={}",
                        sampleItemId, locationIdInt, locationType, positionCoordinate);
            }

            // Create SampleStorageAssignment - always use locationId + locationType
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setSampleItem(sampleItem);
            assignment.setLocationId(locationIdInt);
            assignment.setLocationType(locationType);
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                assignment.setPositionCoordinate(positionCoordinate.trim());
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
            String hierarchicalPath = buildHierarchicalPathForEntity(locationEntity, locationType, positionCoordinate);

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
            if (!locationType.equals("device") && !locationType.equals("shelf") && !locationType.equals("rack")) {
                throw new LIMSRuntimeException(
                        "Invalid location type: " + locationType + ". Must be one of: 'device', 'shelf', 'rack'");
            }

            // Validate SampleItem exists
            SampleItem sampleItem = sampleItemDAO.get(sampleItemId)
                    .orElseThrow(() -> new LIMSRuntimeException("SampleItem not found: " + sampleItemId));

            // Load target location entity based on locationType
            Integer locationIdInt = Integer.parseInt(locationId);
            Object targetLocationEntity = null;
            StorageDevice targetDevice = null;
            StorageShelf targetShelf = null;
            StorageRack targetRack = null;

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

            // Find existing assignment for SampleItem
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);

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
                if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                    existingAssignment.setPositionCoordinate(positionCoordinate.trim());
                } else {
                    existingAssignment.setPositionCoordinate(null);
                }
                existingAssignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (notes != null) {
                    existingAssignment.setNotes(notes);
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
                SampleStorageAssignment assignment = new SampleStorageAssignment();
                assignment.setSampleItem(sampleItem);
                assignment.setLocationId(locationIdInt);
                assignment.setLocationType(locationType);
                if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                    assignment.setPositionCoordinate(positionCoordinate.trim());
                }
                assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                if (notes != null) {
                    assignment.setNotes(notes);
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
        case "position":
            StoragePosition position = (StoragePosition) locationEntity;
            device = position.getParentDevice();
            if (device != null) {
                room = device.getParentRoom();
            }
            shelf = position.getParentShelf();
            rack = position.getParentRack();
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
     * Build hierarchical path for a location entity (device, shelf, rack, or
     * position)
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

    @Override
    @Transactional
    public java.util.Map<String, Object> updateAssignmentMetadata(String sampleItemId, String positionCoordinate,
            String notes) {
        // Validate SampleItem exists
        if (sampleItemId == null || sampleItemId.trim().isEmpty()) {
            throw new LIMSRuntimeException("SampleItem ID is required");
        }

        // Find existing assignment for SampleItem
        SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleItemId(sampleItemId);
        if (existingAssignment == null) {
            throw new LIMSRuntimeException("No storage assignment found for SampleItem: " + sampleItemId);
        }

        // Update position coordinate if provided (empty string clears it, null keeps
        // existing)
        if (positionCoordinate != null) {
            if (positionCoordinate.trim().isEmpty()) {
                existingAssignment.setPositionCoordinate(null);
            } else {
                existingAssignment.setPositionCoordinate(positionCoordinate.trim());
            }
        }

        // Update notes if provided (empty string clears it, null keeps existing)
        if (notes != null) {
            if (notes.trim().isEmpty()) {
                existingAssignment.setNotes(null);
            } else {
                existingAssignment.setNotes(notes.trim());
            }
        }

        sampleStorageAssignmentDAO.update(existingAssignment);

        // Build response with current assignment state
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("assignmentId", existingAssignment.getId());
        response.put("sampleItemId", sampleItemId);
        response.put("positionCoordinate", existingAssignment.getPositionCoordinate());
        response.put("notes", existingAssignment.getNotes());
        response.put("updatedDate", new Timestamp(System.currentTimeMillis()).toString());

        // Build hierarchical path for location
        String hierarchicalPath = buildHierarchicalPathForAssignment(existingAssignment);
        response.put("hierarchicalPath", hierarchicalPath);

        logger.info("Updated metadata for SampleItem {}: positionCoordinate={}, notes={}", sampleItemId,
                existingAssignment.getPositionCoordinate(), existingAssignment.getNotes() != null ? "updated" : "null");

        return response;
    }
}
