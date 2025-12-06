package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;

public interface InventoryLotService extends BaseObjectService<InventoryLot, String> {

    /**
     * Get available lots for an item sorted by FEFO (First Expired, First Out)
     * Returns lots that are: - ACTIVE or IN_USE status - QC PASSED - Have quantity
     * > 0 - Sorted by earliest expiration date first
     */
    List<InventoryLot> getAvailableLotsByItemFEFO(String itemId);

    /**
     * Get lots by inventory item ID
     */
    List<InventoryLot> getByInventoryItemId(String itemId);

    /**
     * Get lots by storage location ID
     */
    List<InventoryLot> getByStorageLocationId(String locationId);

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
    Double getTotalCurrentQuantity(String itemId);

    /**
     * Open a lot (marks as IN_USE and calculates expiry after opening for reagents)
     * 
     * @param lotId      The lot ID
     * @param openedDate The date the lot was opened
     * @param sysUserId  The user performing the action
     * @return The updated lot with calculated expiry after opening
     */
    InventoryLot openLot(String lotId, Timestamp openedDate, String sysUserId);

    /**
     * Update lot QC status
     */
    InventoryLot updateQCStatus(String lotId, QCStatus qcStatus, String sysUserId);

    /**
     * Update lot status
     */
    InventoryLot updateLotStatus(String lotId, LotStatus status, String sysUserId);

    /**
     * Adjust lot quantity (manual adjustment with transaction recording)
     * 
     * @param lotId       The lot ID
     * @param newQuantity The new quantity
     * @param reason      Reason for adjustment
     * @param sysUserId   The user performing the action
     * @return The updated lot
     */
    InventoryLot adjustLotQuantity(String lotId, Double newQuantity, String reason, String sysUserId);

    /**
     * Dispose of a lot
     * 
     * @param lotId     The lot ID
     * @param reason    Reason for disposal
     * @param sysUserId The user performing the action
     * @return The updated lot
     */
    InventoryLot disposeLot(String lotId, String reason, String sysUserId);

    /**
     * Check if a lot is expired based on effective expiration date
     */
    boolean isLotExpired(String lotId);

    /**
     * Check if a lot is available for use
     */
    boolean isLotAvailable(String lotId);

    /**
     * Process automatic expiration updates Marks expired ACTIVE/IN_USE lots as
     * EXPIRED Returns count of lots updated
     */
    int processExpiredLots();
}
