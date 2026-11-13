package org.openelisglobal.inventory.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryOrderCycleDAO;
import org.openelisglobal.inventory.valueholder.InventoryOrderCycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryOrderCycleServiceImpl extends AuditableBaseObjectServiceImpl<InventoryOrderCycle, Long>
        implements InventoryOrderCycleService {

    @Autowired
    private InventoryOrderCycleDAO inventoryOrderCycleDAO;

    public InventoryOrderCycleServiceImpl() {
        super(InventoryOrderCycle.class);
    }

    @Override
    protected InventoryOrderCycleDAO getBaseObjectDAO() {
        return inventoryOrderCycleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryOrderCycle> getReceivedSince(Timestamp cutoff) {
        return inventoryOrderCycleDAO.getReceivedSince(cutoff);
    }
}
