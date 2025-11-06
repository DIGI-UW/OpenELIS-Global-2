package org.openelisglobal.requester.service;

import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.requester.dao.RequesterTypeDAO;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequesterTypeServiceImpl extends AuditableBaseObjectServiceImpl<RequesterType, String>
        implements RequesterTypeService {
    @Autowired
    protected RequesterTypeDAO baseObjectDAO;

    public RequesterTypeServiceImpl() {
        super(RequesterType.class);
    }

    @Override
    protected RequesterTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public RequesterType getRequesterTypeByName(String typeName) {
        return getBaseObjectDAO().getRequesterTypeByName(typeName);
    }
}
