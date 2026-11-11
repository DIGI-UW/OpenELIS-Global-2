package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.testsupport.PdfText;

public class ValidationBacklogReportTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/validation-backlog-report.xml");
        SpringContext.getBean(TestSectionService.class).refreshNames();
    }

    @After
    public void tearDown() {
        SpringContext.getBean(TestSectionService.class).refreshNames();
    }

    @Test
    public void countsTheTestsAwaitingValidationInEachActiveSection() throws Exception {
        ValidationBacklogReport report = new ValidationBacklogReport();
        report.initializeReport(new ReportForm());
        report.setReportPath(getClass().getClassLoader().getResource("reports/").getPath());

        String text = PdfText.of(report.runReport());

        assertTrue(text, text.contains("Delayed Validation"));
        assertTrue(text, text.contains("Test Section Total"));
        assertTrue("only technically accepted tests count: " + text, text.contains("Hematology 2"));
        assertTrue("a section with nothing waiting still lists 0: " + text, text.contains("Biochemistry 0"));
    }
}
