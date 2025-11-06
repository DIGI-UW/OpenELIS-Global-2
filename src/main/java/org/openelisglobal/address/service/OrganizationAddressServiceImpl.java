package org.openelisglobal.address.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.address.dao.OrganizationAddressDAO;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationAddressServiceImpl extends AuditableBaseObjectServiceImpl<OrganizationAddress, AddressPK>
        implements OrganizationAddressService {
    @Autowired
    protected OrganizationAddressDAO baseObjectDAO;

    OrganizationAddressServiceImpl() {
        super(OrganizationAddress.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected OrganizationAddressDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationAddress> getAddressPartsByOrganizationId(String organizationId) {
        return baseObjectDAO.getAddressPartsByOrganizationId(organizationId);
    }
}
