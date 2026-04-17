package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationContact;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
public interface OrganizationContactService extends BaseObjectService<OrganizationContact, String> {
    List<OrganizationContact> getListForOrganizationId(String orgId);
}
