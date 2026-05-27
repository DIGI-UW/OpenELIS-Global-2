package org.openelisglobal.siteinformation.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SiteInformationService extends BaseObjectService<SiteInformation, String> {
    void getData(SiteInformation siteInformation);

    SiteInformation getSiteInformationByName(String siteName);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SiteInformation> getAllSiteInformation();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    int getCountForDomainName(String domainName);

    SiteInformation getSiteInformationById(String urlId);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SiteInformation> getSiteInformationByDomainName(String domainName);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SiteInformation> getPageOfSiteInformationByDomainName(int startingRecNo, String domainName);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void persistData(SiteInformation siteInformation, boolean newSiteInformation);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SiteInformation> updateSiteInformationByName(Map<String, String> map);
}
