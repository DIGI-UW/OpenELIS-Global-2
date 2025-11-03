package org.openelisglobal.storage.service;

import org.openelisglobal.storage.valueholder.StorageRack;

/**
 * Service interface for sample storage assignment and movement operations
 */
public interface SampleStorageService {

    /**
     * Assign a sample to a storage position
     * 
     * @return assignment ID
     */
    String assignSample(String sampleId, String positionId, String notes);

    /**
     * Assign sample with capacity check
     * 
     * @return warning message if capacity threshold exceeded, null otherwise
     */
    String assignSampleWithCapacityCheck(String sampleId, String positionId, String notes);

    /**
     * Move sample to new position
     * 
     * @return movement ID
     */
    String moveSample(String sampleId, String targetPositionId, String reason);

    /**
     * Calculate rack capacity and return warning if threshold exceeded
     */
    CapacityWarning calculateCapacity(StorageRack rack);
}
