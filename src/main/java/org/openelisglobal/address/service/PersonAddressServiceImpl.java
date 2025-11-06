package org.openelisglobal.address.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.address.dao.PersonAddressDAO;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonAddressServiceImpl extends AuditableBaseObjectServiceImpl<PersonAddress, AddressPK>
        implements PersonAddressService {
    @Autowired
    protected PersonAddressDAO baseObjectDAO;

    PersonAddressServiceImpl() {
        super(PersonAddress.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected PersonAddressDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonAddress> getAddressPartsByPersonId(String personId) {
        return baseObjectDAO.getAddressPartsByPersonId(personId);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonAddress getByPersonIdAndPartId(String personId, String addressPartId) {
        return baseObjectDAO.getByPersonIdAndPartId(personId, addressPartId);
    }

    @Override
    public AddressPK insert(PersonAddress personAddress) {
        return super.insert(personAddress);
    }
}
