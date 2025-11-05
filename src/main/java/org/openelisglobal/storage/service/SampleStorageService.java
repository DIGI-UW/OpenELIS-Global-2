package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
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
     * Assign a sample to a storage position and return complete assignment data
     * including hierarchical path. All relationships are resolved within service
     * transaction.
     * 
     * @return Map containing assignmentId, hierarchicalPath, and assignedDate
     */
    java.util.Map<String, Object> assignSampleWithDetails(String sampleId, String positionId, String notes);

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

    /**
     * Get all samples with storage assignments and complete hierarchical paths. All
     * relationships are eagerly fetched within the service transaction.
     * 
     * @return List of maps, each containing: id, sampleId, type, status, location,
     *         assignedBy, date
     */
    List<Map<String, Object>> getAllSamplesWithAssignments();
}
