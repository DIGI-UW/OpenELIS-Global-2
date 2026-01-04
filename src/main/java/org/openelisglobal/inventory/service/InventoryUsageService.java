package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageService extends BaseObjectService<InventoryUsage, Long> {

    /**
     * Get usage records by test result ID (for Lot Traceability Report)
     */
    List<InventoryUsage> getByTestResultId(Long testResultId);

    /**
     * Get usage records by lot ID
     */
    List<InventoryUsage> getByLotId(Long lotId);

    /**
     * Get usage records by inventory item ID
     */
    List<InventoryUsage> getByInventoryItemId(Long itemId);

    /**
     * Get usage records by analysis ID
     */
    List<InventoryUsage> getByAnalysisId(Long analysisId);

    /**
     * Record inventory usage for a test result
     *
     * @param lotId        The lot ID used
     * @param itemId       The inventory item ID
     * @param quantityUsed The quantity consumed
     * @param testResultId The test result ID
     * @param analysisId   The analysis ID
     * @param sysUserId    The user performing the action
     * @return The created usage record
     */
    InventoryUsage recordUsage(Long lotId, Long itemId, Double quantityUsed, Long testResultId, Long analysisId,
            String sysUserId);

    /**
     * Record inventory usage for a test result with optional quantity deduction.
     * Use this when the quantity has already been deducted elsewhere (e.g., by
     * consumeInventoryFEFO).
     *
     * @param lotId          The lot ID used
     * @param itemId         The inventory item ID
     * @param quantityUsed   The quantity consumed
     * @param testResultId   The test result ID
     * @param analysisId     The analysis ID
     * @param sysUserId      The user performing the action
     * @param deductQuantity If true, deduct quantity from lot; if false, only
     *                       record usage
     * @return The created usage record
     */
    InventoryUsage recordUsage(Long lotId, Long itemId, Double quantityUsed, Long testResultId, Long analysisId,
            String sysUserId, boolean deductQuantity);

    /**
     * Record equipment usage without reducing inventory quantities. Tracks usage
     * for traceability but does NOT deduct from available stock.
     *
     * @param lotId        The lot ID used
     * @param itemId       The inventory item ID
     * @param quantityUsed The quantity used
     * @param sysUserId    The user performing the action
     * @param labUnitId    Deprecated, pass null
     * @return The created usage record
     */
    InventoryUsage recordEquipmentUsage(Long lotId, Long itemId, Double quantityUsed, String sysUserId,
            String labUnitId);

    /**
     * Get usage records within a date range
     *
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return List of usage records within the date range
     */
    List<InventoryUsage> getByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Get usage records for a specific item within a date range
     *
     * @param itemId    The inventory item ID
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return List of usage records for the item within the date range
     */
    List<InventoryUsage> getByItemIdAndDateRange(Long itemId, Timestamp startDate, Timestamp endDate);
}
