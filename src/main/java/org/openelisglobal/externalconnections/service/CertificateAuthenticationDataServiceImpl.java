package org.openelisglobal.externalconnections.service;

import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.externalconnections.dao.CertificateAuthenticationDataDAO;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CertificateAuthenticationDataServiceImpl
        extends AuditableBaseObjectServiceImpl<CertificateAuthenticationData, Integer>
        implements CertificateAuthenticationDataService {

    @Autowired
    protected CertificateAuthenticationDataDAO baseObjectDAO;

    CertificateAuthenticationDataServiceImpl() {
        super(CertificateAuthenticationData.class);
        this.auditTrailLog = false;
    }

    @Override
    protected CertificateAuthenticationDataDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public Optional<CertificateAuthenticationData> getByExternalConnection(Integer externalConnectionId) {
        return baseObjectDAO.getByExternalConnection(externalConnectionId);
    }
}
