package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageDAO extends BaseDAO<InventoryUsage, Long> {

    /**
     * Get usage records by test result ID (for Lot Traceability Report)
     */
    List<InventoryUsage> getByTestResultId(Long testResultId) throws LIMSRuntimeException;

    /**
     * Get usage records by lot ID
     */
    List<InventoryUsage> getByLotId(Long lotId) throws LIMSRuntimeException;

    /**
     * Get usage records by inventory item ID
     */
    List<InventoryUsage> getByInventoryItemId(Long itemId) throws LIMSRuntimeException;

    /**
     * Get usage records by analysis ID
     */
    List<InventoryUsage> getByAnalysisId(Long analysisId) throws LIMSRuntimeException;
}
