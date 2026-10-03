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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ValidationBacklogData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;

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
public class ValidationBacklogReport extends Report {

    private List<ValidationBacklogData> reportItems;
    private Map<String, TestBucket> sectionIdToBucketList;
    private List<TestBucket> sectionBucketList;
    private String TECH_ACCEPT_ID;
    private String USER_SELECT_SECTION_ID;

    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

    public ValidationBacklogReport() {
        TECH_ACCEPT_ID = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.TechnicalAcceptance);
        TestSection testSection = testSectionService.getTestSectionByName("user");
        if (testSection != null) {
            USER_SELECT_SECTION_ID = testSection.getId();
        }
    }

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font META_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 10);

    @Override
    protected byte[] renderReport() {
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        List<String> metaLines = new ArrayList<>();
        String siteName = config.getPropertyValue(Property.SiteName);
        if (!GenericValidator.isBlankOrNull(siteName)) {
            metaLines.add(siteName);
        }
        String directorName = config.getPropertyValue(Property.labDirectorName);
        if (!GenericValidator.isBlankOrNull(directorName)) {
            metaLines.add(MessageUtil.getMessage("report.labManager") + " " + directorName);
        }
        metaLines.add(DateUtil.getCurrentDateAsText() + " " + DateUtil.getCurrentTimeAsText());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 48);
        PdfExportSupport.openWithPageNumbers(document, out, "report.label.page");
        PdfExportSupport.addHeading(document, MessageUtil.getMessage("banner.menu.report.validation.backlog"),
                TITLE_FONT, META_FONT, metaLines.stream().map(line -> line + "\n").toArray(String[]::new));
        document.add(new Phrase("\n", META_FONT));

        PdfPTable table = new PdfPTable(new float[] { 3, 1 });
        table.setWidthPercentage(60);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 4, MessageUtil.getMessage("report.testSection"),
                MessageUtil.getMessage("report.total"));
        for (ValidationBacklogData item : reportItems) {
            table.addCell(new Phrase(item.getTestSection(), CELL_FONT));
            table.addCell(new Phrase(item.getCount(), CELL_FONT));
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();

        createReportParameters();
        setMapForAllSections();
        loadBuckets();
        bucketsToBeans();
    }

    private void setMapForAllSections() {
        sectionIdToBucketList = new HashMap<>();
        sectionBucketList = new ArrayList<>();

        List<TestSection> sectionList = testSectionService.getAllActiveTestSections();

        for (TestSection section : sectionList) {
            if (USER_SELECT_SECTION_ID == null || !USER_SELECT_SECTION_ID.equals(section.getId())) {
                TestBucket bucket = new TestBucket();
                bucket.testSection = section.getLocalizedName();
                sectionBucketList.add(bucket);
                sectionIdToBucketList.put(section.getId(), bucket);
            }
        }
    }

    private void loadBuckets() {
        List<Analysis> analysisList = analysisService.getAnalysesForStatusId(TECH_ACCEPT_ID);

        for (Analysis analysis : analysisList) {
            TestBucket bucket = sectionIdToBucketList.get(analysis.getTestSection().getId());
            bucket.count++;
        }
    }

    private void bucketsToBeans() {
        reportItems = new ArrayList<>();

        for (TestBucket bucket : sectionBucketList) {
            ValidationBacklogData data = new ValidationBacklogData();
            data.setTestSection(bucket.testSection);
            data.setCount(String.valueOf(bucket.count));
            reportItems.add(data);
        }
    }

    private class TestBucket {
        public String testSection;
        public int count = 0;
    }
}
