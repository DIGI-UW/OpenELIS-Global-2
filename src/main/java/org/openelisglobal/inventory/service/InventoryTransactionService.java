package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;

public interface InventoryTransactionService extends BaseObjectService<InventoryTransaction, String> {

    List<InventoryTransaction> findByLotId(String lotId);
}
