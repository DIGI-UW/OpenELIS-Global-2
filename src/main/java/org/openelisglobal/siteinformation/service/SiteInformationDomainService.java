package org.openelisglobal.siteinformation.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;

@CrossDomainService(callers = "SiteInformationConfigurationHandler (startup), DisplayListController (public endpoint) — getByName is public; inherited write methods are guarded by BaseObjectService callers")
public interface SiteInformationDomainService extends BaseObjectService<SiteInformationDomain, String> {
    SiteInformationDomain getByName(String name);
}
