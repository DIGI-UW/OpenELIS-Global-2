package org.openelisglobal.externalconnections.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionContact;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ExternalConnectionContactDAOImpl extends BaseDAOImpl<ExternalConnectionContact, Integer>
        implements ExternalConnectionContactDAO {

    public ExternalConnectionContactDAOImpl() {
        super(ExternalConnectionContact.class);
    }
}
