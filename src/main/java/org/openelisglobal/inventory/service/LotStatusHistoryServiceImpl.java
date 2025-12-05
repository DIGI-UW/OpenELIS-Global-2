package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.inventory.dao.LotStatusHistoryDAO;
import org.openelisglobal.inventory.valueholder.LotStatusHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotStatusHistoryServiceImpl extends AuditableBaseObjectServiceImpl<LotStatusHistory, String>
        implements LotStatusHistoryService {

    @Autowired
    protected LotStatusHistoryDAO baseObjectDAO;

    LotStatusHistoryServiceImpl() {
        super(LotStatusHistory.class);
    }

    @Override
    protected LotStatusHistoryDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LotStatusHistory> findByLotId(String lotId) {
        return getBaseObjectDAO().findByLotId(lotId);
    }
}
