package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;

@CrossDomainService(callers = "ExternalConnectionRestController (PRIV_EXTCONNECTION_VIEW) — mocked in AppTestConfig; authorization enforced by the controller")
public interface BasicAuthenticationDataService extends BaseObjectService<BasicAuthenticationData, Integer> {

    Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
