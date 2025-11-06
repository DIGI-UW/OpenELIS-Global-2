package org.openelisglobal.externalconnections.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;

public interface ExternalConnectionService extends BaseObjectService<ExternalConnection, Integer> {

    void createNewExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);

    void updateExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);
}
