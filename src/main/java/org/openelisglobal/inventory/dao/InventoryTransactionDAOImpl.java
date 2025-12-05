package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryTransactionDAOImpl extends BaseDAOImpl<InventoryTransaction, String>
        implements InventoryTransactionDAO {

    public InventoryTransactionDAOImpl() {
        super(InventoryTransaction.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> findByLotId(String lotId) {
        try {
            String hql = "FROM InventoryTransaction t WHERE t.lot.id = :lotId ORDER BY t.transactionDate DESC";
            Query<InventoryTransaction> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryTransaction.class);
            query.setParameter("lotId", lotId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding transactions by lot ID", e);
        }
    }
}
