package org.openelisglobal.inventory.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, Long> {

    List<ItemType> getAllItemTypes();

    /** Every distinct tag any item carries, alphabetically. */
    List<String> getAllTags();

    int markOrdered(List<Long> itemIds, String note, LocalDate expectedDate, String sysUserId);

    int clearOrdered(List<Long> itemIds, String sysUserId);

    /**
     * Get all active inventory items
     */
    List<InventoryItem> getAllActive();

    /**
     * Get items by item type (REAGENT, RDT, CARTRIDGE)
     */
    List<InventoryItem> getByItemType(ItemType itemType);

    /**
     * Get items by category
     */
    List<InventoryItem> getByCategory(String category);

    /**
     * Search items by name (partial matching)
     */
    List<InventoryItem> searchByName(String searchTerm);

    /**
     * Get items with low stock levels Returns items where total current quantity
     * across all lots is below minimum stock level
     */
    List<InventoryItem> getLowStockItems();

    /**
     * Get item by FHIR UUID
     */
    InventoryItem getByFhirUuid(String fhirUuid);

    /**
     * Calculate total current stock quantity for an item across all available lots
     */
    Double getTotalCurrentStock(Long itemId);

    /**
     * Check if an item is currently in stock (has available lots)
     */
    boolean isInStock(Long itemId);

    /**
     * Deactivate an item (soft delete)
     */
    void deactivateItem(Long itemId, String sysUserId);

    /**
     * Activate an item (restore from soft delete)
     */
    void activateItem(Long itemId, String sysUserId);
}
