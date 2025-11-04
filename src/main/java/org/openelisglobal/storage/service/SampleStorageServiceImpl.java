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
                throw new LIMSRuntimeException("Position " + position.getCoordinate() + " is already occupied");
            }

            // Validate location is active
            if (!storageLocationService.validateLocationActive(position)) {
                throw new LIMSRuntimeException("Cannot assign to inactive location");
            }

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
        // TODO: Implement in T083-T088 (Phase 5 - US2B)
        throw new UnsupportedOperationException("Move sample not implemented yet - deferred to Phase 5");
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
        // DAO.getAll() now eagerly fetches entire hierarchy (Sample, Position, Rack, Shelf, Device, Room)
        // All data is loaded within this transaction, so no lazy loading issues
        List<SampleStorageAssignment> assignments = sampleStorageAssignmentDAO.getAll();
        List<Map<String, Object>> response = new java.util.ArrayList<>();

        for (SampleStorageAssignment assignment : assignments) {
            // Skip assignments without samples (data integrity issue)
            if (assignment.getSample() == null) {
                continue;
            }

            // Skip assignments without storage positions (invalid state)
            if (assignment.getStoragePosition() == null) {
                continue;
            }

            // CRITICAL: Force initialization of all relationships by accessing them
            // within the transaction. This ensures proxies are fully loaded.
            StoragePosition position = assignment.getStoragePosition();
            StorageRack rack = position.getParentRack();
            if (rack == null) {
                continue; // Invalid position without rack
            }
            
            // Access properties to force initialization - do this within transaction
            StorageShelf shelf = rack.getParentShelf();
            StorageDevice device = shelf != null ? shelf.getParentDevice() : null;
            StorageRoom room = device != null ? device.getParentRoom() : null;

            // Build hierarchical path directly from already-initialized entities
            // This avoids calling buildHierarchicalPath which might trigger lazy loading
            String hierarchicalPath = buildPathFromEntities(position, rack, shelf, device, room);

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", assignment.getSample().getId());
            map.put("sampleId", assignment.getSample().getId());
            map.put("type",
                    assignment.getSample().getAccessionNumber() != null
                            ? assignment.getSample().getAccessionNumber()
                            : "");
            map.put("status",
                    assignment.getSample().getStatus() != null ? assignment.getSample().getStatus()
                            : "active");
            map.put("location", hierarchicalPath);
            map.put("assignedBy", assignment.getAssignedByUserId());
            map.put("date",
                    assignment.getAssignedDate() != null ? assignment.getAssignedDate().toString() : "");

            response.add(map);
        }

        return response;
    }

    /**
     * Build hierarchical path from already-initialized entities.
     * This method assumes all entities are already loaded (not proxies).
     */
    private String buildPathFromEntities(StoragePosition position, StorageRack rack, 
                                         StorageShelf shelf, StorageDevice device, StorageRoom room) {
        if (room != null && device != null && shelf != null) {
            return room.getName() + " > " + device.getName() + " > " + shelf.getLabel() + " > " 
                   + rack.getLabel() + " > Position " + position.getCoordinate();
        } else if (device != null && shelf != null) {
            return device.getName() + " > " + shelf.getLabel() + " > " + rack.getLabel() 
                   + " > Position " + position.getCoordinate();
        } else if (shelf != null) {
            return shelf.getLabel() + " > " + rack.getLabel() + " > Position " + position.getCoordinate();
        } else {
            return rack.getLabel() + " > Position " + position.getCoordinate();
        }
    }
}
