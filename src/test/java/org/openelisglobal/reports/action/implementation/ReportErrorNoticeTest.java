package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testsupport.PdfText;

public class ReportErrorNoticeTest extends BaseWebContextSensitiveTest {

    private static final String NOTICE_TITLE = "It is not possible to create the report at this time.";

    private final String reportDir = getClass().getClassLoader().getResource("reports/").getPath();

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
    public void reportWithoutADateRange_printsTheNoticeWithTheReason() throws Exception {
        ActivityReportByTest report = new ActivityReportByTest();
        report.initializeReport(new ReportForm());
        report.setReportPath(reportDir);

        String text = PdfText.of(report.runReport());

        assertTrue(text, text.startsWith(NOTICE_TITLE));
        assertTrue(text, text.contains("Received date was not specified"));
    }

    @Test
    public void collectionWithNothingToPrint_printsTheNotice() throws Exception {
        CollectionReport report = new CollectionReport() {
            @Override
            protected List<byte[]> generateReports() {
                return List.of();
            }
        };
        report.initializeReport(new ReportForm());
        report.setReportPath(reportDir);

        String text = PdfText.of(report.runReport());

        assertTrue(text, text.startsWith(NOTICE_TITLE));
        assertTrue(text, text.contains("No reports met printing criteria"));
    }
}
