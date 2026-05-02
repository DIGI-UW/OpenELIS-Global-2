package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.Query;
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
            // Use native SQL to avoid type conversion issues with enum
            String sql = "SELECT * FROM clinlims.inventory_item " + "WHERE item_type = :itemType AND is_active = 'Y' "
                    + "ORDER BY name";

            @SuppressWarnings("unchecked")
            List<InventoryItem> results = entityManager.createNativeQuery(sql, InventoryItem.class)
                    .setParameter("itemType", itemType.name()).getResultList();

            return results;
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

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getPagedItems(int limit, int offset, String sortBy, String sortOrder, ItemType itemType,
            Boolean isActive, String searchTerm) throws LIMSRuntimeException {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM clinlims.inventory_item WHERE 1=1");

            if (itemType != null) {
                sql.append(" AND item_type = :itemType");
            }
            if (isActive != null) {
                sql.append(" AND is_active = :isActive");
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql.append(" AND LOWER(name) LIKE :searchTerm");
            }

            String validatedSortBy = validateAndMapItemSortField(sortBy);
            sql.append(" ORDER BY ").append(mapItemSortFieldToColumn(validatedSortBy));
            sql.append(" ").append("desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC");

            Query query = entityManager.createNativeQuery(sql.toString(), InventoryItem.class);
            if (itemType != null) {
                query.setParameter("itemType", itemType.name());
            }
            if (isActive != null) {
                query.setParameter("isActive", isActive ? "Y" : "N");
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                query.setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%");
            }

            @SuppressWarnings("unchecked")
            List<InventoryItem> results = query.setFirstResult(offset).setMaxResults(limit).getResultList();

            return results;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting paged inventory items", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm) throws LIMSRuntimeException {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM clinlims.inventory_item WHERE 1=1");

            if (itemType != null) {
                sql.append(" AND item_type = :itemType");
            }
            if (isActive != null) {
                sql.append(" AND is_active = :isActive");
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                sql.append(" AND LOWER(name) LIKE :searchTerm");
            }

            Query query = entityManager.createNativeQuery(sql.toString());
            if (itemType != null) {
                query.setParameter("itemType", itemType.name());
            }
            if (isActive != null) {
                query.setParameter("isActive", isActive ? "Y" : "N");
            }
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                query.setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%");
            }

            Number result = (Number) query.getSingleResult();

            return result.longValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting paged inventory items count", e);
        }
    }

    /**
     * Validates and maps sort field names to prevent injection and ensure valid
     * fields
     */
    private String validateAndMapItemSortField(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return "name";
        }

        switch (sortBy.toLowerCase()) {
        case "name":
            return "name";
        case "itemtype":
        case "item_type":
            return "itemType";
        case "category":
            return "category";
        case "manufacturer":
            return "manufacturer";
        case "catalognumber":
        case "catalog_number":
            return "catalogNumber";
        case "lowstockthreshold":
        case "low_stock_threshold":
            return "lowStockThreshold";
        case "isactive":
        case "is_active":
        case "active":
            return "isActive";
        // Equipment-specific fields
        case "equipmentcondition":
        case "equipment_condition":
            return "equipmentCondition";
        case "modelnumber":
        case "model_number":
            return "modelNumber";
        case "serialnumber":
        case "serial_number":
            return "serialNumber";
        case "ahritag":
        case "ahri_tag":
            return "ahriTag";
        case "installationdate":
        case "installation_date":
            return "installationDate";
        case "lastservicedate":
        case "last_service_date":
            return "lastServiceDate";
        case "lastmaintenancedate":
        case "last_maintenance_date":
            return "lastMaintenanceDate";
        case "nextmaintenancedate":
        case "next_maintenance_date":
            return "nextMaintenanceDate";
        case "currentlocation":
        case "current_location":
            return "currentLocation";
        // Reagent-specific fields
        case "concentration":
            return "concentration";
        default:
            // Default to name for safety
            return "name";
        }
    }

    private String mapItemSortFieldToColumn(String sortField) {
        switch (sortField) {
        case "itemType":
            return "item_type";
        case "catalogNumber":
            return "catalog_number";
        case "lowStockThreshold":
            return "low_stock_threshold";
        case "isActive":
            return "is_active";
        case "equipmentCondition":
            return "equipment_condition";
        case "modelNumber":
            return "model_number";
        case "serialNumber":
            return "serial_number";
        case "ahriTag":
            return "ahri_tag";
        case "installationDate":
            return "installation_date";
        case "lastServiceDate":
            return "last_service_date";
        case "lastMaintenanceDate":
            return "last_maintenance_date";
        case "currentLocation":
            return "current_location";
        default:
            return sortField;
        }
    }
}
