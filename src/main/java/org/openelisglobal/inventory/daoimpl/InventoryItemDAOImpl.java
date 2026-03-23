package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InventoryItemDAOImpl extends BaseDAOImpl<InventoryItem, Long> implements InventoryItemDAO {

    public InventoryItemDAOImpl() {
        super(InventoryItem.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemType> getAllItemTypes() {
        return java.util.Arrays.asList(ItemType.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllActive() throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.equal(root.get("isActive"), "Y")).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting all active inventory items", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByItemType(ItemType itemType) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.and(cb.equal(root.get("itemType"), itemType), cb.equal(root.get("isActive"), "Y")))
                    .orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory items by type", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getByCategory(String category) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.and(cb.equal(root.get("category"), category), cb.equal(root.get("isActive"), "Y")))
                    .orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory items by category", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> searchByName(String name) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.and(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"),
                    cb.equal(root.get("isActive"), "Y"))).orderBy(cb.asc(root.get("name")));

            return entityManager.createQuery(cq).getResultList();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error searching inventory items by name", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByFhirUuid(String fhirUuid) throws LIMSRuntimeException {
        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<InventoryItem> cq = cb.createQuery(InventoryItem.class);
            Root<InventoryItem> root = cq.from(InventoryItem.class);

            cq.select(root).where(cb.equal(root.get("fhirUuid"), java.util.UUID.fromString(fhirUuid)));

            List<InventoryItem> results = entityManager.createQuery(cq).setMaxResults(1).getResultList();

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting inventory item by FHIR UUID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() throws LIMSRuntimeException {
        try {
            // For this complex query with aggregations, we'll keep using native SQL
            // or use a more complex Criteria API with subqueries
            // For now, using a simpler approach with native query
            String sql = "SELECT DISTINCT i.* FROM clinlims.inventory_item i "
                    + "LEFT JOIN clinlims.inventory_lot l ON l.inventory_item_id = i.id "
                    + "WHERE i.is_active = 'Y' AND i.low_stock_threshold IS NOT NULL " + "GROUP BY i.id "
                    + "HAVING COALESCE(SUM(l.current_quantity), 0) < i.low_stock_threshold " + "ORDER BY i.name";

            @SuppressWarnings("unchecked")
            List<InventoryItem> results = entityManager.createNativeQuery(sql, InventoryItem.class).getResultList();

            return results;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting low stock items", e);
        }
    }
}
