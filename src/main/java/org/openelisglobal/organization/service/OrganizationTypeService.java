package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
public interface OrganizationTypeService extends BaseObjectService<OrganizationType, String> {

    List<OrganizationType> getAllOrganizationTypes();

    OrganizationType getOrganizationTypeByName(String name);

    List<String> getOrganizationIdsForType(String typeId);
}
