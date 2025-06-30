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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.dataexchange.MalariaSurveilance;

import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.common.ITransmissionResponseHandler;
import org.openelisglobal.dataexchange.common.ReportTransmission;
import org.openelisglobal.dataexchange.common.ReportTransmission.HTTP_TYPE;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.scheduler.service.CronSchedulerService;
import org.openelisglobal.scheduler.valueholder.CronScheduler;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class MalariaSurveilanceJob implements Job {
    private static final String CRON_MALARIA_SCHEDULER = "sendMalariaSurviellanceReport";

    private static String MALARIA_TEST_ID;
    private static Set<String> RAPID_TEST_IDS;
    private static Map<String, String> MALARIA_DICTIONARY_ID_VALUE_MAP;
    private static Map<String, String> RAPID_DICTIONARY_ID_VALUE_MAP;
    private static List<String> MALARIA_TEST_NAMES;
    private static List<String> RAPID_TEST_NAMES;
    private static final String FINISHED_STATUS_ID = SpringContext.getBean(IStatusService.class)
            .getStatusID(AnalysisStatus.Finalized);

    private StringBuffer buffer;

    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    private CronSchedulerService cronSchedulerService = SpringContext.getBean(CronSchedulerService.class);
    private ResultService resultService = SpringContext.getBean(ResultService.class);
    private TestService testService = SpringContext.getBean(TestService.class);
    private TestResultService testResultService = SpringContext.getBean(TestResultService.class);
    private DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);

    public MalariaSurveilanceJob() {

        RAPID_TEST_IDS = new HashSet<>();
        MALARIA_DICTIONARY_ID_VALUE_MAP = new HashMap<>();
        RAPID_DICTIONARY_ID_VALUE_MAP = new HashMap<>();
        MALARIA_TEST_NAMES = new ArrayList<>();
        RAPID_TEST_NAMES = new ArrayList<>();

        // N.B. This should be discoverable from the DB, not hard coded by name
        MALARIA_TEST_NAMES.add("Recherche de plasmodiun - Especes(Sang)");
        RAPID_TEST_NAMES.add("Malaria Test Rapide(Serum)");
        RAPID_TEST_NAMES.add("Malaria Test Rapide(Plasma)");
        RAPID_TEST_NAMES.add("Malaria Test Rapide(Sang)");

        for (String name : MALARIA_TEST_NAMES) {
            Test test = testService.getTestByDescription(name);
            if (test != null && test.getId() != null) {
                MALARIA_TEST_ID = test.getId();
            }
        }

        for (String name : RAPID_TEST_NAMES) {
            addRapidTest(testService, name);
        }

        List<TestResult> malariaResults = testResultService.getActiveTestResultsByTest(MALARIA_TEST_ID);

        for (TestResult testResult : malariaResults) {
            Dictionary dictionary = dictionaryService.getDictionaryById(testResult.getValue());
            if (dictionary != null) {
                MALARIA_DICTIONARY_ID_VALUE_MAP.put(dictionary.getId(), dictionary.getDictEntry());
            }
        }

        for (String rapidTestId : RAPID_TEST_IDS) {
            List<TestResult> rapidResults = testResultService.getActiveTestResultsByTest(rapidTestId);

            for (TestResult testResult : rapidResults) {
                Dictionary dictionary = dictionaryService.getDictionaryById(testResult.getValue());
                if (dictionary != null && RAPID_DICTIONARY_ID_VALUE_MAP.get(dictionary.getId()) == null) {
                    RAPID_DICTIONARY_ID_VALUE_MAP.put(dictionary.getId(), dictionary.getDictEntry());
                }
            }
        }
    }

    private void addRapidTest(TestService testService, String description) {
        Test test = testService.getTestByDescription(description);
        if (test != null && test.getId() != null) {
            RAPID_TEST_IDS.add(test.getId());
        }
    }

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        LogEvent.logInfo(this.getClass().getSimpleName(), "execute",
                "MalariaSurveilance triggered: " + DateUtil.getCurrentDateAsText("dd-MM-yyyy hh:mm"));
        LogEvent.logInfo("MalariaSurveilance", "execute()",
                "Gathering triggered: " + DateUtil.getCurrentDateAsText("dd-MM-yyyy hh:mm"));

        boolean sendReport = true;
        Timestamp latestCollectionDate = getLatestCollectionDate();

        buffer = new StringBuffer();

        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<OpenElisServeillance>\n");
        buffer.append("<version>1</version>\n");
        buffer.append("<site name='");
        buffer.append(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        buffer.append("' code='");
        buffer.append(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        buffer.append("' />\n");
        buffer.append("<reports date='");
        buffer.append(DateUtil.getCurrentDateAsText());
        buffer.append("'>\n");

        Timestamp nowTimestamp = DateUtil.getNowAsTimestamp();
        if (latestCollectionDate == null) {
            for (int i = 120; i > 0; i--) {
                writeReportForDayPeriod(i);
            }
        } else {
            int missedDays = DateUtil.getDaysInPastForDate(new Date(latestCollectionDate.getTime()));
            if (missedDays > 0) {
                for (int i = missedDays; i > 0; i--) {
                    writeReportForDayPeriod(i);
                }
            } else {
                sendReport = false;
            }
        }

        buffer.append("</reports>\n");
        buffer.append("</OpenElisServeillance>\n");

        if (sendReport) {
            boolean sendAsychronously = false;
            String url = ConfigurationProperties.getInstance().getPropertyValue(Property.malariaSurveillanceReportURL);
            ResponseHandler responseHandler = new ResponseHandler();
            responseHandler.setRunTime(nowTimestamp);

            new ReportTransmission().sendRawReport(buffer.toString(), url, sendAsychronously, responseHandler,
                    HTTP_TYPE.POST);
        }
    }

    private void writeReportForDayPeriod(int daysAgo) {
        Timestamp dayOne = DateUtil.getTimestampAtMidnightForDaysAgo(daysAgo);
        Timestamp dayTwo = DateUtil.getTimestampAtMidnightForDaysAgo(daysAgo - 1);

        Map<String, Integer> malairaBucket = new HashMap<>();
        Map<String, Integer> rapidBucket = new HashMap<>();

        buffer.append("<report type='Malaria' startDate='");
        buffer.append(DateUtil.convertTimestampToStringDate(dayOne));
        buffer.append("' endDate='");
        buffer.append(DateUtil.convertTimestampToStringDate(dayOne));
        buffer.append("' >\n");

        fillMalariaBuckets(dayOne, dayTwo, MALARIA_TEST_NAMES, malairaBucket, MALARIA_DICTIONARY_ID_VALUE_MAP);
        fillMalariaBuckets(dayOne, dayTwo, RAPID_TEST_NAMES, rapidBucket, RAPID_DICTIONARY_ID_VALUE_MAP);

        writeMalariaResults(malairaBucket, "Recherche de plasmodiun - Especes", MALARIA_DICTIONARY_ID_VALUE_MAP);
        writeMalariaResults(rapidBucket, "Malaria Test Rapide", RAPID_DICTIONARY_ID_VALUE_MAP);
        buffer.append("</report>\n");
    }

    private void writeMalariaResults(Map<String, Integer> bucket, String name, Map<String, String> idValueMap) {
        buffer.append("<indicator name='");
        buffer.append(name);
        buffer.append("' >\n");
        for (String id : bucket.keySet()) {
            buffer.append("\t<result name='");
            buffer.append(idValueMap.get(id));
            buffer.append("' count='");
            buffer.append(bucket.get(id));
            buffer.append("' />\n");
        }
        buffer.append("</indicator>\n");
    }

    private void fillMalariaBuckets(Timestamp dayOne, Timestamp dayTwo, List<String> testNames,
            Map<String, Integer> bucket, Map<String, String> idValueMap) {

        java.sql.Date sqlDayOne = DateUtil.convertTimestampToSqlDate(dayOne);
        java.sql.Date sqlDayTwo = DateUtil.convertTimestampToSqlDate(dayTwo);
        List<Analysis> analysisList = analysisService.getAnalysisByTestDescriptionAndCompletedDateRange(testNames,
                sqlDayOne, sqlDayTwo);

        for (String key : idValueMap.keySet()) {
            bucket.put(key, 0);
        }

        List<Integer> analysisIdList = new ArrayList<>();
        for (Analysis analysis : analysisList) {
            if (FINISHED_STATUS_ID.equals(analysis.getStatusId())) {
                analysisIdList.add(Integer.parseInt(analysis.getId()));
            }
        }
        List<Result> results = resultService.getResultsForAnalysisIdList(analysisIdList);

        if (results != null) {
            for (Result result : results) {
                if (result.getValue() != null) {
                    Integer count = bucket.get(result.getValue());

                    if (count != null) {
                        count++;
                        bucket.put(result.getValue(), count);
                    }
                }
            }
        }
    }

    private Timestamp getLatestCollectionDate() {
        return cronSchedulerService.getCronScheduleByJobName(CRON_MALARIA_SCHEDULER).getLastRun();
    }

    class ResponseHandler implements ITransmissionResponseHandler {
        private Timestamp runTime;

        public void setRunTime(Timestamp runTime) {
            this.runTime = runTime;
        }

        @Override
        public void handleResponse(int httpReturnStatus, List<String> errors, String msg) {
            if (httpReturnStatus == HttpServletResponse.SC_OK) {
            }

            switch (httpReturnStatus) {
            case HttpServletResponse.SC_OK:
                setJobTimestamp(runTime);
                break;
            default:
                LogEvent.logInfo(this.getClass().getSimpleName(), "handleResponse", errors.toString());
            }
        }

        private void setJobTimestamp(Timestamp nowTimestamp) {
            CronScheduler gatherScheduler = cronSchedulerService.getCronScheduleByJobName(CRON_MALARIA_SCHEDULER);
            gatherScheduler.setLastRun(nowTimestamp);
            gatherScheduler.setSysUserId("1");

            try {
                cronSchedulerService.update(gatherScheduler);
            } catch (LIMSRuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }
}
