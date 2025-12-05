package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;

public interface InventoryTransactionDAO extends BaseDAO<InventoryTransaction, String> {

    List<InventoryTransaction> findByLotId(String lotId);
}
