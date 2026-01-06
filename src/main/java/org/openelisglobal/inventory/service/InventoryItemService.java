package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, Long> {

    List<ItemType> getAllItemTypes();

    /** Get all active inventory items */
    List<InventoryItem> getAllActive();

    /** Get items by item type (REAGENT, RDT, CARTRIDGE) */
    List<InventoryItem> getByItemType(ItemType itemType);

    /** Get items by category */
    List<InventoryItem> getByCategory(String category);

    /** Search items by name (partial matching) */
    List<InventoryItem> searchByName(String searchTerm);

    /**
     * Get items with low stock levels Returns items where total current quantity
     * across all lots is below minimum stock level
     */
    List<InventoryItem> getLowStockItems();

    /** Get item by FHIR UUID */
    InventoryItem getByFhirUuid(String fhirUuid);

    /**
     * Calculate total current stock quantity for an item across all available lots
     */
    Double getTotalCurrentStock(Long itemId);

    /** Check if an item is currently in stock (has available lots) */
    boolean isInStock(Long itemId);

    /** Deactivate an item (soft delete) */
    void deactivateItem(Long itemId, String sysUserId);

    /** Activate an item (restore from soft delete) */
    void activateItem(Long itemId, String sysUserId);

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
            Boolean isActive, String searchTerm);

    /**
     * Get total count of items matching the same filters as getPagedItems
     */
    Long getPagedItemsCount(ItemType itemType, Boolean isActive, String searchTerm);
}
