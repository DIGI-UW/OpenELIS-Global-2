package org.openelisglobal.testresultsview.service;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.testresultsview.dao.ClientResultsViewInfoDAO;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientResultsViewInfoServiceImpl extends AuditableBaseObjectServiceImpl<ClientResultsViewBean, Integer>
        implements ClientResultsViewInfoService {

    @Autowired
    private ClientResultsViewInfoDAO baseObjectDAO;

    public ClientResultsViewInfoServiceImpl() {
        super(ClientResultsViewBean.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<ClientResultsViewBean, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
