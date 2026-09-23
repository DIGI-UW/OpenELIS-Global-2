package org.openelisglobal.inventory.daoimpl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryTransactionDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryTransactionDAOImpl extends BaseDAOImpl<InventoryTransaction, Long>
        implements InventoryTransactionDAO {

    public InventoryTransactionDAOImpl() {
        super(InventoryTransaction.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByLotId(Long lotId) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.lot.id = :lotId ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting transactions by lot ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByTransactionType(TransactionType transactionType)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.transactionType = :transactionType ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("transactionType", transactionType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting transactions by type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByTypeAndDateRange(TransactionType transactionType, Timestamp startDate,
            Timestamp endDate) throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.transactionType = :transactionType"
                    + " AND t.transactionDate >= :startDate AND t.transactionDate < :endDate"
                    + " ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("transactionType", transactionType);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting transactions by type and date range", e);
        }
    }

    /**
     * Native, because the query is "the newest row per lot" and DISTINCT ON does
     * that in one pass. The HQL alternative is a correlated subquery per lot, and
     * the result is bounded either way: one row per lot, not one per transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> getQuantityOnHandAsOf(Timestamp asOf) throws LIMSRuntimeException {
        try {
            String sql = "SELECT DISTINCT ON (t.lot_id) t.lot_id, t.quantity_after"
                    + " FROM clinlims.inventory_transaction t WHERE t.transaction_date < :asOf"
                    + " ORDER BY t.lot_id, t.transaction_date DESC, t.id DESC";
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.unwrap(Session.class).createNativeQuery(sql).setParameter("asOf", asOf)
                    .getResultList();
            Map<Long, Double> quantityByLotId = new HashMap<>();
            for (Object[] row : rows) {
                if (row[0] == null || row[1] == null) {
                    continue;
                }
                quantityByLotId.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
            }
            return quantityByLotId;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting quantity on hand as of a date", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.transactionDate >= :startDate"
                    + " AND t.transactionDate < :endDate ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting transactions by date range", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getByReference(Long referenceId, String referenceType)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.referenceId = :referenceId AND t.referenceType = :referenceType ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("referenceId", referenceId);
            query.setParameter("referenceType", referenceType);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting transactions by reference", e);
        }
    }
}
