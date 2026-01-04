package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemDAO extends BaseDAO<InventoryItem, Long> {

    /**
     * Get all active inventory items
     */
    List<InventoryItem> getAllActive() throws LIMSRuntimeException;

    /**
     * Get inventory items by type
     */
    List<InventoryItem> getByItemType(ItemType itemType) throws LIMSRuntimeException;

    /**
     * Get inventory items by category
     */
    List<InventoryItem> getByCategory(String category) throws LIMSRuntimeException;

    /**
     * Search inventory items by name (partial match)
     */
    List<InventoryItem> searchByName(String name) throws LIMSRuntimeException;

    /**
     * Get inventory item by FHIR UUID
     */
    InventoryItem getByFhirUuid(String fhirUuid) throws LIMSRuntimeException;

    /**
     * Get items with low stock (total quantity < threshold) This requires joining
     * with InventoryLot to calculate total stock
     */
    List<InventoryItem> getLowStockItems() throws LIMSRuntimeException;

    List<ItemType> getAllItemTypes();

    /**
     * Get paginated inventory items with filtering and sorting
     *
     * @param limit      Maximum number of results to return
     * @param offset     Number of results to skip
     * @param sortBy     Field to sort by (e.g., "name", "itemType")
     * @param sortOrder  Sort direction ("asc" or "desc")
     * @param itemType   Filter by item type (optional)
     * @param isActive   Filter by active status (optional)
     * @param searchTerm Search term for item name (optional)
     * @return List of paginated items
     */
    List<InventoryItem> getPagedItems(int limit, int offset, String sortBy, String sortOrder, ItemType itemType,
            Boolean isActive, String searchTerm) throws LIMSRuntimeException;

    /**
     * Get total count of items matching the same filters as getPagedItems
     */
    Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm) throws LIMSRuntimeException;
}
