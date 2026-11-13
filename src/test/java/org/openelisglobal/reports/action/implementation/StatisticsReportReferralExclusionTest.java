package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.reports.action.implementation.reportBeans.StatisticsReportData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.reports.form.ReportForm.ReceptionTime;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A test sent to a reference laboratory is that laboratory's work. The
 * Statistics Report counted it as this laboratory's own, so a month in which
 * the laboratory referred everything out still reported a workload.
 */
@WithMockUser(username = "admin", roles = "GLOBAL_ADMIN")
public class StatisticsReportReferralExclusionTest extends BaseWebContextSensitiveTest {

    private static final String REPORT_YEAR = "2024";

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private LocalizationService localizationService;

    private Analysis inHouse;
    private Analysis referred;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/referral-set.xml");

        // The fixture leaves no lab units behind, so the report needs one to
        // filter on.
        TestSection section = createTestSection();
        String finalized = statusService.getStatusID(AnalysisStatus.Finalized);

        List<Analysis> analyses = analysisService.getAll();
        inHouse = analyses.get(0);
        referred = analyses.get(1);
        for (Analysis analysis : Arrays.asList(inHouse, referred)) {
            analysis.setTestSection(section);
            analysis.setStatusId(finalized);
            analysis.setStartedDate(Timestamp.valueOf(REPORT_YEAR + "-03-04 09:30:00"));
            analysis.setReferredOut(false);
            analysis.setSysUserId("1");
            analysisService.update(analysis);
        }
    }

    private TestSection createTestSection() {
        Localization name = new Localization();
        name.setDescription("test unit name");
        name.setLocalizedValue("en", "Referral Stats Unit");
        name.setSysUserId("1");
        localizationService.insert(name);

        TestSection section = new TestSection();
        section.setTestSectionName("Referral Stats Unit");
        section.setDescription("lab unit under test");
        section.setIsActive("Y");
        section.setSortOrderInt(1);
        section.setLocalization(name);
        section.setSysUserId("1");
        String id = testSectionService.insert(section);
        return testSectionService.getTestSectionById(id);
    }

    @Test
    public void createReportData_countsBothTests_whenNeitherIsReferredOut() {
        assertEquals(2, countedTests());
        assertEquals(2, countedSamples());
    }

    @Test
    public void createReportData_stopsCountingATestOnceItIsReferredOut() {
        markReferredOut(referred.getId());

        // Only the test the laboratory ran itself is left; the referred one is
        // the reference laboratory's work and belongs on the External Referrals
        // report instead.
        assertEquals(1, countedTests());
        assertEquals(1, countedSamples());
        assertEquals(List.of(inHouse.getTest().getName()), countedTestNames());
    }

    @Test
    public void createReportData_countsNothing_whenEveryTestIsReferredOut() {
        markReferredOut(inHouse.getId());
        markReferredOut(referred.getId());

        assertEquals(0, countedTests());
        assertEquals(0, countedSamples());
    }

    private void markReferredOut(String analysisId) {
        Analysis fresh = analysisService.get(analysisId);
        fresh.setReferredOut(true);
        fresh.setSysUserId("1");
        analysisService.update(fresh);
    }

    private List<StatisticsReportData> runReport() {
        StatisticsReport report = new StatisticsReport();
        ReportForm form = new ReportForm();
        form.setUpperYear(REPORT_YEAR);
        form.setLabSections(new ArrayList<>(List.of(analysisService.get(inHouse.getId()).getTestSection().getId())));
        // Passing every priority and reception time keeps those filters out of
        // the way, so the only thing under test is the referral exclusion.
        form.setPriority(new ArrayList<>(Arrays.asList(OrderPriority.values())));
        form.setReceptionTime(new ArrayList<>(Arrays.asList(ReceptionTime.values())));

        report.createReportData(form);
        @SuppressWarnings("unchecked")
        List<StatisticsReportData> items = (List<StatisticsReportData>) ReflectionTestUtils.getField(report,
                "reportItems");
        return items;
    }

    /** March holds the fixture's analyses; the report is a month-by-month grid. */
    private int countedTests() {
        return runReport().stream().mapToInt(StatisticsReportData::getTestsMar).sum();
    }

    private int countedSamples() {
        return runReport().stream().mapToInt(StatisticsReportData::getSamplesMar).sum();
    }

    private List<String> countedTestNames() {
        return runReport().stream().filter(item -> item.getTestsMar() > 0).map(StatisticsReportData::getTestName)
                .collect(Collectors.toList());
    }
}
