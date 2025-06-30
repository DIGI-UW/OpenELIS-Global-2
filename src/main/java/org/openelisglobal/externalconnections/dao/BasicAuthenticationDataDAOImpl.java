package org.openelisglobal.externalconnections.dao;

import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BasicAuthenticationDataDAOImpl extends BaseDAOImpl<BasicAuthenticationData, Integer>
        implements BasicAuthenticationDataDAO {

    public BasicAuthenticationDataDAOImpl() {
        super(BasicAuthenticationData.class);
    }

    @Override
    public Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId) {
        if (externalConnectionId == null) {
            return Optional.empty();
        }
        BasicAuthenticationData data;
        try {
            String sql = "from BasicAuthenticationData as cad where cad.externalConnection.id ="
                    + " :externalConnectionId";
            Query<BasicAuthenticationData> query = entityManager.unwrap(Session.class).createQuery(sql,
                    BasicAuthenticationData.class);
            query.setParameter("externalConnectionId", externalConnectionId);
            data = query.uniqueResult();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in BasicAuthenticationDataDAOImpl getByExternalConnection()", e);
        }

        return Optional.ofNullable(data);
    }
}
