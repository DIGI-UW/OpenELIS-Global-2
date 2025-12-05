package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.InventoryUsageDAO;
import org.openelisglobal.inventory.valueholder.InventoryUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryUsageServiceImpl extends AuditableBaseObjectServiceImpl<InventoryUsage, String>
        implements InventoryUsageService {

    @Autowired
    protected InventoryUsageDAO baseObjectDAO;

    InventoryUsageServiceImpl() {
        super(InventoryUsage.class);
    }

    @Override
    protected InventoryUsageDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> findByLotId(String lotId) {
        return getBaseObjectDAO().findByLotId(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryUsage> findByTestResultId(String testResultId) {
        return getBaseObjectDAO().findByTestResultId(testResultId);
    }
}
