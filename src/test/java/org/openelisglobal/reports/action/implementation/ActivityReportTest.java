package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.reports.action.implementation.reportBeans.ActivityReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testsupport.PdfText;
import org.springframework.jdbc.core.JdbcTemplate;

public class ActivityReportTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        AccessionNumberValidatorFactory factory = SpringContext.getBean(AccessionNumberValidatorFactory.class);
        Mockito.when(factory.getGenerator(Mockito.any())).thenReturn(Mockito.mock(IAccessionNumberGenerator.class));
    }

    @After
    public void tearDown() {
        Mockito.reset(SpringContext.getBean(AccessionNumberValidatorFactory.class));
    }

    @Test
    public void listsEachOrdersDetailsOnceAboveItsTests() throws Exception {
        String text = PdfText.of(report().runReport());

        assertTrue(text, text.contains("Activity Report"));
        assertTrue(text, text.contains("Unit: Biochemistry"));
        assertTrue(text, text.contains("From 01/10/2026 To 02/10/2026"));
        assertTrue(text, text.contains("Collection Date"));
        assertTrue(text, text.contains("Patient Name"));
        assertFalse("every header resolves to a message: " + text, text.contains("barcode.label.info"));
        assertTrue("headers wrap between words: " + text, text.contains("Turnaround"));
        assertTrue(text, text.contains("DEV0126000000000101 30/09/26 01/10/26 NID-1 / REF-1"));
        assertTrue(text, text
                .contains("DEV0126000000000101 Doe, Jane Glucose Finalized 5.4 mmol/L 02/10/2026 10:00 1.10 26.50"));
        assertTrue("an order's patient prints once: " + text,
                text.contains("DEV0126000000000101 Creatinine Finalized 80 umol/L 02/10/2026 10:00 1.10 26.50"));
        assertTrue(text, text.contains("DEV0126000000000102 Doe, John Glucose Referred out 6.1 mmol/L"));
    }

    @Test
    public void anOrderWithoutAPatientIsListedWithABlankPatient() throws Exception {
        executeDataSetWithStateManagement("testdata/validation-backlog-report.xml");
        Result result = new Result();
        result.setAnalysis(SpringContext.getBean(AnalysisService.class).get("901"));
        result.setResultType("N");
        result.setValue("5.4");
        result.setLastupdated(new Timestamp(System.currentTimeMillis()));

        ActivityReportBean item = report().createActivityReportBean(result, true);

        assertTrue(item.getAccessionNumber(), item.getAccessionNumber().endsWith("901"));
        assertTrue("no patient name: " + item.getPatientLastName(), item.getPatientLastName().isEmpty());
        assertTrue("no patient id: " + item.getPatientId(), item.getPatientId().isEmpty());
    }

    /**
     * OGC-1515: the report by test wrote to a parameters map that no longer existed
     * and answered 500 for every test chosen.
     */
    @Test
    public void theReportByTestListsTheChosenTestsResults() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-results-report.xml");
        new JdbcTemplate(SpringContext.getBean(DataSource.class))
                .update("UPDATE clinlims.result SET result_type = 'N' WHERE id = 1");
        ActivityReportByTest report = new ActivityReportByTest();
        ReportForm form = new ReportForm();
        form.setLowerDateRange("12/02/2024");
        form.setUpperDateRange("12/02/2024");
        ReportSpecificationList selection = new ReportSpecificationList(List.of(new IdValuePair("1", "Glucose")),
                "Test");
        selection.setSelection("1");
        form.setSelectList(selection);

        report.initializeReport(form);
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());
        String text = PdfText.of(report.runReport());

        assertTrue(text, text.contains("Test: Glucose"));
        assertTrue(text, text.contains("24-00001"));
    }

    private ActivityReport report() {
        ActivityReport report = new ActivityReport() {
            @Override
            protected String getActivityLabel() {
                return "Unit: Biochemistry";
            }

            @Override
            protected void buildReportContent(ReportSpecificationList selection) {
                testsResults = new ArrayList<>();
                ActivityReportBean glucose = result("DEV0126000000000101", "Jane", "Glucose", "Finalized",
                        "5.4 mmol/L");
                testsResults.add(createIdentityActivityBean(glucose, false));
                testsResults.add(glucose);
                testsResults.add(result("DEV0126000000000101", "Jane", "Creatinine", "Finalized", "80 umol/L"));
                ActivityReportBean referred = result("DEV0126000000000102", "John", "Glucose", "Referred out",
                        "6.1 mmol/L");
                referred.setNonPrintingPatient("NID-2 / REF-2");
                testsResults.add(createIdentityActivityBean(referred, false));
                testsResults.add(referred);
            }
        };
        ReportForm form = new ReportForm();
        form.setLowerDateRange("01/10/2026");
        form.setUpperDateRange("02/10/2026");
        ReportSpecificationList selection = new ReportSpecificationList(List.of(), "Unit");
        selection.setSelection("1");
        form.setSelectList(selection);
        report.initializeReport(form);
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());
        return report;
    }

    private static ActivityReportBean result(String accession, String firstName, String test, String status,
            String value) {
        ActivityReportBean item = new ActivityReportBean();
        item.setAccessionNumber(accession);
        item.setCollectionDate("30/09/26");
        item.setReceivedDate("01/10/26");
        item.setPatientLastName("Doe");
        item.setPatientFirstName(firstName);
        item.setNonPrintingPatient("NID-1 / REF-1");
        item.setPatientOrTestName(test);
        item.setSampleStatus(status);
        item.setResultValue(value);
        item.setResultDate("02/10/2026 10:00");
        item.setTurnaroundDays("1.10");
        item.setTurnaroundHours("26.50");
        return item;
    }
}
