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

import java.io.ByteArrayOutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ErrorMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.IPCIRealisationTest;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

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

    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9);

    @Override
    protected byte[] renderReport() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out);
        ReportHeaderPdf.add(document, "Rapport sur la realisation des tests", ReportHeaderPdf.siteNameLines());
        Paragraph period = new Paragraph(
                lowerDateRange + " - " + upperDateRange + "    Date du rapport : " + DateUtil.getCurrentDateAsText(),
                CELL_FONT);
        period.setSpacingBefore(6);
        period.setSpacingAfter(8);
        document.add(period);

        PdfPTable table = new PdfPTable(new float[] { 3, 2, 2, 2 });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, "Test", "Demande", "Effectue", "Non effectue");
        int[] grand = new int[3];
        int start = 0;
        while (start < reportItems.size()) {
            String section = reportItems.get(start).getSectionName();
            PdfPCell sectionCell = new PdfPCell(new Phrase(section, HEADER_FONT));
            sectionCell.setColspan(4);
            table.addCell(sectionCell);
            int[] sectionTotals = new int[3];
            int end = start;
            while (end < reportItems.size() && Objects.equals(reportItems.get(end).getSectionName(), section)) {
                IPCIRealisationTest item = reportItems.get(end);
                int[] counts = { item.getRequired(), item.getPerformed(), item.getNoPerformed() };
                table.addCell(new Phrase(item.getTestName(), CELL_FONT));
                for (int i = 0; i < counts.length; i++) {
                    table.addCell(new Phrase(String.valueOf(counts[i]), CELL_FONT));
                    sectionTotals[i] += counts[i];
                    grand[i] += counts[i];
                }
                end++;
            }
            addTotalRow(table, "Total", sectionTotals);
            start = end;
        }
        addTotalRow(table, "Totaux", grand);
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private static void addTotalRow(PdfPTable table, String label, int[] totals) {
        table.addCell(new Phrase(label, HEADER_FONT));
        for (int total : totals) {
            table.addCell(new Phrase(String.valueOf(total), HEADER_FONT));
        }
    }

    private class TestBucket {
        public String testName = "";
        public String testSection = "";
        public int notStartedCount = 0;
        public int inProgressCount = 0;
        public int finishedCount = 0;
    }
}
