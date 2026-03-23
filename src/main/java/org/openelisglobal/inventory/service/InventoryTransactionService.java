package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;

public interface InventoryTransactionService extends BaseObjectService<InventoryTransaction, Long> {

    /**
     * Get transactions by lot ID
     */
    List<InventoryTransaction> getByLotId(Long lotId);

    /**
     * Get transactions by transaction type
     */
    List<InventoryTransaction> getByTransactionType(TransactionType transactionType);

    /**
     * Get transactions within a date range
     */
    List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate);

    /**
     * Get transactions by reference (e.g., test result ID)
     */
    List<InventoryTransaction> getByReference(Long referenceId, String referenceType);

    /**
     * Record a transaction (helper method for creating transactions)
     *
     * @param lotId           The lot ID
     * @param transactionType The type of transaction
     * @param quantityChange  The quantity change (positive for additions, negative
     *                        for consumption)
     * @param quantityAfter   The quantity after the transaction
     * @param referenceId     Optional reference ID (e.g., test result ID)
     * @param referenceType   Optional reference type
     * @param notes           Optional notes
     * @param sysUserId       The user performing the action
     * @return The created transaction
     */
    InventoryTransaction recordTransaction(Long lotId, TransactionType transactionType, Double quantityChange,
            Double quantityAfter, Long referenceId, String referenceType, String notes, String sysUserId);
}
