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
    public String assignSample(String sampleId, String positionId, String notes) {
        try {
            // Validate inputs
            Sample sample = sampleDAO.get(sampleId)
                    .orElseThrow(() -> new LIMSRuntimeException("Sample not found: " + sampleId));

            Integer positionIdInt = Integer.parseInt(positionId);
            StoragePosition position = (StoragePosition) storageLocationService.get(positionIdInt,
                    StoragePosition.class);
            if (position == null) {
                throw new LIMSRuntimeException("Position not found: " + positionId);
            }

            // Validate position not occupied
            if (position.getOccupied() != null && position.getOccupied()) {
                // Generate position label for different hierarchy levels
                String positionLabel;
                if (position.getCoordinate() != null && !position.getCoordinate().isEmpty()) {
                    positionLabel = position.getCoordinate();
                } else if (position.getParentRack() != null) {
                    positionLabel = position.getParentRack().getLabel();
                } else if (position.getParentShelf() != null) {
                    positionLabel = position.getParentShelf().getLabel();
                } else {
                    positionLabel = position.getParentDevice().getName();
                }
                throw new LIMSRuntimeException("Position " + positionLabel + " is already occupied");
            }

            // Validate location is active
            if (!storageLocationService.validateLocationActive(position)) {
                throw new LIMSRuntimeException("Cannot assign to inactive location");
            }

            // Validate 2-level minimum rule (FR-033a): Position must have parent_device_id (minimum 2 levels: room + device)
            if (position.getParentDevice() == null) {
                throw new LIMSRuntimeException("Position must have a parent device (minimum 2 levels: room + device)");
            }
            StorageDevice device = position.getParentDevice();
            if (device.getParentRoom() == null) {
                throw new LIMSRuntimeException("Device must have a parent room");
            }
            // Validate hierarchy integrity: if rack exists, shelf must exist; if coordinate exists, rack must exist
            if (!position.validateHierarchyIntegrity()) {
                throw new LIMSRuntimeException("Position hierarchy integrity constraint violated");
            }
            // Validation passed: Position has Room and Device (minimum 2 levels)

            // Mark position as occupied
            position.setOccupied(true);
            storageLocationService.update(position);

            // Create assignment record
            SampleStorageAssignment assignment = new SampleStorageAssignment();
            assignment.setSample(sample);
            assignment.setStoragePosition(position);
            assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
            assignment.setNotes(notes);
            assignment.setAssignedByUserId(1); // Default to system user for tests

            Integer assignmentIdInt = sampleStorageAssignmentDAO.insert(assignment);
            String assignmentId = assignmentIdInt != null ? assignmentIdInt.toString() : null;

            // Create audit log entry
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSample(sample);
            movement.setPreviousPosition(null); // Initial assignment
            movement.setNewPosition(position);
            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(notes);
            movement.setMovedByUserId(1); // Default to system user for tests

            sampleStorageMovementDAO.insert(movement);

            return assignmentId;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Position was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> assignSampleWithDetails(String sampleId, String positionId, String notes) {
        // Assign the sample (this handles all the business logic)
        String assignmentId = assignSample(sampleId, positionId, notes);

        // Build hierarchical path within same transaction
        Integer positionIdInt = Integer.parseInt(positionId);
        StoragePosition position = (StoragePosition) storageLocationService.get(positionIdInt, StoragePosition.class);
        String hierarchicalPath = storageLocationService.buildHierarchicalPath(position);

        // Prepare response data
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("assignmentId", assignmentId);
        response.put("hierarchicalPath", hierarchicalPath != null ? hierarchicalPath : "Unknown");
        response.put("assignedDate", new java.sql.Timestamp(System.currentTimeMillis()).toString());

        return response;
    }

    @Override
    public String assignSampleWithCapacityCheck(String sampleId, String positionId, String notes) {
        // First do the assignment
        String assignmentId = assignSample(sampleId, positionId, notes);

        // Then check capacity and return warning if needed
        Integer positionIdInt = Integer.parseInt(positionId);
        StoragePosition position = (StoragePosition) storageLocationService.get(positionIdInt, StoragePosition.class);
        if (position != null && position.getParentRack() != null) {
            CapacityWarning warning = calculateCapacity(position.getParentRack());
            if (warning != null && warning.hasWarning()) {
                return warning.getWarningMessage();
            }
        }

        return null; // No warning
    }

    @Override
    public String moveSample(String sampleId, String targetPositionId, String reason) {
        try {
            // Validate inputs
            Sample sample = sampleDAO.get(sampleId)
                    .orElseThrow(() -> new LIMSRuntimeException("Sample not found: " + sampleId));

            Integer targetPositionIdInt = Integer.parseInt(targetPositionId);
            StoragePosition targetPosition = (StoragePosition) storageLocationService.get(targetPositionIdInt,
                    StoragePosition.class);
            if (targetPosition == null) {
                throw new LIMSRuntimeException("Target position not found: " + targetPositionId);
            }

            // Validate target position not occupied
            if (targetPosition.getOccupied() != null && targetPosition.getOccupied()) {
                // Generate position label for different hierarchy levels
                String positionLabel;
                if (targetPosition.getCoordinate() != null && !targetPosition.getCoordinate().isEmpty()) {
                    positionLabel = targetPosition.getCoordinate();
                } else if (targetPosition.getParentRack() != null) {
                    positionLabel = targetPosition.getParentRack().getLabel();
                } else if (targetPosition.getParentShelf() != null) {
                    positionLabel = targetPosition.getParentShelf().getLabel();
                } else {
                    positionLabel = targetPosition.getParentDevice().getName();
                }
                throw new LIMSRuntimeException("Target position " + positionLabel + " is already occupied");
            }

            // Validate target location is active
            if (!storageLocationService.validateLocationActive(targetPosition)) {
                throw new LIMSRuntimeException("Cannot move to inactive location");
            }

            // Validate 2-level minimum rule (FR-033a): Position must have parent_device_id (minimum 2 levels: room + device)
            if (targetPosition.getParentDevice() == null) {
                throw new LIMSRuntimeException("Target position must have a parent device (minimum 2 levels: room + device)");
            }
            StorageDevice device = targetPosition.getParentDevice();
            if (device.getParentRoom() == null) {
                throw new LIMSRuntimeException("Device must have a parent room");
            }
            // Validate hierarchy integrity of target position
            if (!targetPosition.validateHierarchyIntegrity()) {
                throw new LIMSRuntimeException("Target position hierarchy integrity constraint violated");
            }
            // Validation passed: Position has Room and Device (minimum 2 levels)

            // Find existing assignment
            SampleStorageAssignment existingAssignment = sampleStorageAssignmentDAO.findBySampleId(sampleId);
            StoragePosition previousPosition = null;

            if (existingAssignment != null) {
                previousPosition = existingAssignment.getStoragePosition();
                
                // Free the previous position
                if (previousPosition != null) {
                    previousPosition.setOccupied(false);
                    storageLocationService.update(previousPosition);
                }
            }

            // Mark target position as occupied
            targetPosition.setOccupied(true);
            storageLocationService.update(targetPosition);

            // Update or create assignment record
            if (existingAssignment != null) {
                // Update existing assignment
                existingAssignment.setStoragePosition(targetPosition);
                existingAssignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                existingAssignment.setNotes(reason);
                sampleStorageAssignmentDAO.update(existingAssignment);
            } else {
                // Create new assignment (sample was not previously assigned)
                SampleStorageAssignment assignment = new SampleStorageAssignment();
                assignment.setSample(sample);
                assignment.setStoragePosition(targetPosition);
                assignment.setAssignedDate(new Timestamp(System.currentTimeMillis()));
                assignment.setNotes(reason);
                assignment.setAssignedByUserId(1); // Default to system user for tests
                sampleStorageAssignmentDAO.insert(assignment);
            }

            // Create audit log entry
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSample(sample);
            movement.setPreviousPosition(previousPosition);
            movement.setNewPosition(targetPosition);
            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(reason);
            movement.setMovedByUserId(1); // Default to system user for tests

            Integer movementIdInt = sampleStorageMovementDAO.insert(movement);
            String movementId = movementIdInt != null ? movementIdInt.toString() : null;

            return movementId;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException("Position was just modified by another user. Please refresh and try again.",
                    e);
        }
    }

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

            // Skip assignments without storage positions (invalid state)
            if (assignment.getStoragePosition() == null) {
                logger.debug("Skipping assignment {} - null storage position", assignment.getId());
                continue;
            }

            // CRITICAL: Force initialization of all relationships by accessing them
            // within the transaction. This ensures proxies are fully loaded.
            StoragePosition position = assignment.getStoragePosition();
            StorageRack rack = position.getParentRack();
            if (rack == null) {
                logger.debug("Skipping assignment {} - position {} has null rack", assignment.getId(),
                        position.getId());
                continue; // Invalid position without rack
            }

            // Access properties to force initialization - do this within transaction
            StorageShelf shelf = rack.getParentShelf();
            StorageDevice device = shelf != null ? shelf.getParentDevice() : null;
            StorageRoom room = device != null ? device.getParentRoom() : null;

            // Note: shelf, device, and room can be null - buildPathFromEntities handles
            // this

            // Build hierarchical path directly from already-initialized entities
            // This avoids calling buildHierarchicalPath which might trigger lazy loading
            String hierarchicalPath = buildPathFromEntities(position, rack, shelf, device, room);

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
}
