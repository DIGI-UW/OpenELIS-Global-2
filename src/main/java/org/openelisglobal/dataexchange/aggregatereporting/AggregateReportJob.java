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
package org.openelisglobal.dataexchange.aggregatereporting;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.openelisglobal.dataexchange.common.ITransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.openelisglobal.spring.util.SpringContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public class AggregateReportJob implements Job {
    private String LAB_INDICATOR_REPORT_ID;
    private ReportExternalExportService reportExternalExportService = SpringContext
            .getBean(ReportExternalExportService.class);
    private CronSchedulerService cronSchedulerService = SpringContext.getBean(CronSchedulerService.class);
    private SiteInformationService siteInfoService = SpringContext.getBean(SiteInformationService.class);
    private ReportQueueTypeService reportQueueTypeService = SpringContext.getBean(ReportQueueTypeService.class);

    public AggregateReportJob() {
        ReportQueueType reportType = reportQueueTypeService.getReportQueueTypeByName("labIndicator");

        if (reportType != null) {
            LAB_INDICATOR_REPORT_ID = reportType.getId();
        }
    }

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        LogEvent.logInfo("AggregateReportJob", "execute()",
                "Reporting triggered: " + DateUtil.getCurrentDateAsText("dd-MM-yyyy hh:mm"));

        updateRunTimestamp();

        List<ReportExternalExport> sendableReports = reportExternalExportService
                .getUnsentReportExports(LAB_INDICATOR_REPORT_ID);

        if (!sendableReports.isEmpty()) {
            SendingAggregateReportWrapper wrapper = new SendingAggregateReportWrapper();
            wrapper.setSiteId(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));

            SiteInformation authInfo = siteInfoService.getSiteInformationByName("testUsageAggregationUserName");
            wrapper.setUser(authInfo == null ? " " : authInfo.getValue());
            authInfo = siteInfoService.getSiteInformationByName("testUsageAggregationPassword");
            wrapper.setPassword(authInfo == null ? " " : authInfo.getValue());

            List<AggregateReportXmit> reportXmit = new ArrayList<>();

            for (ReportExternalExport report : sendableReports) {
                reportXmit.add(new AggregateReportXmit(report));
            }

            wrapper.setReports(reportXmit);

            String castorPropertyName = "AggregateReportingMapping";

            String url = ConfigurationProperties.getInstance().getPropertyValue(Property.testUsageReportingURL)
                    + "/IndicatorAggregation"; //

            boolean sendAsychronously = false;
            ResponseHandler responseHandler = (ResponseHandler) SpringContext.getBean("aggregateResponseHandler");
            responseHandler.setReports(sendableReports);
            responseHandler.setReAttemptTry(wrapper, castorPropertyName, url);

            // String xml = createXML(wrapper);
            new ReportTransmission().sendReport(wrapper, castorPropertyName, url, sendAsychronously, responseHandler);
            // new ReportTransmission().sendRawReport(xml, url, sendAsychronously,
            // responseHandler);

        }
    }

    /*
     * This is an alternative if castor is not behaving well. It was adding newline
     * chars to the data which was messing it up when it's displayed
     */
    /*
     * private String createXML(SendingAggregateReportWrapper wrapper) {
     * StringBuffer buffer = new StringBuffer();
     *
     * buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
     * buffer.append("<aggregate-reports>\n");
     * buffer.append("<version>1</version>\n"); buffer.append("<drowssap>");
     * buffer.append(wrapper.getPassword()); buffer.append("</drowssap>\n");
     * buffer.append(" <user>"); buffer.append(wrapper.getUser());
     * buffer.append("</user>\n"); buffer.append("<site-id>");
     * buffer.append(wrapper.getSiteId()); buffer.append("</site-id>\n"); for
     * (AggregateReportXmit report : wrapper.getReports()) { buffer.
     * append(" <reports xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"report\">\n"
     * ); buffer.append("<event-date>"); buffer.append(report.getEventDate());
     * buffer.append("</event-date>\n"); buffer.append("<data>");
     * buffer.append(report.getData().replace("\"", "&quot;").replace("'",
     * "&apos;")); buffer.append("</data>\n"); buffer.append("</reports>\n"); }
     * buffer.append("</aggregate-reports>");
     *
     * return buffer.toString(); }
     */
    private void updateRunTimestamp() {
        CronScheduler reportScheduler = cronSchedulerService.getCronScheduleByJobName("sendSiteIndicators");
        reportScheduler.setLastRun(DateUtil.getNowAsTimestamp());
        reportScheduler.setSysUserId("1");

        try {
            cronSchedulerService.update(reportScheduler);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
        }
    }

    @Service("aggregateResponseHandler")
    @Scope("prototype")
    class ResponseHandler implements ITransmissionResponseHandler {

        private static final long MAX_DELAY = 256; // Anything past this will be
        // a cumulative time of over
        // 8 hours
        private List<ReportExternalExport> sendableReports;
        private ReportExternalExportService reportExternalExportService = SpringContext
                .getBean(ReportExternalExportService.class);
        private long delayInMin = 5L;
        private SendingAggregateReportWrapper wrapper;
        private String castorPropertyName;
        private String url;
        private ResponseHandler instance = this;

        public void setReports(List<ReportExternalExport> sendableReports) {
            this.sendableReports = sendableReports;
        }

        public void setReAttemptTry(SendingAggregateReportWrapper wrapper, String castorPropertyName, String url) {
            this.wrapper = wrapper;
            this.castorPropertyName = castorPropertyName;
            this.url = url;
        }

        @Override
        @Transactional
        public void handleResponse(int httpReturnStatus, List<String> errors, String msg) {

            switch (httpReturnStatus) {
            case HttpServletResponse.SC_OK:
                handleSuccess();
                break;
            case HttpServletResponse.SC_UNAUTHORIZED:
                handleUnauthorized();
                retry();
                break;
            case HttpServletResponse.SC_NOT_FOUND:
                handleNotFound(errors);
                retry();
                break;
            case HttpServletResponse.SC_BAD_REQUEST:
                handleBadRequest(errors);
                break;
            default:
                handleUnknownFailure(errors);
                retry();
            }
        }

        private void handleSuccess() {

            try {
                for (ReportExternalExport report : sendableReports) {
                    report = reportExternalExportService.loadReport(report);
                    report.setSend(false);
                    report.setSentDate(DateUtil.getNowAsTimestamp());
                    report.setSysUserId("1");
                    reportExternalExportService.update(report);
                }

                SiteInformation sendInfo = siteInfoService.getSiteInformationByName("testUsageSendStatus");
                if (sendInfo != null) {
                    sendInfo.setValue(MessageUtil.getMessage("http.success"));
                    sendInfo.setSysUserId("1");
                    siteInfoService.update(sendInfo);
                }

            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
                throw e;
            }
        }

        private void handleUnknownFailure(List<String> errors) {
            for (String error : errors) {
                if (error.startsWith("Unable to connect")) {
                    writeSendStatus(MessageUtil.getMessage("http.site.unreachable"));
                    return;
                }
            }

            writeSendStatus(MessageUtil.getMessage("http.unknown.failure"));
        }

        private void handleUnauthorized() {
            writeSendStatus(MessageUtil.getMessage("http.unauthorized"));
        }

        private void handleNotFound(List<String> errors) {
            String error = errors.isEmpty() ? "" : errors.get(0);
            writeSendStatus(MessageUtil.getContextualMessage("http.notfound") + ": " + error);
        }

        private void handleBadRequest(List<String> errors) {
            String error = errors.isEmpty() ? "" : errors.get(0);
            writeSendStatus(MessageUtil.getContextualMessage("http.badrequest") + ": " + error);
        }

        private void retry() {
            delayInMin = delayInMin * 2L;
            if (delayInMin < MAX_DELAY) {
                // new Thread() {
                // @Override
                // public void run() {
                // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
                // "Aggregate Report: Will attempt to resend report in " +
                // delayInMin + " minutes.");
                // LogEvent.logInfo("AggregateReportJob", "retry()",
                // "Will attempt to resend report in " + delayInMin + "
                // minutes.");
                // try {
                // sleep(delayInMin * MILLI_SEC_PER_MIN);
                // } catch (InterruptedException e) {
                // LogEvent.logDebug(e);
                // }
                new ReportTransmission().sendReport(wrapper, castorPropertyName, url, false, instance);
                // }
                // }.start();
            } else {
                LogEvent.logInfo(this.getClass().getSimpleName(), "retry",
                        "Aggregate report: Giving up trying to connect");
            }
        }

        private void writeSendStatus(String status) {
            try {
                SiteInformation sendInfo = siteInfoService.getSiteInformationByName("testUsageSendStatus");
                if (sendInfo != null) {
                    sendInfo.setValue(status);
                    sendInfo.setSysUserId("1");
                    siteInfoService.update(sendInfo);
                }
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
