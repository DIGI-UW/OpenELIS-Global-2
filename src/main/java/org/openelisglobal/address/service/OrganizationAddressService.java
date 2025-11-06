package org.openelisglobal.address.service;

import java.util.List;
import org.openelisglobal.address.valueholder.AddressPK;
import org.openelisglobal.address.valueholder.OrganizationAddress;
import org.openelisglobal.common.service.BaseObjectService;

public interface OrganizationAddressService extends BaseObjectService<OrganizationAddress, AddressPK> {

    List<OrganizationAddress> getAddressPartsByOrganizationId(String organizationId);
}
