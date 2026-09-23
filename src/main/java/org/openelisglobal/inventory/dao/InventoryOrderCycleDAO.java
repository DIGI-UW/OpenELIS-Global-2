package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;

public interface InventoryOrderCycleDAO extends BaseDAO<InventoryOrderCycle, Long> {

    /**
     * Every cycle closed on or after the given cut-off, newest first, across all
     * items.
     *
     * <p>
     * Deliberately not per item: the board resolves a lead time for every row it
     * renders, and a per-item read would turn one query into one per item. Callers
     * group the result in memory.
     */
    List<InventoryOrderCycle> getReceivedSince(java.sql.Timestamp cutoff) throws LIMSRuntimeException;
}
