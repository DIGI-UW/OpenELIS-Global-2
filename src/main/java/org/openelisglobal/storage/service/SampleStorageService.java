package org.openelisglobal.storage.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.storage.valueholder.StorageRack;

/**
 * Service interface for sample storage assignment and movement operations
 */
public interface SampleStorageService {

    /**
     * Calculate rack capacity and return warning if threshold exceeded
     */
    CapacityWarning calculateCapacity(StorageRack rack);

    /**
     * Get all SampleItems with storage assignments and complete hierarchical paths.
     * All relationships are eagerly fetched within the service transaction.
     * 
     * @return List of maps, each containing: id, sampleItemId,
     *         sampleAccessionNumber, type, status, location, assignedBy, date
     */
    List<Map<String, Object>> getAllSamplesWithAssignments();

    /**
     * Assign a SampleItem to a location using simplified polymorphic relationship
     * (locationId + locationType). Supports assignment to device, shelf, or rack
     * level with optional text-based position coordinate.
     * 
     * @param sampleItemId       SampleItem ID
     * @param locationId         Location ID (device, shelf, or rack ID)
     * @param locationType       Location type: 'device', 'shelf', or 'rack'
     * @param positionCoordinate Optional text-based coordinate (max 50 chars) - can
     *                           be set for any location_type
     * @param notes              Optional assignment notes
     * @return Map containing assignmentId, hierarchicalPath, assignedDate, and
     *         shelfCapacityWarning if applicable
     */
    java.util.Map<String, Object> assignSampleItemWithLocation(String sampleItemId, String locationId,
            String locationType, String positionCoordinate, String notes);

    /**
     * Move a SampleItem to a new location using simplified polymorphic relationship
     * (locationId + locationType). Supports movement to device, shelf, or rack
     * level with optional text-based position coordinate.
     *
     * @param sampleItemId       SampleItem ID
     * @param locationId         Target location ID (device, shelf, rack, or box ID)
     * @param locationType       Target location type: 'device', 'shelf', 'rack', or
     *                           'box'
     * @param positionCoordinate Optional text-based coordinate (max 50 chars)
     * @param reason             Optional reason for movement
     * @return Movement ID
     */
    String moveSampleItemWithLocation(String sampleItemId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes);

    java.util.Map<String, Object> updateAssignmentMetadata(String sampleItemId, String positionCoordinate,
            String notes);

    java.util.Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes);

    /**
     * Get storage location for a specific SampleItem
     * 
     * @param sampleItemId SampleItem ID
     * @return Map with location details including hierarchicalPath, or empty map if
     *         not assigned
     */
    java.util.Map<String, Object> getSampleItemLocation(String sampleItemId);

    /**
     * Get paginated sample storage assignments for dashboard display (OGC-150).
     * 
     * @param pageable Pagination parameters (page number, page size, sorting)
     * @return Page of SampleStorageAssignment entities
     */
    org.springframework.data.domain.Page<org.openelisglobal.storage.valueholder.SampleStorageAssignment> getSampleAssignments(
            org.springframework.data.domain.Pageable pageable);
}
