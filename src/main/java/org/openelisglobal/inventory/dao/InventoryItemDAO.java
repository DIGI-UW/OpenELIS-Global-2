package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemDAO extends BaseDAO<InventoryItem, Long> {

    /**
     * Get all active inventory items
     */
    List<InventoryItem> getAllActive() throws LIMSRuntimeException;

    /**
     * Get inventory items by category
     */
    List<InventoryItem> getByCategory(String category) throws LIMSRuntimeException;

    /**
     * Search inventory items by name (partial match)
     */
    List<InventoryItem> searchByName(String name) throws LIMSRuntimeException;

    /**
     * Get inventory item by its human-readable code
     */
    InventoryItem getByCode(String code) throws LIMSRuntimeException;

    /** The item carrying this UPC, or null. */
    InventoryItem getByUpc(String upc) throws LIMSRuntimeException;

    /** The item with this name in any case, deactivated ones included, or null. */
    InventoryItem getByExactName(String name) throws LIMSRuntimeException;

    /**
     * Get inventory item by FHIR UUID
     */
    InventoryItem getByFhirUuid(String fhirUuid) throws LIMSRuntimeException;

    /** Every distinct tag any item carries, alphabetically. */
    List<String> getAllTags();

    /** Stored spellings whose canonical key is in canonicalKeys. */
    List<String> getTagsMatching(java.util.Collection<String> canonicalKeys);
}
