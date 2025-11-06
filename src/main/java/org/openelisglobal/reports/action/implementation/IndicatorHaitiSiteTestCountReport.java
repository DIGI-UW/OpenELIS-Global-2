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
package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.aggregatereporting.valueholder.ReportExternalImport;
import org.openelisglobal.dataexchange.service.aggregatereporting.ReportExternalImportService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.TestSiteYearReport;
import org.openelisglobal.reports.action.implementation.reportBeans.TestSiteYearReport.Months;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;

public class IndicatorHaitiSiteTestCountReport extends CSVExportReport
        implements IReportCreator, IReportParameterSetter {
    private ReportExternalImportService reportExternalImportService = SpringContext
            .getBean(ReportExternalImportService.class);
    private String EOL = System.getProperty("line.separator");
    private static List<IdValuePair> MONTH_LIST;

    private static ContainerFactory containerFactory = new ContainerFactory() {
        @Override
        public List creatArrayContainer() {
            return new ArrayList();
        }

        @Override
        public Map<String, Integer> createObjectContainer() {
            return new HashMap<>();
        }
    };

    private List<TestSiteYearReport> reportList = new ArrayList<>();

    static {
        MONTH_LIST = new ArrayList<>();

        MONTH_LIST.add(new IdValuePair("0", MessageUtil.getMessage("month.january.abbrev")));
        MONTH_LIST.add(new IdValuePair("1", MessageUtil.getMessage("month.february.abbrev")));
        MONTH_LIST.add(new IdValuePair("2", MessageUtil.getMessage("month.march.abbrev")));
        MONTH_LIST.add(new IdValuePair("3", MessageUtil.getMessage("month.april.abbrev")));
        MONTH_LIST.add(new IdValuePair("4", MessageUtil.getMessage("month.may.abbrev")));
        MONTH_LIST.add(new IdValuePair("5", MessageUtil.getMessage("month.june.abbrev")));
        MONTH_LIST.add(new IdValuePair("6", MessageUtil.getMessage("month.july.abbrev")));
        MONTH_LIST.add(new IdValuePair("7", MessageUtil.getMessage("month.august.abbrev")));
        MONTH_LIST.add(new IdValuePair("8", MessageUtil.getMessage("month.september.abbrev")));
        MONTH_LIST.add(new IdValuePair("9", MessageUtil.getMessage("month.october.abbrev")));
        MONTH_LIST.add(new IdValuePair("10", MessageUtil.getMessage("month.november.abbrev")));
        MONTH_LIST.add(new IdValuePair("11", MessageUtil.getMessage("month.december.abbrev")));
    }

    @Override
    protected String reportFileName() {
        return "SiteTestCount";
    }

    @Override
    public void setRequestParameters(ReportForm form) {
        try {
            form.setUsePredefinedDateRanges(Boolean.TRUE);
            new ReportSpecificationList(getSiteList(), MessageUtil.getMessage("report.select.site"))
                    .setRequestParameters(form);
            form.setInstructions(MessageUtil.getMessage("report.instruction.inventory.test.count"));
            form.setMonthList(MONTH_LIST);
            form.setYearList(getYearList());
        } catch (RuntimeException e) {
            LogEvent.logDebug(e);
        }
    }

    private List<IdValuePair> getYearList() {
        List<IdValuePair> list = new ArrayList<>();

        int currentYear = DateUtil.getCurrentYear();

        for (int i = 5; i >= 0; i--) {
            String year = String.valueOf(currentYear - i);
            list.add(new IdValuePair(year, year));
        }

        return list;
    }

    private List<IdValuePair> getSiteList() {
        List<IdValuePair> pairList = new ArrayList<>();

        List<String> sites = reportExternalImportService.getUniqueSites();
        for (String site : sites) {
            pairList.add(new IdValuePair(site, site));
        }

        return pairList;
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        createReportParameters();

        String period = form.getDatePeriod();
        ReportSpecificationList specificationList = form.getSelectList();

        createResults(specificationList.getSelection(), period, form);
    }

    @SuppressWarnings("unchecked")
    private void createResults(String site, String period, ReportForm form) {

        Timestamp beginning = null;
        Timestamp end = DateUtil.getTimestampForBeginningOfMonthAgo(-1);

        if ("year".equals(period)) {
            beginning = DateUtil.getTimestampForBeginingOfYear();
        } else if ("months3".equals(period)) {
            beginning = DateUtil.getTimestampForBeginningOfMonthAgo(2);
        } else if ("months6".equals(period)) {
            beginning = DateUtil.getTimestampForBeginningOfMonthAgo(5);
        } else if ("months12".equals(period)) {
            beginning = DateUtil.getTimestampForBeginningOfMonthAgo(11);
        } else if ("custom".equals(period)) {
            int lowYear = Integer.parseInt(form.getLowerYear());
            int lowMonth = Integer.parseInt(form.getLowerMonth());
            int highYear = Integer.parseInt(form.getUpperYear());
            int highMonth = Integer.parseInt(form.getUpperMonth());

            int currentYear = DateUtil.getCurrentYear();
            int currentMonth = DateUtil.getCurrentMonth();

            beginning = DateUtil
                    .getTimestampForBeginningOfMonthAgo(currentMonth - lowMonth + (12 * (currentYear - lowYear)));
            end = DateUtil
                    .getTimestampForBeginningOfMonthAgo(currentMonth - highMonth + (12 * (currentYear - highYear)) - 1);
        }

        List<ReportExternalImport> reportImportList;
        // get all rows for the date range sort by date and site
        if (GenericValidator.isBlankOrNull(site)) {
            reportImportList = reportExternalImportService.getReportsInDateRangeSorted(beginning, end);
        } else {
            reportImportList = reportExternalImportService.getReportsInDateRangeSortedForSite(beginning, end, site);
        }
        String currentSite = null;

        Map<String, Integer>[] monthlyTestCount = null;

        for (ReportExternalImport report : reportImportList) {
            if (!report.getSendingSite().equals(currentSite)) {
                createReportLinesForSite(currentSite, monthlyTestCount);

                monthlyTestCount = createNewMonthlyTestCountArray();
                currentSite = report.getSendingSite();
            }

            Map<String, Integer> targetMonthTestCount;
            if (monthlyTestCount != null) {
                targetMonthTestCount = monthlyTestCount[DateUtil.getMonthForTimestamp(report.getEventDate())];
            } else {
                throw new IllegalStateException();
            }

            JSONParser parser = new JSONParser();

            try {
                Map<String, Integer> databaseTestCountList = (Map<String, Integer>) parser
                        .parse(report.getData().replace("\n", ""), containerFactory);

                for (String test : databaseTestCountList.keySet()) {
                    if (!targetMonthTestCount.containsKey(test)) {
                        targetMonthTestCount.put(test, 0);
                    }

                    int current = targetMonthTestCount.get(test);
                    int additional = Integer.parseInt(String.valueOf(databaseTestCountList.get(test)));
                    targetMonthTestCount.put(test, current + additional);
                }

            } catch (ParseException e) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "createResults", e.toString());
            }
        }

        createReportLinesForSite(currentSite, monthlyTestCount);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer>[] createNewMonthlyTestCountArray() {
        Map<String, Integer>[] newArray = new HashMap[12];

        for (int i = 0; i < newArray.length; i++) {
            newArray[i] = new HashMap<>();
        }

        return newArray;
    }

    private void createReportLinesForSite(String currentSite, Map<String, Integer>[] monthlyTestCount) {
        if (monthlyTestCount == null) {
            return;
        }

        Map<String, TestSiteYearReport> testToLineMap = new HashMap<>();
        for (Months month : Months.values()) {
            Map<String, Integer> testToCountMap = monthlyTestCount[month.getIndex()];

            for (String test : testToCountMap.keySet()) {
                if (!testToLineMap.containsKey(test)) {
                    TestSiteYearReport newReportLine = new TestSiteYearReport();
                    newReportLine.setSiteName(currentSite);
                    newReportLine.setTestName(test);
                    testToLineMap.put(test, newReportLine);
                    reportList.add(newReportLine);
                }

                testToLineMap.get(test).addToMonth(month, testToCountMap.get(test));
            }
        }
    }

    @Override
    public String getResponseHeaderName() {
        return "Content-Disposition";
    }

    @Override
    public String getResponseHeaderContent() {
        return "attachment;filename=" + getReportFileName() + ".csv";
    }

    @Override
    public byte[] runReport() throws UnsupportedEncodingException, IOException, IllegalStateException, SQLException,
            JRException, java.text.ParseException {
        if (errorFound) {
            return super.runReport();
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream(100000);
        buffer.write(getColumnNamesLine().getBytes("windows-1252"));

        Collections.sort(reportList, new Comparator<TestSiteYearReport>() {
            @Override
            public int compare(TestSiteYearReport o1, TestSiteYearReport o2) {
                int compare = o1.getSiteName().compareTo(o2.getSiteName());

                if (compare == 0) {
                    return o1.getTestName().compareTo(o2.getTestName());
                }

                return compare;
            }
        });

        for (TestSiteYearReport report : reportList) {
            buffer.write(getReportLine(report).getBytes("windows-1252"));
        }

        return buffer.toByteArray();
    }

    private String getReportLine(TestSiteYearReport report) {
        int total = 0;
        StringBuilder line = new StringBuilder();

        line.append(StringUtil.escapeCSVValue(report.getTestName()));
        line.append(",");
        line.append(StringUtil.escapeCSVValue(report.getSiteName()));
        line.append(",");

        for (Months month : Months.values()) {
            int value = report.getCountForMonth(month);
            total += value;
            if (value > 0) {
                line.append(value);
            }
            line.append(",");
        }

        line.append(String.valueOf(total));
        line.append(EOL);

        return line.toString();
    }

    private String getColumnNamesLine() {
        StringBuilder line = new StringBuilder();

        line.append(MessageUtil.getMessage("report.column.test"));
        line.append(",");
        line.append(MessageUtil.getMessage("report.column.site"));
        line.append(",");
        for (IdValuePair month : MONTH_LIST) {
            line.append(month.getValue());
            line.append(",");
        }
        line.append(MessageUtil.getMessage("report.column.total"));
        line.append(EOL);
        return line.toString();
    }
}
