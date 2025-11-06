package org.openelisglobal.externalconnections.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ExternalConnectionDAOImpl extends BaseDAOImpl<ExternalConnection, Integer>
        implements ExternalConnectionDAO {

    public ExternalConnectionDAOImpl() {
        super(ExternalConnection.class);
    }
}
