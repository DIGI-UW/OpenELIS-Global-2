package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;

public interface InventoryLotService extends BaseObjectService<InventoryLot, Long> {

    /**
     * Get available lots for an item sorted by FEFO (First Expired, First Out)
     * Returns lots that are: - ACTIVE or IN_USE status - QC PASSED - Have quantity
     * > 0 - Sorted by earliest expiration date first
     */
    List<InventoryLot> getAvailableLotsByItemFEFO(Long itemId);

    /**
     * Get lots by inventory item ID
     */
    List<InventoryLot> getByInventoryItemId(Long itemId);

    /**
     * Get lots expiring within specified days
     */
    List<InventoryLot> getExpiringLots(int daysFromNow);

    /**
     * Get expired lots that are still marked as ACTIVE
     */
    List<InventoryLot> getExpiredActiveLots();

    /**
     * Get lot by lot number
     */
    InventoryLot getByLotNumber(String lotNumber);

    /**
     * Get lot by FHIR UUID
     */
    InventoryLot getByFhirUuid(String fhirUuid);

    /**
     * Get total current quantity for an item across all lots
     */
    Double getTotalCurrentQuantity(Long itemId);

    /**
     * Open a lot (marks as IN_USE and calculates expiry after opening for reagents)
     *
     * @param lotId      The lot ID
     * @param openedDate The date the lot was opened
     * @param sysUserId  The user performing the action
     * @return The updated lot with calculated expiry after opening
     */
    InventoryLot openLot(Long lotId, Timestamp openedDate, String sysUserId);

    /**
     * Update lot QC status
     *
     * @param lotId     The lot ID
     * @param qcStatus  The new QC status
     * @param notes     Optional notes explaining the QC status change
     * @param sysUserId The user performing the action
     * @return The updated lot
     */
    InventoryLot updateQCStatus(Long lotId, QCStatus qcStatus, String notes, String sysUserId);

    /**
     * Update lot status
     */
    InventoryLot updateLotStatus(Long lotId, LotStatus status, String sysUserId);

    /**
     * Adjust lot quantity (manual adjustment with transaction recording)
     *
     * @param lotId       The lot ID
     * @param newQuantity The new quantity
     * @param reason      Reason for adjustment
     * @param sysUserId   The user performing the action
     * @return The updated lot
     */
    InventoryLot adjustLotQuantity(Long lotId, Double newQuantity, String reason, String sysUserId);

    /**
     * Dispose of a lot
     *
     * @param lotId     The lot ID
     * @param reason    Reason for disposal
     * @param notes     Additional notes about the disposal
     * @param sysUserId The user performing the action
     * @return The updated lot
     */
    InventoryLot disposeLot(Long lotId, String reason, String notes, String sysUserId);

    /**
     * Check if a lot is expired based on effective expiration date
     */
    boolean isLotExpired(Long lotId);

    /**
     * Check if a lot is available for use
     */
    boolean isLotAvailable(Long lotId);

    /**
     * Process automatic expiration updates Marks expired ACTIVE/IN_USE lots as
     * EXPIRED Returns count of lots updated
     */
    int processExpiredLots();

    /**
     * Update lot storage location using the unified storage hierarchy. Supports
     * assignment at any level: room, device, shelf, rack, box.
     *
     * @param lotId              The lot ID
     * @param locationId         The location ID (storage_room.id,
     *                           storage_device.id, etc.)
     * @param locationType       The type of location ('room', 'device', 'shelf',
     *                           'rack', 'box')
     * @param positionCoordinate Optional position within the location (e.g., well
     *                           coordinate)
     * @param storagePath        The hierarchical path string (e.g., "Room A >
     *                           Freezer 1 > Shelf 2")
     * @param sysUserId          The user performing the action
     * @return The updated lot
     */
    InventoryLot updateStorageLocation(Long lotId, Integer locationId, String locationType, String positionCoordinate,
            String storagePath, String sysUserId);

    /**
     * Get lots by unified storage location (room, device, shelf, rack, or box)
     *
     * @param locationId   The location ID
     * @param locationType The location type
     * @return List of lots at the specified location
     */
    List<InventoryLot> getByUnifiedLocation(Integer locationId, String locationType);

    /**
     * Get paginated lots with filtering and sorting
     *
     * @param limit      Maximum number of results to return
     * @param offset     Number of results to skip
     * @param sortBy     Field to sort by (e.g., "expirationDate", "lotNumber")
     * @param sortOrder  Sort direction ("asc" or "desc")
     * @param itemType   Filter by inventory item type (optional)
     * @param status     Filter by lot status (optional)
     * @param searchTerm Search term for lot number or item name (optional)
     * @return List of paginated lots
     */
    List<InventoryLot> getPagedLots(int limit, int offset, String sortBy, String sortOrder, String itemType,
            LotStatus status, String searchTerm);

    /**
     * Get total count of lots matching the same filters as getPagedLots
     */
    Long getPagedLotsCount(String itemType, LotStatus status, String searchTerm);
}
