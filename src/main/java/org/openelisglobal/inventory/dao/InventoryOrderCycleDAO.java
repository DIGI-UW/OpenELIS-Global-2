package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;

public interface InventoryOrderCycleDAO extends BaseDAO<InventoryOrderCycle, Long> {

    /** Cycles received on or after cutoff, across all items, newest first. */
    List<InventoryOrderCycle> getReceivedSince(java.sql.Timestamp cutoff) throws LIMSRuntimeException;
}
