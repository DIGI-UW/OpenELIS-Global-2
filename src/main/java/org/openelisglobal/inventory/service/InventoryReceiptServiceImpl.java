package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryReceiptDAO;
import org.openelisglobal.inventory.valueholder.InventoryReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReceiptServiceImpl extends AuditableBaseObjectServiceImpl<InventoryReceipt, String>
        implements InventoryReceiptService {
    @Autowired
    protected InventoryReceiptDAO baseObjectDAO;

    InventoryReceiptServiceImpl() {
        super(InventoryReceipt.class);
    }

    @Override
    protected InventoryReceiptDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(InventoryReceipt inventoryReceipt) {
        getBaseObjectDAO().getData(inventoryReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptById(String id) {
        return getBaseObjectDAO().getInventoryReceiptById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReceipt> getAllInventoryReceipts() {
        return getBaseObjectDAO().getAllInventoryReceipts();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReceipt getInventoryReceiptByInventoryItemId(String id) {
        return getBaseObjectDAO().getInventoryReceiptByInventoryItemId(id);
    }
}
