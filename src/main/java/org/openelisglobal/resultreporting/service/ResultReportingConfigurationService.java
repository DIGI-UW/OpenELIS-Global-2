package org.openelisglobal.resultreporting.service;

import java.util.List;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultReportingConfigurationService {

    // Result-reporting CONFIGURATION: writes site-information settings + cron
    // schedulers, a system-configuration concern (its collaborators
    // SiteInformationService.persistData / scheduler config are the
    // PRIV_SYSTEM_CONFIGURE surface), not result data modification.
    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void updateInformationAndSchedulers(List<SiteInformation> informationList, List<CronScheduler> scheduleList);
}
