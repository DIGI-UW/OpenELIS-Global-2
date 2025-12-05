package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryTransactionDAO;
import org.openelisglobal.inventory.valueholder.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTransactionServiceImpl extends AuditableBaseObjectServiceImpl<InventoryTransaction, String>
        implements InventoryTransactionService {

    @Autowired
    protected InventoryTransactionDAO baseObjectDAO;

    InventoryTransactionServiceImpl() {
        super(InventoryTransaction.class);
    }

    @Override
    protected InventoryTransactionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> findByLotId(String lotId) {
        return getBaseObjectDAO().findByLotId(lotId);
    }
}
