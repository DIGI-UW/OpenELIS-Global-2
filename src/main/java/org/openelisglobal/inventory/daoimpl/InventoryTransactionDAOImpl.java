package org.openelisglobal.inventory.daoimpl;

import java.sql.Timestamp;
import java.util.List;
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
    public List<InventoryTransaction> getByDateRange(Timestamp startDate, Timestamp endDate)
            throws LIMSRuntimeException {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC";
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
