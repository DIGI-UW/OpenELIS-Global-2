package org.openelisglobal.externalconnections.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;

public interface BasicAuthenticationDataDAO extends BaseDAO<BasicAuthenticationData, Integer> {

    Optional<BasicAuthenticationData> getByExternalConnection(Integer externalConnectionId);
}
