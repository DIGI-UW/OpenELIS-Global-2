package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.reports.action.implementation.reportBeans.RejectionReportBean;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testsupport.PdfText;

public class RejectionReportTest extends BaseWebContextSensitiveTest {

    @Before
    public void setUp() throws Exception {
        IAccessionNumberGenerator generator = Mockito.mock(IAccessionNumberGenerator.class);
        Mockito.when(generator.getPrefix()).thenReturn("DEV");
        AccessionNumberValidatorFactory factory = SpringContext.getBean(AccessionNumberValidatorFactory.class);
        Mockito.when(factory.getGenerator(Mockito.any())).thenReturn(generator);
    }

    @After
    public void tearDown() {
        Mockito.reset(SpringContext.getBean(AccessionNumberValidatorFactory.class));
    }

    @Test
    public void listsEachRejectedTestUnderItsOrder() throws Exception {
        String text = PdfText.of(report().runReport());

        assertTrue(text, text.contains("Rejection report"));
        assertTrue(text, text.contains("Unit: Biochemistry"));
        assertTrue(text, text.contains("Reason for rejection"));
        assertTrue(text, text.contains("Tech ID"));
        assertFalse("no unresolved message references: " + text, text.contains("$R{"));
        List<String> lines = Arrays.asList(text.split("\n"));
        assertTrue(text, lines.contains("30/09/26 01/10/26 0126000000000101 DOE / NID-1"));
        assertTrue("an order's dates and lab number print once: " + text, lines.contains("Glucose Hemolysed tech1"));
        assertTrue(text, lines.contains("30/09/26 01/10/26 0126000000000102 Not registered"));
        assertTrue(text, lines.contains("Glucose Clotted tech2"));
    }

    private RejectionReport report() {
        RejectionReport report = new RejectionReport() {
            @Override
            protected String getActivityLabel() {
                return "Unit: Biochemistry";
            }

            @Override
            protected void buildReportContent(ReportSpecificationList selection) {
                rejections = new ArrayList<>();
                RejectionReportBean hemolysed = rejected("0126000000000101", "DOE / NID-1", "Hemolysed", "tech1");
                rejections.add(createIdentityRejectionBean(hemolysed, false));
                hemolysed.setCollectionDate(null);
                rejections.add(hemolysed);
                RejectionReportBean clotted = rejected("0126000000000102", "", "Clotted", "tech2");
                rejections.add(createIdentityRejectionBean(clotted, false));
                clotted.setCollectionDate(null);
                rejections.add(clotted);
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

    private static RejectionReportBean rejected(String accession, String patient, String reason, String technician) {
        RejectionReportBean item = new RejectionReportBean();
        item.setAccessionNumber(accession);
        item.setCollectionDate("30/09/26");
        item.setReceivedDate("01/10/26");
        item.setNonPrintingPatient(patient);
        item.setPatientOrTestName("Glucose");
        item.setRejectionReason(reason);
        item.setTechnician(technician);
        return item;
    }
}
