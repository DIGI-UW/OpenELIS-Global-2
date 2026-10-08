package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

@org.springframework.transaction.annotation.Transactional
public class PatientResultsPdfTest extends BaseWebContextSensitiveTest {

    private String originalPaperSize;
    private String originalBillingLabel;

    @Before
    public void setUp() throws Exception {
        originalPaperSize = ConfigurationProperties.getInstance().getPropertyValue(Property.REPORT_PAPER_SIZE);
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        executeDataSetWithStateManagement("testdata/reporting-r1-patient.xml");
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9842");
    }

    @After
    public void tearDown() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, originalPaperSize);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
    }

    @Test
    public void completionAndCorrectedNotesBelongToEachOrder() throws Exception {
        ClinicalPatientData complete = rows("PAT-0001").get(0);
        ClinicalPatientData partial = rows("PAT-0001").get(0);
        complete.setAccessionNumber("COMPLETE-ORDER");
        partial.setAccessionNumber("PARTIAL-ORDER");
        partial.setCompleteFlag("Partial");
        partial.setCorrectedResult(true);
        partial.setNote("Corrected<br/>A &amp; B &lt;literal&gt;");
        String text = PdfText.of(PatientResultsPdf.render(settings(true, List.of(), null), List.of(complete, partial)));
        assertTrue(text, text.contains("Results Complete") && text.contains("Results Partial"));
        assertTrue(text, text.indexOf("Results Partial") > text.indexOf("COMPLETE-ORDER"));
        assertTrue(text, text.contains("Corrected\nA & B <literal>"));
        org.junit.Assert.assertFalse(text, text.contains("<br") || text.contains("&amp;"));
    }

    @Test
    public void overflowingSectionsRetainOrderIdentityAndColumnLabelsOnBothPaperSizes() throws Exception {
        for (String size : List.of("A4", "Letter")) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, size);
            List<ClinicalPatientData> rows = new ArrayList<>();
            for (int i = 0; i < 95; i++) {
                rows.add(row("PAT-CONTINUATION", "Long Section", "Test " + i, "12.5", "B", "12 - 16", "g/L",
                        "Note " + i));
            }
            byte[] pdf = PatientResultsPdf.render(settings(true, List.of(), null), rows);
            for (int page = 1; page <= PdfText.pageCount(pdf); page++) {
                String text = PdfText.ofPage(pdf, page);
                if (text.contains("Test ")) {
                    assertTrue(text, text.contains("PAT-CONTINUATION") && text.contains("DEV0126000000000961"));
                    assertTrue(text, text.contains("Long Section")
                            && text.contains("Test Spec Result Status Alert Reference value Unit"));
                }
            }
            assertTrue("must paginate", PdfText.pageCount(pdf) > 1);
            org.openelisglobal.testsupport.PdfRegression.save(pdf, "patient-results-" + size);
        }
    }

    @Test
    public void listsAPatientsOrderDetailsAndResultsBySection() throws Exception {
        byte[] pdf = PatientResultsPdf.render(settings(true, List.of(), null), rows("PAT-0001"));
        List<String> lines = lines(PdfText.of(pdf));

        assertLine(lines, "Results of Analysis");
        assertLine(lines, "Results Complete");
        assertLine(lines, "Patient code PAT-0001 National ID NID-778 Age 34 Y DOB 12/03/1992 Sex F");
        assertLine(lines, "URAP Number BILL-55");
        assertContains(lines, "Referring site Central Clinic");
        assertContains(lines, "Ward/Dept/Unit Outpatients");
        assertContains(lines, "Lab Number DEV0126000000000961 Program Routine");
        assertLine(lines, "Specimen number - Collection date and time: DEV0126000000000961-1 01/10/2026 08:40");
        assertLine(lines, "Hematology");
        assertLine(lines, "Test Spec Result Status Alert Reference value Unit");
        assertLine(lines, "Hemoglobin 1 10.2 Validated B 12.0 - 16.0 g/dL");
        assertLine(lines, "Biochemistry");
        assertLine(lines, "Glucose 1 6.3 Validated E 3.9 - 5.8 mmol/L");
        assertLine(lines, "Fasting sample not confirmed");
        assertContains(lines, "General Comments");
        assertContains(lines, "Signature / Validation");
        assertContains(lines, "B = Below Normal");
    }

    @Test
    public void startsEachPatientOnANewPageNumberedFromOne() throws Exception {
        List<ClinicalPatientData> rows = new ArrayList<>(rows("PAT-0001"));
        rows.addAll(rows("PAT-0002"));

        byte[] pdf = PatientResultsPdf.render(settings(true, List.of(), null), rows);

        assertEquals(2, PdfText.pageCount(pdf));
        for (int page = 1; page <= 2; page++) {
            String text = PdfText.ofPage(pdf, page);
            assertTrue(text, text.contains("Results of Analysis"));
            assertTrue(text, text.contains("Page 1"));
        }
        assertTrue(PdfText.ofPage(pdf, 2).contains("PAT-0002"));
    }

    @Test
    public void printsTheAccreditationNotesLineUnderTheTitle() throws Exception {
        byte[] pdf = PatientResultsPdf
                .render(settings(false, List.of(logo()), "Accredited to ISO 15189 for marked tests"), rows("PAT-0001"));

        assertContains(lines(PdfText.of(pdf)), "Accredited to ISO 15189 for marked tests");
    }

    @Test
    public void printsTheSiteNameLinesInTheHeader() throws Exception {
        PatientResultsPdf.Settings settings = new PatientResultsPdf.Settings(
                List.of("Central Public Health Laboratory", "PO Box 12, Port Moresby"), List.of(), null, true,
                "URAP Number", false, null, null, null, false);

        List<String> lines = lines(PdfText.of(PatientResultsPdf.render(settings, rows("PAT-0001"))));

        assertLine(lines, "Central Public Health Laboratory");
        assertLine(lines, "PO Box 12, Port Moresby");
    }

    @Test
    public void printsOnTheSitesPaperSize() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, "Letter");
        byte[] letter = PatientResultsPdf.render(settings(false, List.of(), null), rows("PAT-0001"));
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, "A4");
        byte[] a4 = PatientResultsPdf.render(settings(false, List.of(), null), rows("PAT-0001"));

        assertEquals(792f, PdfText.pageHeight(letter, 1), 0.5f);
        assertEquals(842f, PdfText.pageHeight(a4, 1), 0.5f);
    }

    @Test
    public void theRoutinePatientReportDrawsItsRowsWithThisLayout() throws Exception {
        PatientCILNSPClinical_vreduit report = new PatientCILNSPClinical_vreduit();
        report.initializeReport(new ReportForm());
        report.reportItems = new ArrayList<>(rows("PAT-0001"));

        List<String> lines = lines(PdfText.of(report.renderReport()));

        assertLine(lines, "Results of Analysis");
        assertLine(lines, "Hemoglobin 1 10.2 Validated B 12.0 - 16.0 g/dL");
    }

    private static PatientResultsPdf.Settings settings(boolean usePageNumbers, List<byte[]> logos, String notes) {
        return new PatientResultsPdf.Settings(List.of(), logos, notes, true, "URAP Number", false, null, null, null,
                usePageNumbers);
    }

    private static List<ClinicalPatientData> rows(String patient) {
        return List.of(row(patient, "Hematology", "Hemoglobin", "10.2", "B", "12.0 - 16.0", "g/dL", null), row(patient,
                "Biochemistry", "Glucose", "6.3", "E", "3.9 - 5.8", "mmol/L", "Fasting sample not confirmed"));
    }

    private static ClinicalPatientData row(String patient, String section, String test, String result, String flag,
            String range, String uom, String note) {
        ClinicalPatientData data = new ClinicalPatientData();
        data.setSubjectNumber(patient);
        data.setAccessionNumber("DEV0126000000000961");
        data.setNationalId("NID-778");
        data.setBillingNumber("BILL-55");
        data.setPatientName("Doe, Jane");
        data.setAge("34 Y");
        data.setDob("12/03/1992");
        data.setGender("F");
        data.setContactInfo("Dr Prescriber");
        data.setSiteInfo("Central Clinic|Outpatients");
        data.setLabOrderType("Routine");
        data.setOrderDate("01/10/2026");
        data.setReceivedDate("01/10/2026 09:15");
        data.setCollectionDateTime("DEV0126000000000961-1 01/10/2026 08:40");
        data.setTestSection(section);
        data.setTestName(test);
        data.setSampleSortOrder("1");
        data.setResult(result);
        data.setAnalysisStatus("Validated");
        data.setAlerts(flag);
        data.setTestRefRange(range);
        data.setUom(uom);
        data.setNote(note);
        data.setCompleteFlag("Complete");
        return data;
    }

    private static byte[] logo() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static List<String> lines(String text) {
        return Arrays.asList(text.split("\n"));
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }

    private static void assertContains(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.stream().anyMatch(line -> line.contains(expected)));
    }
}
