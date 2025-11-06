package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;

public interface CertificateAuthenticationDataService
        extends BaseObjectService<CertificateAuthenticationData, Integer> {

    Optional<CertificateAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
