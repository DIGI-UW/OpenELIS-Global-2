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
package org.openelisglobal.scheduler.independentthreads;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalExport;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportQueueType;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalExportService;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportQueueTypeService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestUsageBacklog {

    @Autowired
    private ReportExternalExportService reportExternalExportService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ReportQueueTypeService reportQueueTypeService;

    private static String TEST_UTALIZATION_ID;

    @PostConstruct
    public void initializeGlobalVariables() {
        ReportQueueType queueType = reportQueueTypeService.getReportQueueTypeByName("labIndicator");

        if (queueType != null) {
            TEST_UTALIZATION_ID = queueType.getId();
        }
    }

    // workaround to make this run once on startup.
    // If program is continuously running for 290,0000 millenia it will run again
    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void run() {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.testUsageReporting, "true")) {
            handleBacklog();
        }
    }

    private void handleBacklog() {
        LogEvent.logInfo("TestUsagebacklog", "handleBacklog",
                "Gathering triggered: " + DateUtil.getCurrentDateAsText("dd-MM-yyyy hh:mm"));

        Timestamp latestCollectionDate = getLatestCollectionDate();

        if (latestCollectionDate == null) {
            for (int i = 120; i >= 0; i--) {
                writeReportForDayPeriod(i);
            }
        } else {
            int daysInPast = DateUtil.getDaysInPastForDate(latestCollectionDate) - 1;
            for (int i = daysInPast; i >= 0; i--) {
                writeReportForDayPeriod(i);
            }
        }
    }

    private Timestamp getLatestCollectionDate() {
        ReportExternalExport report = reportExternalExportService.getLatestEventReportExport(TEST_UTALIZATION_ID);
        if (report != null) {
            return report.getEventDate();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void writeReportForDayPeriod(int daysAgo) {
        Timestamp dayOne = DateUtil.getTimestampAtMidnightForDaysAgo(daysAgo);
        Timestamp dayTwo = DateUtil.getTimestampAtMidnightForDaysAgo(daysAgo - 1);

        List<Analysis> analysisList = analysisService.getAnalysisCompleteInRange(dayOne, dayTwo);

        Map<String, Integer> testBucket = new HashMap<>();
        for (String key : TestServiceImpl.getMap(TestServiceImpl.Entity.TEST_AUGMENTED_NAME).keySet()) {
            testBucket.put(key, 0);
        }

        for (Analysis analysis : analysisList) {
            if ("Y".equals(analysis.getTest().getIsActive())) {
                String testId = analysis.getTest().getId();
                testBucket.put(testId, testBucket.get(testId).intValue() + 1);
            }
        }

        JSONObject json = new JSONObject();
        for (String id : testBucket.keySet()) {
            if (testBucket.get(id).intValue() > 0) {
                json.put(TestServiceImpl.getMap(TestServiceImpl.Entity.TEST_AUGMENTED_NAME).get(id),
                        testBucket.get(id));
            }
        }

        StringWriter out = new StringWriter();
        try {
            json.writeJSONString(out);
        } catch (IOException e) {
            LogEvent.logDebug(e);
        }
        String jsonText = out.toString().replace("\n", "");

        ReportExternalExport report = new ReportExternalExport();
        report.setEventDate(dayOne);
        report.setTypeId(TEST_UTALIZATION_ID);

        report = reportExternalExportService.getReportByEventDateAndType(report);
        report.setSend(true);
        report.setData(jsonText); // buffer.toString());
        report.setCollectionDate(DateUtil.getNowAsTimestamp());
        report.setSysUserId("1");

        try {
            if (report.getId() == null) {
                reportExternalExportService.insert(report);
            } else {
                reportExternalExportService.insert(report);
            }

        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }
}
