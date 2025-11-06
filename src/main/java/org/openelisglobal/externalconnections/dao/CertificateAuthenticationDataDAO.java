package org.openelisglobal.externalconnections.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;

public interface CertificateAuthenticationDataDAO extends BaseDAO<CertificateAuthenticationData, Integer> {

    Optional<CertificateAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
