package org.openelisglobal.address.service;

import org.openelisglobal.address.dao.AddressPartDAO;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressPartServiceImpl extends AuditableBaseObjectServiceImpl<AddressPart, String>
        implements AddressPartService {
    @Autowired
    protected AddressPartDAO baseObjectDAO;

    public AddressPartServiceImpl() {
        super(AddressPart.class);
    }

    @Override
    protected AddressPartDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public AddressPart getAddresPartByName(String name) {
        return getMatch("partName", name).orElse(null);
    }
}
