package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.organization.dao.OrganizationContactDAO;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationContactServiceImpl extends AuditableBaseObjectServiceImpl<OrganizationContact, String>
        implements OrganizationContactService {
    @Autowired
    protected OrganizationContactDAO baseObjectDAO;

    OrganizationContactServiceImpl() {
        super(OrganizationContact.class);
    }

    @Override
    protected OrganizationContactDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationContact> getListForOrganizationId(String orgId) {
        return getBaseObjectDAO().getListForOrganizationId(orgId);
    }
}
