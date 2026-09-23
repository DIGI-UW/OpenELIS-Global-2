package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;

public interface InventoryOrderCycleService extends BaseObjectService<InventoryOrderCycle, Long> {

    /**
     * Every cycle closed on or after the cut-off, across all items, newest first.
     */
    List<InventoryOrderCycle> getReceivedSince(Timestamp cutoff);
}
