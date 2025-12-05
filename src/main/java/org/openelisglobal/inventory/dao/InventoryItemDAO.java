/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryItem;

/**
 * DAO for InventoryItem entity Provides methods to query inventory items
 * (reagents, RDTs, cartridges)
 */
public interface InventoryItemDAO extends BaseDAO<InventoryItem, String> {

    /**
     * Get all inventory items
     * 
     * @deprecated Use getAll() from BaseDAO instead
     */
    @Deprecated
    List<InventoryItem> getAllInventoryItems() throws LIMSRuntimeException;

    /**
     * Read inventory item by ID
     * 
     * @deprecated Use get(String id) from BaseDAO instead
     */
    @Deprecated
    InventoryItem readInventoryItem(String idString) throws LIMSRuntimeException;

    /**
     * Find all active inventory items
     *
     * @return List of active items
     */
    List<InventoryItem> findAllActive();

    /**
     * Find inventory items by item type
     *
     * @param itemType Item type (REAGENT, RDT, CARTRIDGE)
     * @return List of items matching the type
     */
    List<InventoryItem> findByItemType(String itemType);

    /**
     * Find inventory items by category
     *
     * @param category Category name
     * @return List of items in the category
     */
    List<InventoryItem> findByCategory(String category);

    /**
     * Search inventory items by name (case-insensitive, partial match)
     *
     * @param name Search term
     * @return List of matching items
     */
    List<InventoryItem> searchByName(String name);

    /**
     * Find items with low stock (total current quantity below threshold)
     *
     * @return List of items with low stock
     */
    List<InventoryItem> findLowStockItems();
}
