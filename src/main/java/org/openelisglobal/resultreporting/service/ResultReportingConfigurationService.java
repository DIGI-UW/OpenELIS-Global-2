package org.openelisglobal.resultreporting.service;

import java.util.List;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResultReportingConfigurationService {

    @PreAuthorize("hasAuthority('PRIV_RESULT_MODIFY')")
    void updateInformationAndSchedulers(List<SiteInformation> informationList, List<CronScheduler> scheduleList);
}
