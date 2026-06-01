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

    java.util.Map<String, Object> assignSampleItemWithLocation(String sampleItemId, String locationId,
            String locationType, String positionCoordinate, String notes, String sysUserId);

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

    String moveSampleItemWithLocation(String sampleItemId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes, String sysUserId);

    /**
     * Mark a sample item as missing by clearing its current assignment location while
     * retaining the assignment row for audit/traceability.
     *
     * @param sampleItemId SampleItem ID
     * @param reason       Required reason for missing state
     * @param notes        Optional notes
     * @return Map containing movementId, status, and updatedLocation details
     */
    java.util.Map<String, Object> markSampleItemMissing(String sampleItemId, String reason, String notes);

    /**
     * Remove a stored sample from its physical location when checked out for retrieval
     * (clears assignment location; records movement audit).
     */
    java.util.Map<String, Object> clearStorageAssignmentForCheckout(String sampleItemId, String notes,
            String sysUserId);

    java.util.Map<String, Object> updateAssignmentMetadata(String sampleItemId, String positionCoordinate,
            String notes);

    java.util.Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes);

    java.util.Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes,
            String sysUserId);

    /**
     * Get storage location for a specific SampleItem
     *
     * @param sampleItemId SampleItem ID
     * @return Map with location details including hierarchicalPath, or empty map if
     *         not assigned
     */
    java.util.Map<String, Object> getSampleItemLocation(String sampleItemId);

    /**
     * Resolve a SampleItem by accession number, external id, or internal id (same rules
     * as storage assignment).
     *
     * @param identifier accession, external id, or sample item id
     * @return resolved SampleItem or null when not found
     */
    org.openelisglobal.sampleitem.valueholder.SampleItem resolveSampleItemByIdentifier(String identifier);

    /**
     * Find an assigned stored SampleItem using the same substring rules as the storage
     * dashboard search (accession, external id, sample item id, location path).
     *
     * @param identifier partial or full accession / external id / id
     * @return first matching assigned SampleItem, or null
     */
    org.openelisglobal.sampleitem.valueholder.SampleItem findAssignedSampleItemByPartialIdentifier(
            String identifier);

    /**
     * Get storage locations for multiple SampleItems in a single call.
         *
         * @param sampleItemIds sample item IDs
         * @return map keyed by SampleItem ID with location details
         */
        java.util.Map<String, java.util.Map<String, Object>> getSampleItemLocations(java.util.List<String> sampleItemIds);

    /**
     * Get paginated sample storage assignments for dashboard display (OGC-150).
     * 
     * @param pageable Pagination parameters (page number, page size, sorting)
     * @return Page of SampleStorageAssignment entities
     */
    org.springframework.data.domain.Page<org.openelisglobal.storage.valueholder.SampleStorageAssignment> getSampleAssignments(
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get a SampleStorageAssignment by ID.
     *
     * @param assignmentId the assignment ID
     * @return the SampleStorageAssignment or null if not found
     */
    org.openelisglobal.storage.valueholder.SampleStorageAssignment getSampleStorageAssignment(Integer assignmentId);

    /**
     * Get all storage assignments for a SampleItem.
     *
     * @param sampleItem the sample item
     * @return list of storage assignments for this sample
     */
    java.util.List<org.openelisglobal.storage.valueholder.SampleStorageAssignment> getSampleStorageAssignmentsBySampleItem(
            org.openelisglobal.sampleitem.valueholder.SampleItem sampleItem);

    /**
     * Get all storage movements for a SampleItem.
     *
     * @param sampleItem the sample item
     * @return list of storage movements for this sample
     */
    java.util.List<org.openelisglobal.storage.valueholder.SampleStorageMovement> getSampleStorageMovementsBySampleItem(
            org.openelisglobal.sampleitem.valueholder.SampleItem sampleItem);
}
