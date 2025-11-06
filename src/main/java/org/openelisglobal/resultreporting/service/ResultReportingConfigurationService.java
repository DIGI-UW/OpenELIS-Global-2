package org.openelisglobal.resultreporting.service;

import java.util.List;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

public interface ResultReportingConfigurationService {

    void updateInformationAndSchedulers(List<SiteInformation> informationList, List<CronScheduler> scheduleList);
}
