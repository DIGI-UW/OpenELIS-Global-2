package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfText;

public class PatientResultsPdfTest extends BaseWebContextSensitiveTest {

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
