package org.openelisglobal.result.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.result.dao.ResultInventoryDAO;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultInventoryServiceImpl extends AuditableBaseObjectServiceImpl<ResultInventory, String>
        implements ResultInventoryService {
    @Autowired
    protected ResultInventoryDAO baseObjectDAO;

    ResultInventoryServiceImpl() {
        super(ResultInventory.class);
    }

    @Override
    protected ResultInventoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(ResultInventory resultInventory) {
        getBaseObjectDAO().getData(resultInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultInventory getResultInventoryById(ResultInventory resultInventory) {
        return getBaseObjectDAO().getResultInventoryById(resultInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultInventory> getAllResultInventoryss() {
        return getBaseObjectDAO().getAllResultInventorys();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResultInventory> getResultInventorysByResult(Result result) {
        return getBaseObjectDAO().getResultInventorysByResult(result);
    }
}
