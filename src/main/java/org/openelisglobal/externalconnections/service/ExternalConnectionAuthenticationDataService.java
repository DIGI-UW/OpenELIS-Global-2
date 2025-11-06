package org.openelisglobal.externalconnections.service;

import java.util.Map;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;

public interface ExternalConnectionAuthenticationDataService {

    Map<AuthType, ExternalConnectionAuthenticationData> getForExternalConnection(Integer externalConnectionId);

    Integer insert(ExternalConnectionAuthenticationData authData);

    ExternalConnectionAuthenticationData update(ExternalConnectionAuthenticationData authData);

    ExternalConnectionAuthenticationData save(ExternalConnectionAuthenticationData authData);
}
