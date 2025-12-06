package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryUsage;

public interface InventoryUsageDAO extends BaseDAO<InventoryUsage, String> {

    /**
     * Get usage records by test result ID (for Lot Traceability Report)
     */
    List<InventoryUsage> getByTestResultId(String testResultId) throws LIMSRuntimeException;

    /**
     * Get usage records by lot ID
     */
    List<InventoryUsage> getByLotId(String lotId) throws LIMSRuntimeException;

    /**
     * Get usage records by inventory item ID
     */
    List<InventoryUsage> getByInventoryItemId(String itemId) throws LIMSRuntimeException;

    /**
     * Get usage records by analysis ID
     */
    List<InventoryUsage> getByAnalysisId(String analysisId) throws LIMSRuntimeException;
}
