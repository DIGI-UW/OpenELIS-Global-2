package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.security.access.prepost.PreAuthorize;

public interface InventoryItemService extends BaseObjectService<InventoryItem, Long> {

    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<ItemType> getAllItemTypes();

    /**
     * Get all active inventory items
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryItem> getAllActive();

    /**
     * Get items by item type (REAGENT, RDT, CARTRIDGE)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryItem> getByItemType(ItemType itemType);

    /**
     * Get items by category
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryItem> getByCategory(String category);

    /**
     * Search items by name (partial matching)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryItem> searchByName(String searchTerm);

    /**
     * Get items with low stock levels Returns items where total current quantity
     * across all lots is below minimum stock level
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    List<InventoryItem> getLowStockItems();

    /**
     * Get item by FHIR UUID
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    InventoryItem getByFhirUuid(String fhirUuid);

    /**
     * Calculate total current stock quantity for an item across all available lots
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    Double getTotalCurrentStock(Long itemId);

    /**
     * Check if an item is currently in stock (has available lots)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_VIEW')")
    boolean isInStock(Long itemId);

    /**
     * Deactivate an item (soft delete)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    void deactivateItem(Long itemId, String sysUserId);

    /**
     * Activate an item (restore from soft delete)
     */
    @PreAuthorize("hasAuthority('PRIV_INVENTORY_MANAGE')")
    void activateItem(Long itemId, String sysUserId);
}
