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

/**
 * This file is the result of the Capstone project five for the Cote d'Ivoire OpenElis software
 * developer course made by Kone Constant
 */
package org.openelisglobal.reports.action.implementation;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.IPCIRealisationTest;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

public class IPCIRealisationReport extends Report {

    protected List<IPCIRealisationTest> reportItems;

    protected String lowerDateRange;
    protected String upperDateRange;
    protected Date lowDate;
    protected Date highDate;

    private HashMap<String, TestBucket> testIdToBucketList;

    private HashMap<String, TestBucket> concatSection_TestToBucketMap;

    private ArrayList<TestBucket> testBucketList;

    private static final String NOT_STARTED_STATUS_ID;
    private static final String FINALIZED_STATUS_ID;
    private static final String TECH_ACCEPT_ID;
    private static final String TECH_REJECT_ID;
    private static final String BIOLOGIST_REJECT_ID;
    private static final String USER_TEST_SECTION_ID;

    private static TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private TestService testService = SpringContext.getBean(TestService.class);
    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    static {
        NOT_STARTED_STATUS_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        FINALIZED_STATUS_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
        TECH_ACCEPT_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
        TECH_REJECT_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalRejected);
        BIOLOGIST_REJECT_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.BiologistRejected);
        USER_TEST_SECTION_ID = testSectionService.getTestSectionByName("user").getId();
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        errorFound = false;

        lowerDateRange = form.getLowerDateRange();
        upperDateRange = form.getUpperDateRange();

        if (GenericValidator.isBlankOrNull(lowerDateRange)) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.noPrintableItems"));
            errorMsgs.add(msgs);
        }

        if (GenericValidator.isBlankOrNull(upperDateRange)) {
            upperDateRange = lowerDateRange;
        }

        try {
            lowDate = DateUtil.convertStringDateToSqlDate(lowerDateRange);
            highDate = DateUtil.convertStringDateToSqlDate(upperDateRange);
        } catch (LIMSRuntimeException e) {
            errorFound = true;
            ErrorMessages msgs = new ErrorMessages();
            msgs.setMsgLine1(MessageUtil.getMessage("report.error.message.date.format"));
            errorMsgs.add(msgs);
        }

        createReportParameters();

        initializeReportItems();

        setTestMapForAllTests();

        setAnalysisForDateRange();

        setTestAggregates();
    }

    protected void initializeReportItems() {
        reportItems = new ArrayList<>();
    }

    private void setTestMapForAllTests() {
        testIdToBucketList = new HashMap<>();
        concatSection_TestToBucketMap = new HashMap<>();
        testBucketList = new ArrayList<>();

        List<Test> testList = testService.getAllActiveTests(false);

        for (Test test : testList) {
            TestBucket bucket = new TestBucket();

            bucket.testName = TestServiceImpl.getUserLocalizedTestName(test);
            bucket.testSection = test.getTestSection().getLocalizedName();

            testIdToBucketList.put(test.getId(), bucket);
            testBucketList.add(bucket);
        }
    }

    private void setTestAggregates() {
        reportItems = new ArrayList<>();
        for (TestBucket bucket : testBucketList) {
            if ((bucket.finishedCount + bucket.notStartedCount + bucket.inProgressCount) > 0) {

                IPCIRealisationTest data = new IPCIRealisationTest();

                data.setPerformed(bucket.finishedCount);
                data.setRequired(bucket.notStartedCount + bucket.inProgressCount + bucket.finishedCount);

                data.setTestName(bucket.testName);
                data.setSectionName(bucket.testSection);
                data.setNoPerformed(data.getRequired() - data.getPerformed());
                reportItems.add(data);
            }
        }
    }

    private void setAnalysisForDateRange() {
        List<Analysis> analysisList = analysisService.getAnalysisStartedOrCompletedInDateRange(lowDate, highDate);

        for (Analysis analysis : analysisList) {
            Test test = analysis.getTest();

            if (test != null) {
                TestBucket testBucket = null;
                if (USER_TEST_SECTION_ID.equals(analysis.getTestSection().getId())) {
                    String concatedName = analysis.getTestSection().getLocalizedName()
                            + TestServiceImpl.getUserLocalizedTestName(analysis.getTest());
                    testBucket = concatSection_TestToBucketMap.get(concatedName);
                    if (testBucket == null) {
                        testBucket = new TestBucket();
                        testBucket.testName = TestServiceImpl.getUserLocalizedReportingTestName(test);
                        testBucket.testSection = analysis.getTestSection().getLocalizedName();
                        concatSection_TestToBucketMap.put(concatedName, testBucket);
                    }
                } else {
                    testBucket = testIdToBucketList.get(test.getId());
                }

                if (testBucket != null) {
                    if (NOT_STARTED_STATUS_ID.equals(analysis.getStatusId())) {
                        testBucket.notStartedCount++;
                    } else if (inProgress(analysis)) {
                        testBucket.inProgressCount++;
                    } else if (FINALIZED_STATUS_ID.equals(analysis.getStatusId())) {
                        testBucket.finishedCount++;
                    }
                }
            }
        }
    }

    private boolean inProgress(Analysis analysis) {
        return TECH_ACCEPT_ID.equals(analysis.getStatusId()) || TECH_REJECT_ID.equals(analysis.getStatusId())
                || BIOLOGIST_REJECT_ID.equals(analysis.getStatusId());
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("startDate", lowerDateRange);
        reportParameters.put("stopDate", upperDateRange);
        reportParameters.put("date_debut", lowerDateRange);
        reportParameters.put("date_fin", upperDateRange);
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected String reportFileName() {

        return "IPCIRealisationTest";
    }

    private class TestBucket {
        public String testName = "";
        public String testSection = "";
        public int notStartedCount = 0;
        public int inProgressCount = 0;
        public int finishedCount = 0;
    }
}
