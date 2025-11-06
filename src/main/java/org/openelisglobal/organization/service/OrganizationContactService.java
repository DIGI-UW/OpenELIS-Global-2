package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationContact;

public interface OrganizationContactService extends BaseObjectService<OrganizationContact, String> {
    List<OrganizationContact> getListForOrganizationId(String orgId);
}
