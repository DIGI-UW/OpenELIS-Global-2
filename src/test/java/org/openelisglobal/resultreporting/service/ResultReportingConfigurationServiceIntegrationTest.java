package org.openelisglobal.resultreporting.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultReportingConfigurationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String RESULT_REPORTING_ENABLED_ID = "9025";
    private static final String RESULT_REPORTING_URL_ID = "9026";
    private static final String MALARIA_SCHEDULER_ID = "9003";

    @Autowired
    private ResultReportingConfigurationService resultReportingConfigurationService;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private CronSchedulerService cronSchedulerService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result-reporting-configuration.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    @Test
    public void updateInformationAndSchedulers_updatesSiteInformationAndScheduler() {
        SiteInformation enabled = siteInformationService.get(RESULT_REPORTING_ENABLED_ID);
        enabled.setValue("true");
        enabled.setSysUserId(TEST_SYS_USER_ID);

        SiteInformation url = siteInformationService.get(RESULT_REPORTING_URL_ID);
        url.setValue("https://example.org/results");
        url.setSysUserId(TEST_SYS_USER_ID);

        CronScheduler scheduler = cronSchedulerService.get(MALARIA_SCHEDULER_ID);
        scheduler.setCronStatement("0 30 14 ? * *");
        scheduler.setActive(true);
        scheduler.setSysUserId(TEST_SYS_USER_ID);

        resultReportingConfigurationService.updateInformationAndSchedulers(List.of(enabled, url), List.of(scheduler));

        SiteInformation updatedEnabled = siteInformationService.get(RESULT_REPORTING_ENABLED_ID);
        SiteInformation updatedUrl = siteInformationService.get(RESULT_REPORTING_URL_ID);
        CronScheduler updatedScheduler = cronSchedulerService.get(MALARIA_SCHEDULER_ID);

        assertEquals("true", updatedEnabled.getValue());
        assertEquals("https://example.org/results", updatedUrl.getValue());
        assertEquals("0 30 14 ? * *", updatedScheduler.getCronStatement());
        assertTrue(updatedScheduler.getActive());
    }

    @Test
    public void updateInformationAndSchedulers_withEmptySchedulerList_updatesSiteInformationOnly() {
        SiteInformation enabled = siteInformationService.get(RESULT_REPORTING_ENABLED_ID);
        enabled.setValue("true");
        enabled.setSysUserId(TEST_SYS_USER_ID);

        SiteInformation url = siteInformationService.get(RESULT_REPORTING_URL_ID);
        url.setValue("https://lab.example.org/report");
        url.setSysUserId(TEST_SYS_USER_ID);

        CronScheduler schedulerBefore = cronSchedulerService.get(MALARIA_SCHEDULER_ID);

        resultReportingConfigurationService.updateInformationAndSchedulers(List.of(enabled, url),
                Collections.emptyList());

        assertEquals("true", siteInformationService.get(RESULT_REPORTING_ENABLED_ID).getValue());
        assertEquals("https://lab.example.org/report", siteInformationService.get(RESULT_REPORTING_URL_ID).getValue());

        CronScheduler schedulerAfter = cronSchedulerService.get(MALARIA_SCHEDULER_ID);
        assertEquals(schedulerBefore.getCronStatement(), schedulerAfter.getCronStatement());
        assertEquals(schedulerBefore.getActive(), schedulerAfter.getActive());
    }

    @Test
    public void updateInformationAndSchedulers_reloadsConfigurationProperties() {
        SiteInformation enabled = siteInformationService.get(RESULT_REPORTING_ENABLED_ID);
        enabled.setValue("true");
        enabled.setSysUserId(TEST_SYS_USER_ID);

        SiteInformation url = siteInformationService.get(RESULT_REPORTING_URL_ID);
        url.setValue("https://example.org/results");
        url.setSysUserId(TEST_SYS_USER_ID);

        resultReportingConfigurationService.updateInformationAndSchedulers(List.of(enabled, url),
                Collections.emptyList());

        assertEquals("true", ConfigurationProperties.getInstance().getPropertyValue(Property.reportResults));
        assertEquals("https://example.org/results",
                ConfigurationProperties.getInstance().getPropertyValue(Property.resultReportingURL));
    }
}
