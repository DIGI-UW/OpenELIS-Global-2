package org.openelisglobal.storage.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StoragePosition;

public interface SampleStorageAssignmentDAO extends BaseDAO<SampleStorageAssignment, Integer> {
    SampleStorageAssignment findBySampleItemId(String sampleItemId);

    /**
     * Find assignment by storage position (for barcode validation)
     *
     * @param position Storage position entity
     * @return SampleStorageAssignment or null if position is not occupied
     */
    SampleStorageAssignment findByStoragePosition(StoragePosition position);

    /**
     * Check if a StoragePosition is occupied by checking for matching
     * SampleStorageAssignment. This replaces the StoragePosition.occupied flag
     * which is no longer maintained.
     * 
     * @param position StoragePosition to check
     * @return true if there's a SampleStorageAssignment matching this position,
     *         false otherwise
     */
    boolean isPositionOccupied(StoragePosition position);
}
