package org.openelisglobal.siteinformation.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.siteinformation.valueholder.SiteInformationDomain;

public interface SiteInformationDomainService extends BaseObjectService<SiteInformationDomain, String> {
    SiteInformationDomain getByName(String name);
}
