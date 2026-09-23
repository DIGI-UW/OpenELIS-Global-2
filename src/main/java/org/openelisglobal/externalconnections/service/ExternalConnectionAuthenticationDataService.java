package org.openelisglobal.externalconnections.service;

import java.util.Map;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ExternalConnectionAuthenticationDataService {

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_VIEW')")
    Map<AuthType, ExternalConnectionAuthenticationData> getForExternalConnection(Integer externalConnectionId);

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    Integer insert(ExternalConnectionAuthenticationData authData);

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    ExternalConnectionAuthenticationData update(ExternalConnectionAuthenticationData authData);

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_MANAGE')")
    ExternalConnectionAuthenticationData save(ExternalConnectionAuthenticationData authData);
}
