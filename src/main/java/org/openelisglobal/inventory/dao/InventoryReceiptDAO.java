package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;

public interface InventoryReceiptDAO extends BaseDAO<InventoryReceipt, String> {

    void getData(InventoryReceipt inventoryReceipt);

    InventoryReceipt getInventoryReceiptById(String id);

    List<InventoryReceipt> getAllInventoryReceipts();

    InventoryReceipt getInventoryReceiptByInventoryItemId(String id);

    List<InventoryReceipt> findByInventoryItemId(String inventoryItemId);
}
