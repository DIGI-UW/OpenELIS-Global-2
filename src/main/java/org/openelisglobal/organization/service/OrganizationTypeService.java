package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationType;

public interface OrganizationTypeService extends BaseObjectService<OrganizationType, String> {

    List<OrganizationType> getAllOrganizationTypes();

    OrganizationType getOrganizationTypeByName(String name);
}
