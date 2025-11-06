package org.openelisglobal.reports.action.implementation;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.StatisticsReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.reports.form.ReportForm.ReceptionTime;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class StatisticsReport extends IndicatorReport implements IReportCreator, IReportParameterSetter {

    private List<StatisticsReportData> reportItems;
    private String year;
    private String priority;
    private String labUnits;
    private String receptionTime;

    @Override
    public void setRequestParameters(ReportForm form) {
        form.setUseStatisticsParams(true);
        new ReportSpecificationList(
                DisplayListService.getInstance().getList(DisplayListService.ListType.TEST_SECTION_ACTIVE),
                MessageUtil.getMessage("workplan.unit.types")).setRequestParameters(form);
        form.setYearList(getYearList());
        form.setPriorityList(DisplayListService.getInstance().getList(DisplayListService.ListType.ORDER_PRIORITY));
        form.setReceptionTimeList(getReceptionTimeList());
    }

    @Override
    public void initializeReport(ReportForm form) {
        super.initializeReport();
        inntialiseReportParams(form);
        createReportParameters();
        createReportData(form);
    }

    @Override
    protected String getNameForReportRequest() {
        return "null";
    }

    @Override
    protected String getNameForReport() {
        return MessageUtil.getMessage("openreports.stat.aggregate");
    }

    @Override
    protected String getLabNameLine1() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
    }

    @Override
    protected String getLabNameLine2() {
        return "";
    }

    @Override
    public JRDataSource getReportDataSource() throws IllegalStateException {
        return errorFound ? new JRBeanCollectionDataSource(errorMsgs) : new JRBeanCollectionDataSource(reportItems);
    }

    @Override
    protected String reportFileName() {
        // TODO Auto-generated method stub
        return "StatisticsReport";
    }

    public void createReportData(ReportForm form) {
        AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
        TestService testService = SpringContext.getBean(TestService.class);
        Date firstDate = DateUtil.getFistDayOfTheYear(Integer.valueOf(form.getUpperYear()));

        Date lastDate = DateUtil.getLastDayOfTheYear(Integer.valueOf(form.getUpperYear()));
        List<Test> testList = testService.getAllActiveTests(false);
        List<Integer> testSectionIds = form.getLabSections().stream().map(sectionId -> Integer.valueOf(sectionId))
                .collect(Collectors.toList());
        reportItems = new ArrayList<>();
        testList.forEach(test -> {
            List<Analysis> yearAnalysis = new ArrayList();
            // get all anaysis collected with in the specifed year m for a specific test and
            // matching specific test sections
            yearAnalysis = analysisService.getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(
                    DateUtil.convertDateTimeToSqlDate(firstDate), DateUtil.convertDateTimeToSqlDate(lastDate),
                    test.getId(), testSectionIds);

            // filter only validated analysis
            yearAnalysis = yearAnalysis.stream().filter(analysis -> SpringContext.getBean(IStatusService.class)
                    .matches(analysis.getStatusId(), AnalysisStatus.Finalized)).collect(Collectors.toList());

            // filter the analysis by priority
            if (form.getPriority() != null || form.getPriority().size() > 0) {
                if (form.getPriority().size() < OrderPriority.values().length) {
                    yearAnalysis = yearAnalysis.stream().filter(
                            analysis -> form.getPriority().contains(analysis.getSampleItem().getSample().getPriority()))
                            .collect(Collectors.toList());
                }
            }

            // filter the analysis by Reception time
            if (form.getReceptionTime() != null) {
                if (form.getReceptionTime().size() < ReceptionTime.values().length) {
                    for (ReceptionTime time : form.getReceptionTime()) {
                        if (time.equals(ReceptionTime.NORMAL_WORK_HOURS)) {
                            // 09:00:00-15:30:00
                            yearAnalysis = yearAnalysis.stream().filter(analysis -> analysis.getEnteredDate() != null)
                                    .filter(analysis -> checkTimeRange(analysis.getEnteredDate(), "09:00:00",
                                            "15:30:59"))
                                    .collect(Collectors.toList());
                        } else if (time.equals(ReceptionTime.OUT_OF_NORMAL_WORK_HOURS)) {
                            // 15:31:00 - 08:59:00
                            yearAnalysis = yearAnalysis.stream().filter(analysis -> analysis.getEnteredDate() != null)
                                    .filter(analysis -> checkOutOfWorkingTimeRange(analysis.getEnteredDate()))
                                    .collect(Collectors.toList());
                        }
                    }
                }
            }
            // group tests and sample by Month

            Set<String> janTests = new HashSet<>();
            Set<String> febTests = new HashSet<>();
            Set<String> marTests = new HashSet<>();
            Set<String> aprTests = new HashSet<>();
            Set<String> mayTests = new HashSet<>();
            Set<String> junTests = new HashSet<>();
            Set<String> julTests = new HashSet<>();
            Set<String> augTests = new HashSet<>();
            Set<String> sepTests = new HashSet<>();
            Set<String> octTests = new HashSet<>();
            Set<String> novTests = new HashSet<>();
            Set<String> decTests = new HashSet<>();

            Set<String> janSamples = new HashSet<>();
            Set<String> febSamples = new HashSet<>();
            Set<String> marSamples = new HashSet<>();
            Set<String> aprSamples = new HashSet<>();
            Set<String> maySamples = new HashSet<>();
            Set<String> junSamples = new HashSet<>();
            Set<String> julSamples = new HashSet<>();
            Set<String> augSamples = new HashSet<>();
            Set<String> sepSamples = new HashSet<>();
            Set<String> octSamples = new HashSet<>();
            Set<String> novSamples = new HashSet<>();
            Set<String> decSamples = new HashSet<>();

            if (!yearAnalysis.isEmpty()) {
                yearAnalysis.forEach(analysis -> {
                    // Test test = analysis.getTest();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(analysis.getStartedDate());
                    switch (cal.get(Calendar.MONTH)) {
                    case 0: {
                        janTests.add(analysis.getId());
                        janSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 1: {
                        febTests.add(analysis.getId());
                        febSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 2: {
                        marTests.add(analysis.getId());
                        marSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 3: {
                        aprTests.add(analysis.getId());
                        aprSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 4: {
                        mayTests.add(analysis.getId());
                        maySamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 5: {
                        junTests.add(analysis.getId());
                        junSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 6: {
                        julTests.add(analysis.getId());
                        julSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 7: {
                        augTests.add(analysis.getId());
                        augSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 8: {
                        sepTests.add(analysis.getId());
                        sepSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 9: {
                        octTests.add(analysis.getId());
                        octSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 10: {
                        novTests.add(analysis.getId());
                        novSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    case 11: {
                        decTests.add(analysis.getId());
                        decSamples.add(analysis.getSampleItem().getSample().getId());
                        break;
                    }
                    }
                });

                StatisticsReportData data = new StatisticsReportData();
                data.setTestName(test.getLocalizedName());
                data.setTestsJan(janTests.size());
                data.setSamplesJan(janSamples.size());
                data.setTestsFeb(febTests.size());
                data.setSamplesFeb(febSamples.size());
                data.setTestsMar(marTests.size());
                data.setSamplesMar(marSamples.size());
                data.setTestsApr(aprTests.size());
                data.setSamplesApr(aprSamples.size());
                data.setTestsMay(mayTests.size());
                data.setSamplesMay(maySamples.size());
                data.setTestsJun(junTests.size());
                data.setSamplesJun(junSamples.size());
                data.setTestsJul(julTests.size());
                data.setSamplesJul(julSamples.size());
                data.setTestsAug(augTests.size());
                data.setSamplesAug(augSamples.size());
                data.setTestsSep(sepTests.size());
                data.setSamplesSep(sepSamples.size());
                data.setTestsOct(octTests.size());
                data.setSamplesOct(octSamples.size());
                data.setTestsNov(novTests.size());
                data.setSamplesNov(novSamples.size());
                data.setTestsDec(decTests.size());
                data.setSamplesDec(decSamples.size());
                reportItems.add(data);
            }
        });
        if (reportItems.isEmpty()) {
            StatisticsReportData emptydata = new StatisticsReportData();
            emptydata.setTestName(null);
            reportItems.add(emptydata);
        }
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();

        reportParameters.put("siteId", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        reportParameters.put("directorName",
                ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName));
        reportParameters.put("labName1", getLabNameLine1());
        reportParameters.put("labName2", getLabNameLine2());
        reportParameters.put("reportTitle", getNameForReport());
        reportParameters.put("usePageNumbers", "true");
        reportParameters.put("headerName", "GeneralHeader.jasper");
        reportParameters.put("year", year);
        reportParameters.put("labUnits", MessageUtil.getMessage("label.openreports.testsection") + " " + labUnits);
        reportParameters.put("workHours",
                MessageUtil.getMessage("sample.batchentry.order.receptiontime") + ": " + receptionTime);
        reportParameters.put("priority", MessageUtil.getMessage("sample.entry.priority") + ": " + priority);
    }

    private List<IdValuePair> getYearList() {
        List<IdValuePair> list = new ArrayList<>();
        int currentYear = DateUtil.getCurrentYear();
        for (int i = 15; i >= 0; i--) {
            String year = String.valueOf(currentYear - i);
            list.add(new IdValuePair(year, year));
        }
        Collections.reverse(list);
        return list;
    }

    private List<IdValuePair> getReceptionTimeList() {
        List<IdValuePair> list = new ArrayList<>();
        list.add(new IdValuePair(ReportForm.ReceptionTime.NORMAL_WORK_HOURS.name(),
                MessageUtil.getMessage("report.normalWorkingHours")));
        list.add(new IdValuePair(ReportForm.ReceptionTime.OUT_OF_NORMAL_WORK_HOURS.name(),
                MessageUtil.getMessage("report.outofnormalWorkingHours")));
        return list;
    }

    private Boolean checkTimeRange(Timestamp targetTime, String startTime, String stopTime) {
        String stringTargetTime = targetTime.toString().split(" ")[1];
        LocalTime start = LocalTime.parse(startTime);
        LocalTime stop = LocalTime.parse(stopTime);
        LocalTime target = LocalTime.parse(stringTargetTime);
        return ((target.isAfter(start) && target.isBefore(stop)) || target.equals(start) || target.equals(stop));
    }

    private Boolean checkOutOfWorkingTimeRange(Timestamp targetTime) {
        return (checkTimeRange(targetTime, "15:31:00", "23:59:59")
                || checkTimeRange(targetTime, "00:00:00", "08:59:59"));
    }

    private void inntialiseReportParams(ReportForm form) {
        TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
        String startDate = DateUtil
                .formatDateAsText(DateUtil.getFistDayOfTheYear(Integer.valueOf(form.getUpperYear())));
        String endDate = DateUtil.formatDateAsText(DateUtil.getLastDayOfTheYear(Integer.valueOf(form.getUpperYear())));
        year = startDate + " - " + endDate;
        priority = form.getPriority().stream().map(priority -> getPriorityMap().get(priority.name()).toString())
                .collect(Collectors.joining(" , "));
        labUnits = form.getLabSections().stream()
                .map(labunitId -> testSectionService.getTestSectionById(labunitId).getLocalizedName())
                .collect(Collectors.joining(" , "));
        receptionTime = form.getReceptionTime().stream().map(time -> getReceptionTimeMap().get(time.name()).toString())
                .collect(Collectors.joining(" ,"));
    }

    private Map getReceptionTimeMap() {
        Map<String, String> timeMap = new HashMap<>();
        for (IdValuePair value : getReceptionTimeList()) {
            timeMap.put(value.getId(), value.getValue());
        }
        return timeMap;
    }

    private Map getPriorityMap() {
        Map<String, String> prioritMap = new HashMap<>();
        for (IdValuePair value : DisplayListService.getInstance().getList(ListType.ORDER_PRIORITY)) {
            prioritMap.put(value.getId(), value.getValue());
        }
        return prioritMap;
    }
}
