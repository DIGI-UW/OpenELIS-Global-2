package org.openelisglobal.inventory.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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
     * Get transactions within a date range. The range is half-open — on or after
     * {@code startDate}, strictly before {@code endDate} — which is the same rule
     * {@link org.openelisglobal.inventory.dao.InventoryUsageDAO#getByDateRange}
     * uses, so a caller cannot get one answer from usage and another from
     * transactions for the same posted dates.
     */
    List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate) throws LIMSRuntimeException;

    /**
     * Get transactions of one type within a half-open date range. Separate from
     * {@link #getByDateRange} because a period report wants receipts alone, and
     * filtering the whole log in memory loads every consumption to discard it.
     */
    List<InventoryTransaction> getByTypeAndDateRange(TransactionType transactionType, Timestamp startDate,
            Timestamp endDate) throws LIMSRuntimeException;

    /**
     * The running balance each lot stood at immediately before {@code asOf}, keyed
     * by lot id. This is how stock at a past date is answered: the lot table only
     * holds today's quantity, while {@code quantity_after} records what each write
     * left behind. Lots with no transaction before {@code asOf} are absent rather
     * than zero, so a caller can tell "no stock" from "not yet known".
     */
    Map<Long, Double> getQuantityOnHandAsOf(Timestamp asOf) throws LIMSRuntimeException;

    /**
     * Get transactions by reference (e.g., test result ID)
     */
    List<InventoryTransaction> getByReference(Long referenceId, String referenceType) throws LIMSRuntimeException;
}
