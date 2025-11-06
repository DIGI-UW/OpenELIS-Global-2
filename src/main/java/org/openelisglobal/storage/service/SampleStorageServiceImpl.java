package org.openelisglobal.storage.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.hibernate.StaleObjectStateException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.valueholder.Sample;
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
    private SampleDAO sampleDAO;

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
        // DAO.getAll() now eagerly fetches entire hierarchy (Sample, Position, Rack,
        // Shelf, Device, Room)
        // All data is loaded within this transaction, so no lazy loading issues
        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.getAll();
        logger.info("getAllSamplesWithAssignments: Found {} total assignments", assignments.size());
        List<Map<String, Object>> response = new java.util.ArrayList<>();

        for (SampleStorageAssignment assignment : assignments) {
            // Skip assignments without samples (data integrity issue)
            if (assignment.getSample() == null) {
                logger.debug("Skipping assignment {} - null sample", assignment.getId());
                continue;
            }

            // Skip assignments without location (invalid state)
            if (assignment.getLocationId() == null || assignment.getLocationType() == null) {
                logger.debug("Skipping assignment {} - null location", assignment.getId());
                continue;
            }

            // Build hierarchical path based on locationType
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
                    if (room != null && device != null) {
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
                    if (room != null && device != null && shelf != null) {
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
                    if (room != null && device != null && shelf != null && rack != null) {
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

            if (hierarchicalPath == null) {
                logger.debug("Skipping assignment {} - could not build hierarchical path", assignment.getId());
                continue;
            }

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", assignment.getSample().getId());
            map.put("sampleId", assignment.getSample().getId());
            map.put("type",
                    assignment.getSample().getAccessionNumber() != null ? assignment.getSample().getAccessionNumber()
                            : "");
            map.put("status",
                    assignment.getSample().getStatus() != null ? assignment.getSample().getStatus() : "active");
            map.put("location", hierarchicalPath);
            map.put("assignedBy", assignment.getAssignedByUserId());
            map.put("date", assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");

            response.add(map);
        }

        logger.info("getAllSamplesWithAssignments: Returning {} samples after processing {} assignments",
                response.size(), assignments.size());
        return response;
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
    public java.util.Map<String, Object> assignSampleWithLocation(String sampleId, String locationId,
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

            // Validate sample exists
            Sample sample = sampleDAO.get(sampleId)
                    .orElseThrow(() -> new LIMSRuntimeException("Sample not found: " + sampleId));

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

            // Create SampleStorageAssignment - always use locationId + locationType
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setSample(sample);
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
            movement.setSample(sample);

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

            sampleStorageMovementDAO.insert(movement);

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
    public String moveSampleWithLocation(String sampleId, String locationId, String locationType,
            String positionCoordinate, String reason) {
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

            // Validate sample exists
            Sample sample = sampleDAO.get(sampleId)
                    .orElseThrow(() -> new LIMSRuntimeException("Sample not found: " + sampleId));

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

            // Find existing assignment for sample
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleId(sampleId);

            if (existingAssignment != null) {
                // Update existing assignment - always use locationId + locationType
                existingAssignment.setLocationId(locationIdInt);
                existingAssignment.setLocationType(locationType);
                if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                    existingAssignment.setPositionCoordinate(positionCoordinate.trim());
                } else {
                    existingAssignment.setPositionCoordinate(null);
                }
                existingAssignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                existingAssignment.setNotes(reason);
                sampleStorageAssignmentDAO.update(existingAssignment);
            } else {
                // Create new assignment (sample was not previously assigned) - always use
                // locationId + locationType
                SampleStorageAssignment assignment = new SampleStorageAssignment();
                assignment.setSample(sample);
                assignment.setLocationId(locationIdInt);
                assignment.setLocationType(locationType);
                if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                    assignment.setPositionCoordinate(positionCoordinate.trim());
                }
                assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                assignment.setNotes(reason);
                assignment.setAssignedByUserId(1); // Default to system user for tests
                sampleStorageAssignmentDAO.insert(assignment);
            }

            // Create audit log entry with flexible assignment model
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSample(sample);

            // Set previous location (from existing assignment if exists)
            if (existingAssignment != null) {
                movement.setPreviousLocationId(existingAssignment.getLocationId());
                movement.setPreviousLocationType(existingAssignment.getLocationType());
                movement.setPreviousPositionCoordinate(existingAssignment.getPositionCoordinate());
            } else {
                // Initial assignment - no previous location
                movement.setPreviousLocationId(null);
                movement.setPreviousLocationType(null);
                movement.setPreviousPositionCoordinate(null);
            }

            // Set new location (target location)
            movement.setNewLocationId(locationIdInt);
            movement.setNewLocationType(locationType);
            if (positionCoordinate != null && !positionCoordinate.trim().isEmpty()) {
                movement.setNewPositionCoordinate(positionCoordinate.trim());
            } else {
                movement.setNewPositionCoordinate(null);
            }

            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(reason);
            movement.setMovedByUserId(1); // Default to system user for tests

            Integer movementIdInt = sampleStorageMovementDAO.insert(movement);
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

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
}
