package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, String> {

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
    Double getTotalCurrentStock(String itemId);

    /**
     * Check if an item is currently in stock (has available lots)
     */
    boolean isInStock(String itemId);

    /**
     * Deactivate an item (soft delete)
     */
    void deactivateItem(String itemId, String sysUserId);
}
