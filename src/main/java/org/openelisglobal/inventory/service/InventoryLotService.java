package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.security.access.prepost.PreAuthorize;

public interface InventoryLotService extends BaseObjectService<InventoryLot, Long> {

    /**
     * Get available lots for an item sorted by FEFO (First Expired, First Out)
     * Returns lots that are: - ACTIVE or IN_USE status - QC PASSED - Have quantity
     * > 0 - Sorted by earliest expiration date first
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryLot> getAvailableLotsByItemFEFO(Long itemId);

    /**
     * Get lots by inventory item ID
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryLot> getByInventoryItemId(Long itemId);

    /**
     * Get lots by storage location ID
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryLot> getByStorageLocationId(Long locationId);

    /**
     * Get lots expiring within specified days
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryLot> getExpiringLots(int daysFromNow);

    /**
     * Get expired lots that are still marked as ACTIVE
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryLot> getExpiredActiveLots();

    /**
     * Get lot by lot number
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    InventoryLot getByLotNumber(String lotNumber);

    /**
     * Get lot by FHIR UUID
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    InventoryLot getByFhirUuid(String fhirUuid);

    /**
     * Get total current quantity for an item across all lots
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    Double getTotalCurrentQuantity(Long itemId);

    /**
     * Open a lot (marks as IN_USE and calculates expiry after opening for reagents)
     *
     * @param lotId      The lot ID
     * @param openedDate The date the lot was opened
     * @param sysUserId  The user performing the action
     * @return The updated lot with calculated expiry after opening
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
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
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    InventoryLot updateQCStatus(Long lotId, QCStatus qcStatus, String notes, String sysUserId);

    /**
     * Update lot status
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
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
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
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
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    InventoryLot disposeLot(Long lotId, String reason, String notes, String sysUserId);

    /**
     * Check if a lot is expired based on effective expiration date
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    boolean isLotExpired(Long lotId);

    /**
     * Check if a lot is available for use
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    boolean isLotAvailable(Long lotId);

    /**
     * Process automatic expiration updates Marks expired ACTIVE/IN_USE lots as
     * EXPIRED Returns count of lots updated
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    int processExpiredLots();
}
