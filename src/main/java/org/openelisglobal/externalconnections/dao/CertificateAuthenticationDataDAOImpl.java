package org.openelisglobal.externalconnections.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.externalconnections.valueholder.CertificateAuthenticationData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CertificateAuthenticationDataDAOImpl extends BaseDAOImpl<CertificateAuthenticationData, Integer>
        implements CertificateAuthenticationDataDAO {

    public CertificateAuthenticationDataDAOImpl() {
        super(CertificateAuthenticationData.class);
    }

    @Override
    public Optional<CertificateAuthenticationData> getByExternalConnection(Integer externalConnectionId) {
        CertificateAuthenticationData data;
        try {
            String sql = "from CertificateAuthenticationData as cad where cad.externalConnection.id ="
                    + " :externalConnectionId";
            Query<CertificateAuthenticationData> query = entityManager.unwrap(Session.class).createQuery(sql,
                    CertificateAuthenticationData.class);
            query.setParameter("externalConnectionId", externalConnectionId);
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in CertificateAuthenticationDataDAOImpl getByExternalConnection()",
                    e);
        }

        return Optional.ofNullable(data);
    }
}
