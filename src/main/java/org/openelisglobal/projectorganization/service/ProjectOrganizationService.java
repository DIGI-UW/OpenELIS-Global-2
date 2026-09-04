package org.openelisglobal.projectorganization.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.projectorganization.valueholder.ProjectOrganization;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_EQA_VIEW')")
public interface ProjectOrganizationService extends BaseObjectService<ProjectOrganization, String> {
}
