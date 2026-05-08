package org.openelisglobal.inventory.daoimpl;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        return getPagedItems(limit, offset, sortBy, sortOrder, itemType, isActive, searchTerm, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getPagedItems(int limit, int offset, String sortBy, String sortOrder, ItemType itemType,
            Boolean isActive, String searchTerm, Set<Integer> departmentScopeIds) throws LIMSRuntimeException {
        try {
            if (departmentScopeIds != null && departmentScopeIds.isEmpty()) {
                return Collections.emptyList();
            }
            StringBuilder sql = new StringBuilder("SELECT i.* FROM clinlims.inventory_item i WHERE 1=1");
            appendInventoryPagedFilters(sql, itemType, isActive, searchTerm, departmentScopeIds);

            String validatedSortBy = validateAndMapItemSortField(sortBy);
            sql.append(" ORDER BY i.").append(mapItemSortFieldToColumn(validatedSortBy));
            sql.append(" ").append("desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC");

            Query query = entityManager.createNativeQuery(sql.toString(), InventoryItem.class);
            bindInventoryPagedParameters(query, itemType, isActive, searchTerm, departmentScopeIds);

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
        return getPagedItemsCount(itemType, isActive, searchTerm, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm,
            Set<Integer> departmentScopeIds) throws LIMSRuntimeException {
        try {
            if (departmentScopeIds != null && departmentScopeIds.isEmpty()) {
                return 0L;
            }
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM clinlims.inventory_item i WHERE 1=1");
            appendInventoryPagedFilters(sql, itemType, isActive, searchTerm, departmentScopeIds);

            Query query = entityManager.createNativeQuery(sql.toString());
            bindInventoryPagedParameters(query, itemType, isActive, searchTerm, departmentScopeIds);

            Number result = (Number) query.getSingleResult();

            return result.longValue();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error getting paged inventory items count", e);
        }
    }

    private void appendInventoryPagedFilters(StringBuilder sql, ItemType itemType, Boolean isActive, String searchTerm,
            Set<Integer> departmentScopeIds) {
        if (itemType != null) {
            sql.append(" AND i.item_type = :itemType");
        }
        if (isActive != null) {
            sql.append(" AND i.is_active = :isActive");
        }
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            sql.append(" AND LOWER(i.name) LIKE :searchTerm");
        }
        if (departmentScopeIds != null && !departmentScopeIds.isEmpty()) {
            appendInventoryDepartmentScopeOrNotebookProject(sql);
        }
    }

    /**
     * Mirrors department isolation for inventory: owned row via
     * {@code department_test_section_id}, or legacy rows linked through
     * {@code project_name} to {@code notebook} / {@code notebook_departments} (same
     * idea as
     * {@link org.openelisglobal.department.service.DepartmentIsolationService#resolveInventoryDepartmentKeys}).
     */
    private void appendInventoryDepartmentScopeOrNotebookProject(StringBuilder sql) {
        sql.append(" AND (i.department_test_section_id IN (:deptIds)");
        sql.append(" OR (i.department_test_section_id IS NULL AND i.project_name IS NOT NULL");
        sql.append(" AND TRIM(i.project_name) <> '' AND EXISTS (");
        sql.append("SELECT 1 FROM clinlims.notebook nb INNER JOIN clinlims.notebook_departments nd");
        sql.append(" ON nd.notebook_id = nb.id WHERE nd.test_section_id IN (:deptIds) AND (");
        sql.append("(TRIM(i.project_name) ~ '^[0-9]+$' AND nb.id = CAST(TRIM(i.project_name) AS INTEGER)) OR ");
        sql.append("(TRIM(i.project_name) !~ '^[0-9]+$' AND LOWER(TRIM(nb.title)) = LOWER(TRIM(i.project_name)))");
        sql.append("))))");
    }

    private void bindInventoryPagedParameters(Query query, ItemType itemType, Boolean isActive, String searchTerm,
            Set<Integer> departmentScopeIds) {
        if (itemType != null) {
            query.setParameter("itemType", itemType.name());
        }
        if (isActive != null) {
            query.setParameter("isActive", isActive ? "Y" : "N");
        }
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query.setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%");
        }
        if (departmentScopeIds != null && !departmentScopeIds.isEmpty()) {
            query.setParameter("deptIds", departmentScopeIds);
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
