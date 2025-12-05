package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.LotStatus;

/**
 * DAO for InventoryLot entity Provides methods for FEFO (First Expired, First
 * Out) lot selection and tracking
 */
public interface InventoryLotDAO extends BaseDAO<InventoryLot, String> {

    /**
     * Find lots by inventory item ID
     *
     * @param inventoryItemId Inventory item ID
     * @return List of lots for the item
     */
    List<InventoryLot> findByInventoryItemId(String inventoryItemId);

    /**
     * Find available lots for an inventory item using FEFO (First Expired, First
     * Out) Returns lots that are ACTIVE, QC PASSED, have quantity > 0, ordered by
     * expiration date
     *
     * @param inventoryItemId Inventory item ID
     * @return List of available lots sorted by expiration date (earliest first)
     */
    List<InventoryLot> findAvailableLotsByItemFEFO(String inventoryItemId);

    /**
     * Find lot by barcode
     *
     * @param barcode Lot barcode
     * @return InventoryLot or null if not found
     */
    InventoryLot findByBarcode(String barcode);

    /**
     * Find lots expiring soon (within specified days)
     *
     * @param days Number of days from now
     * @return List of lots expiring within the specified timeframe
     */
    List<InventoryLot> findExpiringSoon(int days);

    /**
     * Find expired lots that are still marked as ACTIVE
     *
     * @return List of expired active lots
     */
    List<InventoryLot> findExpiredActiveLots();

    /**
     * Find lots by status
     *
     * @param status Lot status
     * @return List of lots with the given status
     */
    List<InventoryLot> findByStatus(LotStatus status);

    /**
     * Get total available quantity for an inventory item
     *
     * @param inventoryItemId Inventory item ID
     * @return Total quantity available across all active lots
     */
    Integer getTotalAvailableQuantity(String inventoryItemId);
}
