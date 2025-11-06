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
package org.openelisglobal.reports.action.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.HaitiAggregateReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;

/**
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * <p>
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * <p>
 * The Original Code is OpenELIS code.
 *
 * <p>
 * Copyright (C) CIRG, University of Washington, Seattle WA. All Rights
 * Reserved.
 */
public abstract class IndicatorAllTest extends IndicatorReport implements IReportCreator, IReportParameterSetter {

    private List<HaitiAggregateReportData> reportItems;
    private Map<String, TestBucket> testNameToBucketList;
    private Map<String, TestBucket> concatSection_TestToBucketMap;
    private List<TestBucket> testBucketList;

    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private TestService testService = SpringContext.getBean(TestService.class);
    private static final String USER_TEST_SECTION_ID;

    static {
        USER_TEST_SECTION_ID = SpringContext.getBean(TestSectionService.class).getTestSectionByName("user").getId();
    }

    @Override
    protected String reportFileName() {
        return "LabAggregate";
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        setDateRange(form);

        createReportParameters();

        setTestMapForAllTests();

        setAnalysisForDateRange();

        mergeLists();

        setTestAggregates();
    }

    private void setTestMapForAllTests() {
        testNameToBucketList = new HashMap<>();
        concatSection_TestToBucketMap = new HashMap<>();
        testBucketList = new ArrayList<>();

        List<Test> testList = testService.getAllActiveTests(false);

        for (Test test : testList) {

            TestBucket bucket = new TestBucket();

            bucket.testName = TestServiceImpl.getUserLocalizedReportingTestName(test);
            bucket.testSort = Integer.parseInt(test.getSortOrder());
            bucket.testSection = test.getTestSection().getLocalizedName();
            bucket.sectionSort = test.getTestSection().getSortOrderInt();

            testNameToBucketList.put(TestServiceImpl.getUserLocalizedReportingTestName(test), bucket);
            testBucketList.add(bucket);
        }
    }

    private void setAnalysisForDateRange() {
        HashMap<String, ArrayList<Analysis>> sampleToPanelAnalysisMap = new HashMap<>();
        AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
        List<Analysis> rawAnalysisList = analysisService.getAnalysisStartedOrCompletedInDateRange(lowDate, highDate);
        ArrayList<Analysis> analysisList = new ArrayList<>();

        // group analysis w/ panels by samples (sampleItem)
        for (Analysis analysis : rawAnalysisList) {
            extractAnalysisInPanels(sampleToPanelAnalysisMap, analysisList, analysis);
        }

        // for each sample we will break it down into panels with list of analysis
        for (String sampleId : sampleToPanelAnalysisMap.keySet()) {
            HashMap<String, ArrayList<Analysis>> panelIdToAnalysisMap = new HashMap<>();
            for (Analysis sampleAnalysis : sampleToPanelAnalysisMap.get(sampleId)) {
                ArrayList<Analysis> panelAnalysisList = panelIdToAnalysisMap.get(sampleAnalysis.getPanel().getId());
                if (panelAnalysisList == null) {
                    panelAnalysisList = new ArrayList<>();
                    panelIdToAnalysisMap.put(sampleAnalysis.getPanel().getId(), panelAnalysisList);
                }
                panelAnalysisList.add(sampleAnalysis);
            }

            // we will now go through each panel and either throw out all analysis if any
            // one is canceled
            // or create the correct analysis thingy
            for (String panelId : panelIdToAnalysisMap.keySet()) {
                Analysis templateAnalysis = panelIdToAnalysisMap.get(panelId).get(0);
                String panelName = templateAnalysis.getPanel().getLocalizedName();

                boolean canceled = false;
                boolean notStarted = false;
                boolean finished = false;
                boolean inProgress = false;
                for (Analysis panelAnalysis : panelIdToAnalysisMap.get(panelId)) {
                    if (SpringContext.getBean(IStatusService.class).matches(panelAnalysis.getStatusId(),
                            AnalysisStatus.Canceled)) {
                        canceled = true;
                        break;
                    } else if (SpringContext.getBean(IStatusService.class).matches(panelAnalysis.getStatusId(),
                            AnalysisStatus.NotStarted)) {
                        notStarted = true;
                    } else if (SpringContext.getBean(IStatusService.class).matches(panelAnalysis.getStatusId(),
                            AnalysisStatus.Finalized)) {
                        finished = true;
                    } else {
                        inProgress = true;
                    }
                }

                if (canceled) {
                    // all analysis are treated as being out of the panel
                    for (Analysis analysis : panelIdToAnalysisMap.get(panelId)) {
                        analysisList.add(analysis);
                    }
                } else {
                    // we will create a fake analysis and add it to the analysisList
                    // and make sure the panel name is in the bucket list

                    String status;
                    if (inProgress || (notStarted && finished)) {
                        status = SpringContext.getBean(IStatusService.class)
                                .getStatusID(AnalysisStatus.TechnicalAcceptance);
                    } else if (notStarted) {
                        status = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
                    } else {
                        status = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
                    }

                    Analysis proxyAnalysis = getProxyAnalysis(templateAnalysis, panelName, status);
                    analysisList.add(proxyAnalysis);

                    createAndAddBucketIfNeeded(panelName, proxyAnalysis);
                }
            }
        }
        // go through map and both classify panels and return sets with any canceled
        // analysis
        for (Analysis analysis : analysisList) {
            addStatusToBuckets(analysis);
        }
    }

    private void extractAnalysisInPanels(HashMap<String, ArrayList<Analysis>> sampleToPanalAnalysisMap,
            ArrayList<Analysis> analysisList, Analysis analysis) {
        if (analysis.getPanel() == null) {
            analysisList.add(analysis);
        } else {
            ArrayList<Analysis> itemAnalysisList = sampleToPanalAnalysisMap.get(analysis.getSampleItem().getId());
            if (itemAnalysisList == null) {
                itemAnalysisList = new ArrayList<>();
                sampleToPanalAnalysisMap.put(analysis.getSampleItem().getId(), itemAnalysisList);
            }
            itemAnalysisList.add(analysis);
        }
    }

    private void addStatusToBuckets(Analysis analysis) {
        Test test = analysis.getTest();

        if (test != null) {
            TestBucket testBucket;
            // N.B. We need to look at the test->test section because the analysis test
            // section reflects the user selection for the test section
            // that entry will not be in the test to test section map
            if (USER_TEST_SECTION_ID.equals(analysis.getTest().getTestSection().getId())) {
                String concatedName = analysis.getTestSection().getLocalizedName()
                        + TestServiceImpl.getUserLocalizedTestName(analysis.getTest());
                testBucket = concatSection_TestToBucketMap.get(concatedName);
                if (testBucket == null) {
                    testBucket = new TestBucket();
                    testBucket.testName = TestServiceImpl.getUserLocalizedReportingTestName(test);
                    testBucket.testSort = Integer.parseInt(test.getSortOrder());
                    testBucket.testSection = analysis.getTestSection().getLocalizedName();
                    testBucket.sectionSort = analysis.getTestSection().getSortOrderInt();
                    concatSection_TestToBucketMap.put(concatedName, testBucket);
                }
            } else if (test.getLocalizedTestName() == null) {
                testBucket = testNameToBucketList.get(test.getLocalizedName());
            } else {
                testBucket = testNameToBucketList.get(TestServiceImpl.getUserLocalizedReportingTestName(test));
            }

            if (testBucket != null) {
                if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.NotStarted)) {
                    testBucket.notStartedCount++;
                } else if (inProgress(analysis)) {
                    testBucket.inProgressCount++;
                } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.Finalized)) {
                    testBucket.finishedCount++;
                }
            }
        }
    }

    private void createAndAddBucketIfNeeded(String panelName, Analysis proxyAnalysis) {
        TestBucket panelBucket = testNameToBucketList.get(panelName);
        if (panelBucket == null) {
            panelBucket = new TestBucket();
            panelBucket.testName = panelName;
            panelBucket.testSort = -1;
            panelBucket.testSection = proxyAnalysis.getTestSection().getLocalizedName();
            panelBucket.sectionSort = proxyAnalysis.getTestSection().getSortOrderInt();
            testNameToBucketList.put(panelName, panelBucket);
            testBucketList.add(panelBucket);
        }
    }

    private Analysis getProxyAnalysis(Analysis templateAnalysis, String panelName, String status) {
        Analysis proxyAnalysis = new Analysis();
        TestSection proxyTestSection = new TestSection();
        Test proxyTest = new Test();

        proxyAnalysis.setStatusId(status);
        proxyAnalysis.setTest(proxyTest);
        proxyAnalysis.setTestSection(proxyTestSection);

        proxyTestSection.setTestSectionName(templateAnalysis.getTestSection().getLocalizedName());
        proxyTestSection.setSortOrderInt(templateAnalysis.getTestSection().getSortOrderInt());
        proxyTestSection.setId(templateAnalysis.getTestSection().getId());

        proxyTest.setTestSection(proxyTestSection);
        // proxyTest.setTestName(panelName);

        return proxyAnalysis;
    }

    private boolean inProgress(Analysis analysis) {
        return SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                AnalysisStatus.TechnicalAcceptance)
                || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.TechnicalRejected)
                || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                        AnalysisStatus.BiologistRejected);
    }

    private void mergeLists() {
        Map<String, TestSection> testSectionMap = getTestSectionNameMap();

        // get rid of empty buckets, make sorting faster
        List<TestBucket> fullBucketList = new ArrayList<>();
        for (TestBucket bucket : testBucketList) {
            if ((bucket.finishedCount + bucket.notStartedCount + bucket.inProgressCount) > 0) {
                fullBucketList.add(bucket);
                testSectionMap.remove(bucket.testSection);
            }
        }

        testBucketList = fullBucketList;

        addEmptySectionsToBucketList(testSectionMap, testBucketList);

        for (TestBucket bucket : concatSection_TestToBucketMap.values()) {
            testBucketList.add(bucket);
        }

        Collections.sort(testBucketList, new Comparator<TestBucket>() {
            @Override
            public int compare(TestBucket o1, TestBucket o2) {
                int order = o1.sectionSort - o2.sectionSort;

                if (order == 0) {
                    order = o1.testSort - o2.testSort;
                }

                return order;
            }
        });
    }

    private void addEmptySectionsToBucketList(Map<String, TestSection> testSectionMap, List<TestBucket> bucketList) {
        for (String sectionName : testSectionMap.keySet()) {
            TestSection section = testSectionMap.get(sectionName);
            TestBucket testBucket = new TestBucket();
            testBucket.sectionSort = section.getSortOrderInt();
            testBucket.testSection = sectionName;
            testBucket.testSort = 0;
            testBucket.testName = null;
            bucketList.add(testBucket);
        }
    }

    private Map<String, TestSection> getTestSectionNameMap() {
        List<TestSection> allTestSections = testSectionService.getAllActiveTestSections();
        Map<String, TestSection> testSectionMap = new HashMap<>();
        for (TestSection section : allTestSections) {
            testSectionMap.put(section.getLocalizedName(), section);
        }
        return testSectionMap;
    }

    @Override
    protected String getNameForReportRequest() {
        return "";
        // return MessageUtil.getMessage("openreports.all.tests.aggregate");
    }

    private void setTestAggregates() {
        reportItems = new ArrayList<>();

        for (TestBucket bucket : testBucketList) {
            HaitiAggregateReportData data = new HaitiAggregateReportData();

            data.setFinished(bucket.finishedCount);
            data.setNotStarted(bucket.notStartedCount);
            data.setInProgress(bucket.inProgressCount);
            data.setTestName(bucket.testName);
            data.setSectionName(bucket.testSection);

            reportItems.add(data);
        }
    }

    private class TestBucket {
        public String testName = "";
        public int testSort = 0;
        public String testSection = "";
        public int sectionSort = 0;
        public int notStartedCount = 0;
        public int inProgressCount = 0;
        public int finishedCount = 0;
    }

    @Override
    protected String getNameForReport() {
        return MessageUtil.getContextualMessage("openreports.all.tests.aggregate");
    }
}
