package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryItem;

public interface InventoryItemService extends BaseObjectService<InventoryItem, String> {

    InventoryItem readInventoryItem(String idString);

    List<InventoryItem> getAllInventoryItems();
}
