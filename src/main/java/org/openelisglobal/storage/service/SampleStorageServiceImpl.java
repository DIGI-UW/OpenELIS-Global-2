package org.openelisglobal.storage.service;

import java.sql.Timestamp;
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
 * Implementation of SampleStorageService - Handles sample assignment and movement
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
            Sample sample = sampleDAO.get(sampleId).orElseThrow(
                () -> new LIMSRuntimeException("Sample not found: " + sampleId));
            
            StoragePosition position = (StoragePosition) storageLocationService.get(positionId, StoragePosition.class);
            if (position == null) {
                throw new LIMSRuntimeException("Position not found: " + positionId);
            }

            // Validate position not occupied
            if (position.getOccupied() != null && position.getOccupied()) {
                throw new LIMSRuntimeException("Position " + position.getCoordinate() + 
                    " is already occupied");
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
            // assignedByUser would be set from security context in real implementation

            String assignmentId = sampleStorageAssignmentDAO.insert(assignment);

            // Create audit log entry
            SampleStorageMovement movement = new SampleStorageMovement();
            movement.setSample(sample);
            movement.setPreviousPosition(null); // Initial assignment
            movement.setNewPosition(position);
            movement.setMovementDate(new Timestamp(System.currentTimeMillis()));
            movement.setReason(notes);
            // movedByUser would be set from security context

            sampleStorageMovementDAO.insert(movement);

            return assignmentId;

        } catch (StaleObjectStateException e) {
            throw new LIMSRuntimeException(
                "Position was just modified by another user. Please refresh and try again.", e);
        }
    }

    @Override
    public String assignSampleWithCapacityCheck(String sampleId, String positionId, String notes) {
        // First do the assignment
        String assignmentId = assignSample(sampleId, positionId, notes);

        // Then check capacity and return warning if needed
        StoragePosition position = (StoragePosition) storageLocationService.get(positionId, StoragePosition.class);
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
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", 
                rack.getLabel(), percentage);
        } else if (percentage >= 90) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", 
                rack.getLabel(), percentage);
        } else if (percentage >= 80) {
            warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.", 
                rack.getLabel(), percentage);
        }

        return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
    }
}

