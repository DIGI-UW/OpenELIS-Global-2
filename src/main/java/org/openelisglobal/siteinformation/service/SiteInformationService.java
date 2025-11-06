package org.openelisglobal.siteinformation.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public interface SiteInformationService extends BaseObjectService<SiteInformation, String> {
    void getData(SiteInformation siteInformation);

    SiteInformation getSiteInformationByName(String siteName);

    List<SiteInformation> getAllSiteInformation();

    int getCountForDomainName(String domainName);

    SiteInformation getSiteInformationById(String urlId);

    List<SiteInformation> getSiteInformationByDomainName(String domainName);

    List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String domainName);

    void persistData(SiteInformation siteInformation, boolean newSiteInformation);

    List<SiteInformation> updateSiteInformationByName(Map<String, String> map);
}
