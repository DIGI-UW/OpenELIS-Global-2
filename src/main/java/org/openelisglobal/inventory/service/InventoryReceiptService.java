package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;

public interface InventoryReceiptService extends BaseObjectService<InventoryReceipt, String> {
    void getData(InventoryReceipt inventoryReceipt);

    InventoryReceipt getInventoryReceiptById(String id);

    List<InventoryReceipt> getAllInventoryReceipts();

    InventoryReceipt getInventoryReceiptByInventoryItemId(String id);
}
