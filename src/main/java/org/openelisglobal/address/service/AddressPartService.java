package org.openelisglobal.address.service;

import java.util.List;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.common.service.BaseObjectService;

public interface AddressPartService extends BaseObjectService<AddressPart, String> {
    List<AddressPart> getAll();

    AddressPart getAddresPartByName(String name);
}
