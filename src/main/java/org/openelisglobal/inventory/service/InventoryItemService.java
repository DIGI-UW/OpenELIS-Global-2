package org.openelisglobal.inventory.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, Long> {

    /**
     * Every distinct tag any item carries, alphabetically — the typeahead's
     * suggestion list.
     */
    List<String> getAllTags();

    /**
     * Record that these items have been ordered, stamping each with the moment the
     * mark was made.
     *
     * <p>
     * Reversible by design — {@link #clearOrdered} undoes it — because this is an
     * acknowledgement that an order was placed, not the order itself, and a lab
     * that marks the wrong row needs a way back.
     *
     * @return the number of items whose mark actually changed
     */
    int markOrdered(List<Long> itemIds, String note, LocalDate expectedDate, String sysUserId);

    /** Undo {@link #markOrdered}, clearing the stamp, note and expected date. */
    int clearOrdered(List<Long> itemIds, String sysUserId);

    /**
     * Get all active inventory items
     */
    List<InventoryItem> getAllActive();

    /**
     * Get items by item type (REAGENT, RDT, CARTRIDGE)
     */
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

    /** The item carrying this UPC, or null. */
    InventoryItem getByUpc(String upc);

    /** The item with exactly this name, deactivated ones included, or null. */
    InventoryItem getByExactName(String name);

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
