package org.openelisglobal.inventory.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryItemDAOImpl extends BaseDAOImpl<InventoryItem, String> implements InventoryItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(InventoryItemDAOImpl.class);

    public InventoryItemDAOImpl() {
        super(InventoryItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllInventoryItems() throws LIMSRuntimeException {
        return getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem readInventoryItem(String idString) throws LIMSRuntimeException {
        return get(idString).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> findAllActive() {
        try {
            String hql = "FROM InventoryItem i WHERE i.isActive = '1' ORDER BY i.name";
            Query<InventoryItem> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryItem.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active inventory items", e);
            throw new LIMSRuntimeException("Error finding active inventory items", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> findByItemType(String itemType) {
        try {
            String hql = "FROM InventoryItem i WHERE i.itemType = :itemType ORDER BY i.name";
            Query<InventoryItem> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryItem.class);
            query.setParameter("itemType", org.openelisglobal.inventory.valueholder.ItemType.valueOf(itemType));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding inventory items by type", e);
            throw new LIMSRuntimeException("Error finding inventory items by type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> findByCategory(String category) {
        try {
            String hql = "FROM InventoryItem i WHERE i.category = :category ORDER BY i.name";
            Query<InventoryItem> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryItem.class);
            query.setParameter("category", category);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding inventory items by category", e);
            throw new LIMSRuntimeException("Error finding inventory items by category", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> searchByName(String name) {
        try {
            String hql = "FROM InventoryItem i WHERE LOWER(i.name) LIKE LOWER(:name) ORDER BY i.name";
            Query<InventoryItem> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryItem.class);
            query.setParameter("name", "%" + name + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching inventory items by name", e);
            throw new LIMSRuntimeException("Error searching inventory items by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> findLowStockItems() {
        try {
            String hql = "SELECT DISTINCT i FROM InventoryItem i "
                    + "LEFT JOIN InventoryLot l ON l.inventoryItem.id = i.id AND l.status = 'ACTIVE' "
                    + "WHERE i.lowStockThreshold IS NOT NULL " + "GROUP BY i.id "
                    + "HAVING COALESCE(SUM(l.currentQuantity), 0) < i.lowStockThreshold";
            Query<InventoryItem> query = entityManager.unwrap(Session.class).createQuery(hql, InventoryItem.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding low stock items", e);
            throw new LIMSRuntimeException("Error finding low stock items", e);
        }
    }
}
