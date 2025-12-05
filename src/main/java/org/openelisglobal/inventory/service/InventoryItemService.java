package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, String> {

    InventoryItem readInventoryItem(String idString);

    List<InventoryItem> getAllInventoryItems();

    List<InventoryItem> findAllActive();

    List<InventoryItem> findByItemType(String itemType);

    List<InventoryItem> findByCategory(String category);

    List<InventoryItem> searchByName(String name);

    List<InventoryItem> findLowStockItems();
}
