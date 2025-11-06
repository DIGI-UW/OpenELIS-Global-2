package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;

public interface BasicAuthenticationDataService extends BaseObjectService<BasicAuthenticationData, Integer> {

    Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
