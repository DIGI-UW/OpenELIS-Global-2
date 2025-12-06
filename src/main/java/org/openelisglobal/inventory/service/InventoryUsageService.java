package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageService extends BaseObjectService<InventoryUsage, String> {

    /**
     * Get usage records by test result ID (for Lot Traceability Report)
     */
    List<InventoryUsage> getByTestResultId(String testResultId);

    /**
     * Get usage records by lot ID
     */
    List<InventoryUsage> getByLotId(String lotId);

    /**
     * Get usage records by inventory item ID
     */
    List<InventoryUsage> getByInventoryItemId(String itemId);

    /**
     * Get usage records by analysis ID
     */
    List<InventoryUsage> getByAnalysisId(String analysisId);

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
    InventoryUsage recordUsage(String lotId, String itemId, Double quantityUsed, String testResultId, String analysisId,
            String sysUserId);
}
