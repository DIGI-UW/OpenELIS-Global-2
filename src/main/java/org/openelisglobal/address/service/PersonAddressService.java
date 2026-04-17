package org.openelisglobal.address.service;

import java.util.List;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface PersonAddressService extends BaseObjectService<PersonAddress, AddressPK> {

    @Override
    AddressPK insert(PersonAddress personAddress);

    List<PersonAddress> getAddressPartsByPersonId(String personId);

    PersonAddress getByPersonIdAndPartId(String personId, String addressPartId);
}
