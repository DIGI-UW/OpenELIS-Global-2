package org.openelisglobal.resultreporting.service;

import java.util.List;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultReportingConfigurationServiceImpl implements ResultReportingConfigurationService {

    @Autowired
    private SiteInformationService siteInformationService;
    @Autowired
    private CronSchedulerService schedulerService;

    @Override
    @Transactional
    public void updateInformationAndSchedulers(List<SiteInformation> informationList,
            List<CronScheduler> scheduleList) {
        for (SiteInformation info : informationList) {
            siteInformationService.update(info);
        }

        for (CronScheduler scheduler : scheduleList) {
            schedulerService.update(scheduler);
        }

        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }
}
