package org.openelisglobal.siteinformation.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
public interface SiteInformationDomainService extends BaseObjectService<SiteInformationDomain, String> {
    SiteInformationDomain getByName(String name);
}
