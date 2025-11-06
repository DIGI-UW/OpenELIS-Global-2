package org.openelisglobal.externalconnections.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.externalconnections.dao.ExternalConnectionDAO;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalConnectionServiceImpl extends AuditableBaseObjectServiceImpl<ExternalConnection, Integer>
        implements ExternalConnectionService {

    @Autowired
    protected ExternalConnectionDAO baseObjectDAO;

    @Autowired
    private ExternalConnectionAuthenticationDataService externalConnectionAuthenticationDataService;

    @Autowired
    private ExternalConnectionContactService externalConnectionContactService;

    ExternalConnectionServiceImpl() {
        super(ExternalConnection.class);
        this.auditTrailLog = false;
    }

    @Override
    protected ExternalConnectionDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public void createNewExternalConnection(
            Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection) {
        Integer id = baseObjectDAO.insert(externalConnection);
        externalConnection = baseObjectDAO.get(id).get();
        for (ExternalConnectionAuthenticationData authData : externalConnectionAuthData.values()) {
            authData.setExternalConnection(externalConnection);
            externalConnectionAuthenticationDataService.insert(authData);
        }

        for (ExternalConnectionContact externalConnectionContact : externalConnectionContacts) {
            externalConnectionContact.setExternalConnection(externalConnection);
            externalConnectionContactService.insert(externalConnectionContact);
        }
    }

    @Override
    @Transactional
    public void updateExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection) {
        ExternalConnection updatedExternalConnection = baseObjectDAO.update(externalConnection);

        for (ExternalConnectionAuthenticationData authData : externalConnectionAuthData.values()) {
            authData.setExternalConnection(updatedExternalConnection);
            externalConnectionAuthenticationDataService.save(authData);
        }
        for (ExternalConnectionContact externalConnectionContact : externalConnectionContacts) {
            externalConnectionContact.setExternalConnection(updatedExternalConnection);
            externalConnectionContactService.save(externalConnectionContact);
        }
    }
}
