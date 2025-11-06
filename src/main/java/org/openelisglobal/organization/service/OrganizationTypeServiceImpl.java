package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.organization.dao.OrganizationTypeDAO;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationTypeServiceImpl extends AuditableBaseObjectServiceImpl<OrganizationType, String>
        implements OrganizationTypeService {
    @Autowired
    protected OrganizationTypeDAO baseObjectDAO;

    public OrganizationTypeServiceImpl() {
        super(OrganizationType.class);
    }

    @Override
    protected OrganizationTypeDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationType> getAllOrganizationTypes() {
        return getBaseObjectDAO().getAllOrganizationTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationType getOrganizationTypeByName(String name) {
        return getBaseObjectDAO().getOrganizationTypeByName(name);
    }
}
