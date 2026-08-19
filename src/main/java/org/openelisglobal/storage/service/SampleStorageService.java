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

    /**
     * Dispose a SampleItem. The audit emission for the global audit trail rides on
     * {@code SampleItemService.update} (AuditableBaseObject path), so the caller
     * MUST pass the acting user's sysUserId (numeric String). The same id is
     * stamped on the storage-movement row for the per-sample audit modal.
     *
     * <p>
     * OGC-738: previously the disposal hardcoded {@code movedByUserId=1} and called
     * {@code sampleItemDAO.update} directly, bypassing audit emit.
     */
    java.util.Map<String, Object> disposeSampleItem(String sampleItemId, String reason, String method, String notes,
            String sysUserId);

    /**
     * Record usage against a SampleItem's remaining quantity (OGC-1026, Results
     * Entry v3 R7). Partial use decrements {@code remainingQuantity} (never below
     * zero); {@code markUsedUp} zeroes it — "exhausted" is remaining == 0, not a
     * status, and disposal stays an explicit follow-up step. The update rides
     * {@code SampleItemService.update} so the global audit row reflects the acting
     * user.
     *
     * @param sampleItemId flexible identifier (internal id, accession number, or
     *                     external id)
     * @param amountUsed   amount consumed; required unless markUsedUp
     * @param markUsedUp   true to zero the remaining quantity outright
     * @param sysUserId    acting user's numeric id (required for audit)
     * @return quantity snapshot: sampleItemId, quantity, remainingQuantity,
     *         exhausted
     */
    java.util.Map<String, Object> recordSampleUsage(String sampleItemId, java.math.BigDecimal amountUsed,
            boolean markUsedUp, String sysUserId);

    /**
     * List storage movements for a SampleItem with the acting user's display name
     * resolved. Returns one Map per movement with the same shape the audit modal
     * already renders, plus a {@code movedByUserName} field.
     *
     * <p>
     * OGC-738a: the controller used to return the raw numeric user id; the View
     * Audit modal showed "Moved By: 42" with no way to identify who.
     */
    java.util.List<java.util.Map<String, Object>> getSampleItemMovementsWithUserNames(String sampleItemId);

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

    /**
     * Assign an InventoryLot to a location using the same polymorphic
     * locationId+locationType model and audit trail as sample assignments
     * (OGC-657).
     *
     * @param inventoryLotId     InventoryLot ID
     * @param locationId         Location ID (room, device, shelf, rack, or box ID)
     * @param locationType       Location type: 'room', 'device', 'shelf', 'rack',
     *                           or 'box'
     * @param positionCoordinate Optional text-based coordinate
     * @param notes              Optional assignment notes
     * @param sysUserId          The user performing the assignment
     * @return Map containing assignmentId, hierarchicalPath, assignedDate, and
     *         shelfCapacityWarning if applicable
     */
    java.util.Map<String, Object> assignInventoryLotWithLocation(String inventoryLotId, String locationId,
            String locationType, String positionCoordinate, String notes, String sysUserId);

    /**
     * Move an InventoryLot to a new location (OGC-657).
     *
     * @param inventoryLotId     InventoryLot ID
     * @param locationId         Target location ID
     * @param locationType       Target location type
     * @param positionCoordinate Optional text-based coordinate
     * @param reason             Optional reason for movement
     * @param notes              Optional notes
     * @param sysUserId          The user performing the move
     * @return Movement ID
     */
    String moveInventoryLotWithLocation(String inventoryLotId, String locationId, String locationType,
            String positionCoordinate, String reason, String notes, String sysUserId);

    /**
     * Get storage location for a specific InventoryLot (OGC-657).
     *
     * @param inventoryLotId InventoryLot ID
     * @return Map with location details including hierarchicalPath, or empty map if
     *         not assigned
     */
    java.util.Map<String, Object> getInventoryLotLocation(String inventoryLotId);

    /**
     * List storage movements for an InventoryLot with the acting user's display
     * name resolved (OGC-657).
     */
    java.util.List<java.util.Map<String, Object>> getInventoryLotMovementsWithUserNames(String inventoryLotId);

    /**
     * Bulk lookup of current locations for many InventoryLots, keyed by lot id (as
     * String), to avoid N+1 queries when rendering the Inventory dashboard
     * (OGC-657). Only lots that have an assignment appear in the result.
     */
    java.util.Map<String, java.util.Map<String, Object>> getLocationsForInventoryLots(
            java.util.List<Long> inventoryLotIds);
}
