package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CertificateAuthenticationDataService
        extends BaseObjectService<CertificateAuthenticationData, Integer> {

    @PreAuthorize("hasAuthority('PRIV_EXTCONNECTION_VIEW')")
    Optional<CertificateAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
