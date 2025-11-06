package org.openelisglobal.inventory.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryLocationDAO;
import org.openelisglobal.inventory.valueholder.InventoryLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InventoryLocationServiceImpl extends AuditableBaseObjectServiceImpl<InventoryLocation, String>
        implements InventoryLocationService {
    @Autowired
    protected InventoryLocationDAO baseObjectDAO;

    InventoryLocationServiceImpl() {
        super(InventoryLocation.class);
    }

    @Override
    protected InventoryLocationDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
