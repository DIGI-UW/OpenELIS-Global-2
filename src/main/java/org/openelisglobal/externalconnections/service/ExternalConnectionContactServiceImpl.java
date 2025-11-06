package org.openelisglobal.externalconnections.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.externalconnections.dao.ExternalConnectionContactDAO;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExternalConnectionContactServiceImpl extends
        AuditableBaseObjectServiceImpl<ExternalConnectionContact, Integer> implements ExternalConnectionContactService {

    @Autowired
    protected ExternalConnectionContactDAO baseObjectDAO;

    ExternalConnectionContactServiceImpl() {
        super(ExternalConnectionContact.class);
        this.auditTrailLog = false;
    }

    @Override
    protected ExternalConnectionContactDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }
}
