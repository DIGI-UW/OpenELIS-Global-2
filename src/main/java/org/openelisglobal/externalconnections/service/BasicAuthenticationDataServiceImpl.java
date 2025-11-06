package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.externalconnections.dao.BasicAuthenticationDataDAO;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BasicAuthenticationDataServiceImpl extends AuditableBaseObjectServiceImpl<BasicAuthenticationData, Integer>
        implements BasicAuthenticationDataService {

    @Autowired
    protected BasicAuthenticationDataDAO baseObjectDAO;

    BasicAuthenticationDataServiceImpl() {
        super(BasicAuthenticationData.class);
        this.auditTrailLog = false;
    }

    @Override
    protected BasicAuthenticationDataDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId) {
        return baseObjectDAO.getByExternalConnection(externalConnectionId);
    }
}
