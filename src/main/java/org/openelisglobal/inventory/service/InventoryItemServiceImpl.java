package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryItemDAO;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, String>
        implements InventoryItemService {
    @Autowired
    protected InventoryItemDAO baseObjectDAO;

    InventoryItemServiceImpl() {
        super(InventoryItem.class);
    }

    @Override
    protected InventoryItemDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public InventoryItem readInventoryItem(String idString) {
        return getBaseObjectDAO().readInventoryItem(idString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllInventoryItems() {
        return getBaseObjectDAO().getAllInventoryItems();
    }
}
