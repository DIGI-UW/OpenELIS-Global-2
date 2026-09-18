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

    /**
     * Get inventory item by FHIR UUID
     */
    InventoryItem getByFhirUuid(String fhirUuid) throws LIMSRuntimeException;

    /**
     * Every distinct tag any item carries, alphabetically — the typeahead's
     * suggestion list.
     */
    List<String> getAllTags();

    /**
     * The stored spellings of the given tags, matched on the canonical key
     * (trimmed, inner whitespace collapsed, lower case).
     *
     * <p>
     * A narrower {@link #getAllTags()} for the one thing a write needs: which
     * spelling of the handful of tags on this item is already in use. Reading the
     * whole table to answer that cost a full scan on every item write, and a CSV
     * import pays it once per row.
     *
     * @param canonicalKeys the keys to look for, already canonicalised
     */
    List<String> getTagsMatching(java.util.Collection<String> canonicalKeys);
}
