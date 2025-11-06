package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.dao.NCEventDAO;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NCEventServiceImpl extends AuditableBaseObjectServiceImpl<NcEvent, String> implements NCEventService {

    @Autowired
    protected NCEventDAO baseObjectDAO;

    public NCEventServiceImpl() {
        super(NcEvent.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId) {
        return baseObjectDAO.findByNCENumberOrLabOrderId(nceNumber, labOrderId);
    }

    @Override
    protected NCEventDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
