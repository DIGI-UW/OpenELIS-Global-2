package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryLotDAO;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryLotServiceImpl extends AuditableBaseObjectServiceImpl<InventoryLot, String>
        implements InventoryLotService {

    @Autowired
    protected InventoryLotDAO baseObjectDAO;

    InventoryLotServiceImpl() {
        super(InventoryLot.class);
    }

    @Override
    protected InventoryLotDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findByInventoryItemId(String inventoryItemId) {
        return getBaseObjectDAO().findByInventoryItemId(inventoryItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findAvailableLotsByItemFEFO(String inventoryItemId) {
        return getBaseObjectDAO().findAvailableLotsByItemFEFO(inventoryItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findExpiringSoon(int daysAhead) {
        return getBaseObjectDAO().findExpiringSoon(daysAhead);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLot> findExpiredActiveLots() {
        return getBaseObjectDAO().findExpiredActiveLots();
    }
}
