package org.openelisglobal.externalconnections.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;

public interface ExternalConnectionService extends BaseObjectService<ExternalConnection, Integer> {

    void createNewExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);

    void updateExternalConnection(Map<AuthType, ExternalConnectionAuthenticationData> externalConnectionAuthData,
            List<ExternalConnectionContact> externalConnectionContacts, ExternalConnection externalConnection);

    void updateExternalConnectionFields(Integer id, String sysUserId, Boolean active,
            ProgrammedConnection programmedConnection, AuthType authType, URI uri, String nameValue,
            String descriptionValue, String basicUsername, String basicPassword);
}
