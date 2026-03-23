package org.openelisglobal.inventory.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;

public interface InventoryTransactionDAO extends BaseDAO<InventoryTransaction, Long> {

    /**
     * Get transactions by lot ID, ordered by date descending
     */
    List<InventoryTransaction> getByLotId(Long lotId) throws LIMSRuntimeException;

    /**
     * Get transactions by transaction type
     */
    List<InventoryTransaction> getByTransactionType(TransactionType transactionType) throws LIMSRuntimeException;

    /**
     * Get transactions within a date range
     */
    List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate) throws LIMSRuntimeException;

    /**
     * Get transactions by reference (e.g., test result ID)
     */
    List<InventoryTransaction> getByReference(Long referenceId, String referenceType) throws LIMSRuntimeException;
}
