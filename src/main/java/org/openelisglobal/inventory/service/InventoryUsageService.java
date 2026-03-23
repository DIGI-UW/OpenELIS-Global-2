package org.openelisglobal.inventory.service;

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
}
