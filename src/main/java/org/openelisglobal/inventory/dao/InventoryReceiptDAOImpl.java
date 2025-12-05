package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryReceiptDAOImpl extends BaseDAOImpl<InventoryReceipt, String> implements InventoryReceiptDAO {

    public InventoryReceiptDAOImpl() {
        super(InventoryReceipt.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(InventoryReceipt inventoryReceipt) {
        entityManager.refresh(inventoryReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptById(String id) {
        try {
            return entityManager.unwrap(Session.class).get(InventoryReceipt.class, id);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory receipt by ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReceipt> getAllInventoryReceipts() {
        try {
            String hql = "FROM InventoryReceipt ORDER BY receivedDate DESC";
            Query<InventoryReceipt> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryReceipt.class);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all inventory receipts", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptByInventoryItemId(String id) {
        try {
            String hql = "FROM InventoryReceipt r WHERE r.inventoryItemId = :itemId ORDER BY r.receivedDate DESC";
            Query<InventoryReceipt> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryReceipt.class);
            query.setParameter("itemId", id);
            query.setMaxResults(1);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory receipt by inventory item ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReceipt> findByInventoryItemId(String inventoryItemId) {
        try {
            String hql = "FROM InventoryReceipt r WHERE r.inventoryItemId = :itemId ORDER BY r.receivedDate DESC";
            Query<InventoryReceipt> query = entityManager.unwrap(Session.class).createQuery(hql,
                    InventoryReceipt.class);
            query.setParameter("itemId", inventoryItemId);
            return query.list();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error finding receipts by inventory item ID", e);
        }
    }
}
