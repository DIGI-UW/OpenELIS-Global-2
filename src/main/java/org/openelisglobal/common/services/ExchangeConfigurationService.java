/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.dataexchange.connectionTest.ConnectionTest;
import org.openelisglobal.dataexchange.resultreporting.beans.ReportingConfiguration;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
@DependsOn({ "springContext" })
public class ExchangeConfigurationService {

    private static String RESULT_REPORT_TYPE_ID;
    private static String MALARIA_CASE_TYPE_ID;

    public enum ConfigurationDomain {
        REPORT("resultReporting");

        private String domainName;

        ConfigurationDomain(String siteDomain) {
            domainName = siteDomain;
        }

        public String getSiteDomain() {
            return domainName;
        }
    }

    public enum ExchangeType {
        RESULT_REPORT("Result Reporting", "resultReport", true, RESULT_REPORT_TYPE_ID),
        MALARIA_SURVEILLANCE("Malaria Surveillance", "malariaSurvaeillance", false, null),
        MALARIA_CASE("Malaria Case Report", "malariaCase", true, MALARIA_CASE_TYPE_ID);

        private String title;
        private boolean showbacklog = false;
        private String backlogId = null;
        private String urlTestToken;

        private ExchangeType(String title, String urlTestToken, boolean showbacklog, String backlogId) {
            this.title = title;
            this.showbacklog = showbacklog;
            this.backlogId = backlogId;
            this.urlTestToken = urlTestToken;
        }

        public String getUrlTestToken() {
            return urlTestToken;
        }

        public boolean isShowbacklog() {
            return showbacklog;
        }

        public String getTitle() {
            return title;
        }

        public String getBacklogId() {
            return backlogId;
        }
    }

    private static ReportQueueTypeService reportQueueTypeService = SpringContext.getBean(ReportQueueTypeService.class);
    private SiteInformationService siteInformationService = SpringContext.getBean(SiteInformationService.class);
    private ReportExternalExportService reportExternalExportService = SpringContext
            .getBean(ReportExternalExportService.class);

    private ConfigurationDomain domain;
    private ExchangeType exchangeType;

    private static Map<String, ExchangeType> dbNameToExchangeTypeMap = new HashMap<>();
    private static Map<String, ExchangeType> testTokenToExchangeTypeMap = new HashMap<>();
    private static Map<String, ConfigurationDomain> testTokenToDomainMap = new HashMap<>();

    static {
        RESULT_REPORT_TYPE_ID = reportQueueTypeService.getReportQueueTypeByName("Results").getId();
        MALARIA_CASE_TYPE_ID = reportQueueTypeService.getReportQueueTypeByName("malariaCase").getId();

        dbNameToExchangeTypeMap.put("resultReporting", ExchangeType.RESULT_REPORT);
        dbNameToExchangeTypeMap.put("malariaSurReport", ExchangeType.MALARIA_SURVEILLANCE);
        dbNameToExchangeTypeMap.put("malariaCaseReport", ExchangeType.MALARIA_CASE);

        if (ExchangeType.RESULT_REPORT.getUrlTestToken() != null) {
            String token = ExchangeType.RESULT_REPORT.getUrlTestToken();
            testTokenToExchangeTypeMap.put(token, ExchangeType.RESULT_REPORT);
            testTokenToDomainMap.put(token, ConfigurationDomain.REPORT);
        }
        if (ExchangeType.MALARIA_CASE.getUrlTestToken() != null) {
            String token = ExchangeType.MALARIA_CASE.getUrlTestToken();
            testTokenToExchangeTypeMap.put(token, ExchangeType.MALARIA_CASE);
            testTokenToDomainMap.put(token, ConfigurationDomain.REPORT);
        }
        if (ExchangeType.MALARIA_SURVEILLANCE.getUrlTestToken() != null) {
            String token = ExchangeType.MALARIA_SURVEILLANCE.getUrlTestToken();
            testTokenToExchangeTypeMap.put(token, ExchangeType.MALARIA_SURVEILLANCE);
            testTokenToDomainMap.put(token, ConfigurationDomain.REPORT);
        }
    }

    public ExchangeConfigurationService() {
    }

    public ExchangeConfigurationService(ConfigurationDomain domain) {
        this.domain = domain;
    }

    public ExchangeConfigurationService(String urlTestToken) {
        domain = testTokenToDomainMap.get(urlTestToken);
        exchangeType = testTokenToExchangeTypeMap.get(urlTestToken);
    }

    public List<ReportingConfiguration> getConfigurations() {

        List<SiteInformation> informationList = siteInformationService.getPageOfSiteInformationByDomainName(1,
                domain.getSiteDomain());

        Collections.sort(informationList, new Comparator<SiteInformation>() {
            @Override
            public int compare(SiteInformation o1, SiteInformation o2) {
                return o1.getGroup() - o2.getGroup();
            }
        });

        int group = informationList.get(0).getGroup();
        List<ReportingConfiguration> reports = new ArrayList<>();
        ReportingConfiguration configuration = new ReportingConfiguration();
        reports.add(configuration);

        for (SiteInformation information : informationList) {
            if (group != information.getGroup()) {
                group = information.getGroup();
                configuration = new ReportingConfiguration();
                reports.add(configuration);
            }
            if ("url".equals(information.getTag())) {
                configuration.setUrl(information.getValue());
                configuration.setUrlId(information.getId());
            } else if ("enable".equals(information.getTag())) {
                configuration.setEnabled(getReportingEnabled(information.getValue()));
                configuration.setEnabledId(information.getId());

                ExchangeType exchangeType = dbNameToExchangeTypeMap.get(information.getName());
                if (exchangeType != null) {
                    // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
                    // information.getName());
                    configuration.setTitle(exchangeType.getTitle());
                    configuration.setConnectionTestIdentifier(exchangeType.getUrlTestToken());
                    if (exchangeType.isShowbacklog()) {
                        configuration.setShowBacklog(true);
                        configuration.setBacklogSize(getReportingBacklogSize(exchangeType.getBacklogId()));
                    }
                }
            }

            if (information.getSchedule() != null) {
                configuration.setIsScheduled(true);
                configuration.setSchedulerId(information.getSchedule().getId());

                String cronString = information.getSchedule().getCronStatement();

                if (!"never".equals(cronString)) {
                    String[] cronParts = cronString.split(" ");
                    int minutes = Integer.parseInt(cronParts[1]);
                    configuration.setScheduleHours(cronParts[2]);
                    configuration.setScheduleMin(String.valueOf(minutes / 10 * 10));
                }
            }
        }

        return reports;
    }

    private String getReportingBacklogSize(String reportType) {
        int size = reportExternalExportService.getUnsentReportExports(reportType).size();
        return String.valueOf(size);
    }

    private String getReportingEnabled(String value) {
        return ("true".equals(value) || "enable".equals(value)) ? "enable" : "disable";
    }

    public String testConnection(String url) {
        if (GenericValidator.isBlankOrNull(url)) {
            return MessageUtil.getMessage("connection.test.error.missingURL");
        }

        ConnectionTest connectionTest = new ConnectionTest();
        return connectionTest.testURL(url);
    }
}
