package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BasicAuthenticationDataService extends BaseObjectService<BasicAuthenticationData, Integer> {

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_VIEW')")
    Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
