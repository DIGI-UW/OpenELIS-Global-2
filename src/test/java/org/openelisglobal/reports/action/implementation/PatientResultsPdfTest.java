package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.action.implementation.reportBeans.PatientReportOrder;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportCallbackLine;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportComponentLine;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportSampleLine;
import org.openelisglobal.reports.form.ReportForm;
import org.openelisglobal.testsupport.PdfImages;
import org.openelisglobal.testsupport.PdfRegression;
import org.openelisglobal.testsupport.PdfText;

/**
 * The patient report's layout (FRS OGC-1111 Part A, V1) from report rows: what
 * a clinician and an assessor read on the page.
 */
@org.springframework.transaction.annotation.Transactional
public class PatientResultsPdfTest extends BaseWebContextSensitiveTest {

    private static final String ORDER = "DEV0126000000000961";
    private static final UiMessages ENGLISH = UiMessages.forLocale(Locale.ENGLISH, null);

    @Rule
    public TemporaryFolder overrides = new TemporaryFolder();

    private String originalPaperSize;
    private String originalBillingLabel;

    @Before
    public void rememberSettings() throws Exception {
        originalPaperSize = ConfigurationProperties.getInstance().getPropertyValue(Property.REPORT_PAPER_SIZE);
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        executeDataSetWithStateManagement("testdata/reporting-r1-patient.xml");
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9842");
    }

    @After
    public void restoreSettings() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, originalPaperSize);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
    }

    @Test
    public void printsOnTheSitesPaperSize() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, "Letter");
        byte[] letter = render(rows(ORDER), Map.of(ORDER, order()));
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, "A4");
        byte[] a4 = render(rows(ORDER), Map.of(ORDER, order()));

        assertEquals(792f, PdfText.pageHeight(letter, 1), 0.5f);
        assertEquals(842f, PdfText.pageHeight(a4, 1), 0.5f);
    }

    /**
     * OGC-1031 FR-4.6: a report issued by the Report Print Queue names its version
     * and first issue time in the footer of every page, corrected or not; a print
     * outside the queue keeps the plain issue time.
     */
    @Test
    public void footerNamesTheVersionAndItsIssueTimeOfAQueuePrint() throws Exception {
        PatientReportOrder order = order();
        order.setVersion(2);
        order.setIssueTime("10/20/26 07:40");

        String text = PdfText.of(render(rows(ORDER), Map.of(ORDER, order)));

        assertTrue(text, text.contains("Version 2 · Issued 10/20/26 07:40"));
        assertFalse("not amended, so no corrected banner", text.contains("Corrected report"));

        String plain = PdfText.of(render(rows(ORDER), Map.of(ORDER, order())));
        assertTrue(plain.contains("Issued "));
        assertFalse(plain.contains("Version"));
    }

    @Test
    public void headerNamesTheLabTheFinalOrPartialTitleTheDirectorAndTheAccreditingBodies() throws Exception {
        PatientReportOrder order = order();
        order.setAccreditingBodies(List.of("ISO 15189", "SANAS"));
        String text = PdfText.of(render(rows(ORDER), Map.of(ORDER, order)));

        assertTrue(text, text.contains("Central Public Health Laboratory"));
        assertTrue(text, text.contains("Results: final report"));
        assertTrue(text, text.contains("PO Box 12, Port Moresby"));
        assertTrue(text, text.contains("Lab director: Dr Jane Kila, Laboratory Director"));
        assertTrue(text, text.contains("Tests on this report are accredited by: ISO 15189, SANAS."));

        order.setComplete(false);
        assertTrue(PdfText.of(render(rows(ORDER), Map.of(ORDER, order))).contains("Results: partial report"));
    }

    @Test
    public void cardShowsPatientOrderAndDatesWithTheRequesterUnderOrder() throws Exception {
        List<String> lines = lines(PdfText.of(render(rows(ORDER), Map.of(ORDER, order()))));

        assertContains(lines, "Doe, Jane");
        assertContains(lines, "34 Y · F");
        assertContains(lines, "Date of Birth 12/03/1992");
        assertContains(lines, "Patient ID PAT-0001");
        assertContains(lines, "National ID NID-778");
        assertContains(lines, "URAP Number BILL-55");
        assertContains(lines, "Lab Number " + ORDER);
        assertContains(lines, "Requester Dr Prescriber");
        assertContains(lines, "Site / ward Central Clinic /");
        assertContains(lines, "Outpatients");
        assertContains(lines, "Completed 01/10/26 11:40");
    }

    @Test
    public void samplesBlockListsEachSampleWithItsTimesAndCondition() throws Exception {
        PatientReportOrder order = order();
        order.setSamples(List.of(
                new ReportSampleLine("1", "Serum", "01/10/26 08:10", "01/10/26 08:50", null, "Fasting"),
                new ReportSampleLine("2", "Urine", null, "01/10/26 09:00", null, null),
                new ReportSampleLine("3", "Whole blood", "01/10/26 07:55", "01/10/26 09:00", "Haemolysed", null)));
        List<String> lines = lines(PdfText.of(render(rows(ORDER), Map.of(ORDER, order))));

        assertContains(lines, "# SAMPLES COLLECTED RECEIVED CONDITION");
        assertContains(lines, "1 Serum 01/10/26 08:10 01/10/26 08:50 Fasting");
        assertContains(lines, "2 Urine Not recorded 01/10/26 09:00");
        assertContains(lines, "3 Whole blood 01/10/26 07:55 01/10/26 09:00 Rejected: Haemolysed");
        assertFalse("the run-on collection line is gone",
                lines.stream().anyMatch(line -> line.contains("Specimen") || line.contains("collection times")));
    }

    @Test
    public void flagsPrintAnArrowAndTheLanguagesLetterAndTheLegendExplainsThem() throws Exception {
        List<ClinicalPatientData> rows = new ArrayList<>();
        rows.add(row(ORDER, "Chemistry", "Sodium", "128", "B"));
        rows.add(row(ORDER, "Chemistry", "Glucose", "6.3", "E"));
        rows.add(row(ORDER, "Chemistry", "Potassium", "2.1", "BB"));
        rows.add(row(ORDER, "Chemistry", "Calcium", "4.1", "EE"));
        rows.add(row(ORDER, "Serology", "HIV screen", "Reactive", "*"));
        String text = PdfText.of(render(rows, Map.of(ORDER, order())));

        assertTrue(text, text.contains("128 ↓ L"));
        assertTrue(text, text.contains("6.3 ↑ H"));
        assertTrue(text, text.contains("2.1 ⇊ LL"));
        assertTrue(text, text.contains("4.1 ⇈ HH"));
        assertTrue(text, text.contains("Reactive *"));
        assertTrue(text, text.contains("CRITICAL"));
        assertTrue(text, text.contains("Legend: ↑ H above normal ↓ L below normal ⇈ HH / ⇊ LL critical"));
        assertFalse("no French letters on an English report", text.contains(" E ") || text.contains(" BB"));
    }

    @Test
    public void aDeploymentRewordingPrintsOnTheReport() throws Exception {
        Path directory = overrides.getRoot().toPath();
        Files.writeString(directory.resolve("fr.json"), "{\"report.patient.flag.low\": \"B\","
                + " \"report.patient.flag.high\": \"E\", \"label.results.result\": \"Valeur\"}");
        UiMessages french = UiMessages.forLocale(Locale.FRENCH, directory);
        List<ClinicalPatientData> rows = List.of(row(ORDER, "Chimie", "Sodium", "128", "B"),
                row(ORDER, "Chimie", "Glucose", "6.3", "E"));

        String text = PdfText.of(PatientResultsPdf.render(settings(), french, rows, Map.of(ORDER, order())));

        assertTrue(text, text.contains("128 ↓ B"));
        assertTrue(text, text.contains("6.3 ↑ E"));
        assertTrue(text, text.contains("VALEUR"));
        assertTrue("the French catalogue's own words", text.contains("ANALYSE"));
    }

    @Test
    public void aLongerLanguageWrapsColumnHeadingsBetweenWordsNeverInsideOne() throws Exception {
        UiMessages french = UiMessages.forLocale(Locale.FRENCH, null);
        String flag = french.get("label.results.flag").toUpperCase(Locale.FRENCH);

        String text = PdfText.of(PatientResultsPdf.render(settings(), french, rows(ORDER), Map.of(ORDER, order())));

        assertTrue(flag + " in " + text, text.contains(flag));
    }

    @Test
    public void methodColumnShowsTheMethodOrThePerformingLabWhichTheNoteDoesNotRepeat() throws Exception {
        ClinicalPatientData local = row(ORDER, "Chemistry", "Glucose", "6.3", null);
        local.setMethodName("Hexokinase");
        ClinicalPatientData referred = row(ORDER, "Chemistry", "Vitamin D", "48", null);
        referred.setPerformingLabName("Kokopo Reference Lab");
        referred.setNote("Sent frozen");
        String text = PdfText.of(render(List.of(local, referred), Map.of(ORDER, order())));

        assertTrue(text, text.contains("Glucose (Serum) 1 Hexokinase 6.3"));
        assertTrue(text, text.contains("Vitamin D (Serum) 1 Performed by"));
        assertTrue(text, text.contains("Note: Sent frozen"));
        assertEquals("the note does not repeat the lab: " + text, 1, text.split("Kokopo", -1).length - 1);
    }

    /**
     * "million/mm3" is wider than the Units column at the report's size and broke
     * inside the word, printing "million/m" over "m3".
     */
    @Test
    public void aUnitOrRangeWiderThanItsColumnShrinksInsteadOfBreakingInsideAWord() throws Exception {
        ClinicalPatientData rbc = row(ORDER, "Hematology", "Red Blood Cells", "5.00", null);
        rbc.setUom("million/mm3");
        rbc.setTestRefRange("1000000.00-2000000.00");
        ClinicalPatientData panel = row(ORDER, "Hematology", "Blood count", "", null);
        panel.setComponents(List.of(new ReportComponentLine("Red cells", "5.00", "", "4.80-5.50", "million/mm3")));

        List<String> lines = lines(PdfText.of(render(List.of(rbc, panel), Map.of(ORDER, order()))));

        assertContains(lines, "Red Blood Cells (Serum) 1 5.00 1000000.00-2000000.00 million/mm3");
        assertContains(lines, "Red cells 5.00 4.80-5.50 million/mm3");
    }

    @Test
    public void multiComponentTestPrintsOneAlignedLinePerComponent() throws Exception {
        ClinicalPatientData lipids = row(ORDER, "Chemistry", "Lipid panel", "Total: 6.1\nHDL: 0.8", null);
        lipids.setComponents(List.of(new ReportComponentLine("Total cholesterol", "6.1", "E", "3.0 - 5.2", "mmol/L"),
                new ReportComponentLine("HDL", "0.8", "", "1.0 - 2.0", "mmol/L")));
        List<String> lines = lines(PdfText.of(render(List.of(lipids), Map.of(ORDER, order()))));

        assertContains(lines, "Lipid panel (Serum) 1");
        assertContains(lines, "Total cholesterol 6.1 ↑ H 3.0 - 5.2 mmol/L");
        assertContains(lines, "HDL 0.8 1.0 - 2.0 mmol/L");
        assertFalse(lines.toString(), lines.stream().anyMatch(line -> line.contains("Total: 6.1")));
    }

    @Test
    public void pendingAndRejectedRowsSayWhyThereIsNoResult() throws Exception {
        ClinicalPatientData pending = row(ORDER, "Chemistry", "Urea", "", null);
        pending.setReleased(false);
        pending.setCompletedTime(null);
        pending.setAnalysisStatus("Awaiting validation");
        ClinicalPatientData sampleRejected = row(ORDER, "Chemistry", "Potassium", "", null);
        sampleRejected.setSampleRejected(true);
        ClinicalPatientData testRejected = row(ORDER, "Chemistry", "Creatinine", "", null);
        testRejected.setReleased(false);
        testRejected.setTestRejected(true);
        testRejected.setNote("Clotted specimen");
        List<String> lines = lines(
                PdfText.of(render(List.of(pending, sampleRejected, testRejected), Map.of(ORDER, order()))));

        assertContains(lines, "Urea (Serum) 1 Awaiting validation");
        assertContains(lines, "Potassium (Serum) 1 Sample rejected");
        assertContains(lines, "Creatinine (Serum) 1 Rejected");
        assertContains(lines, "Note: Clotted specimen");
        assertFalse("a rejected sample has no completion time",
                lines.stream().anyMatch(line -> line.startsWith("Potassium") && line.contains("11:40")));
    }

    @Test
    public void eachReleasedTestShowsWhenItWasCompleted() throws Exception {
        ClinicalPatientData glucose = row(ORDER, "Chemistry", "Glucose", "6.3", null);
        glucose.setCompletedTime("01/10/26 10:15");
        List<String> lines = lines(PdfText.of(render(List.of(glucose), Map.of(ORDER, order()))));

        assertContains(lines, "Glucose (Serum) 1 6.3 3.9 - 5.8 mmol/L 01/10/26 10:15");
    }

    @Test
    public void criticalRowCarriesItsLatestCallback() throws Exception {
        ClinicalPatientData potassium = row(ORDER, "Chemistry", "Potassium", "6.8", "EE");
        potassium.setCallback(new ReportCallbackLine("Dr Mek, Ward 4", "01/10/26 10:20", "Sarah Chen", "CONFIRMED", 1));
        String text = PdfText.of(render(List.of(potassium), Map.of(ORDER, order())));

        assertTrue(text, text.contains(
                "Callback: Dr Mek, Ward 4, 01/10/26 10:20, by Sarah Chen. Read back confirmed. (+1 earlier attempts)"));
    }

    @Test
    public void testsOutsideTheAccreditedScopeCarryADaggerTheLegendExplains() throws Exception {
        ClinicalPatientData accredited = row(ORDER, "Chemistry", "Glucose", "6.3", null);
        ClinicalPatientData outside = row(ORDER, "Chemistry", "Vitamin D", "48", null);
        outside.setInAccreditedScope(false);
        String marked = PdfText.of(render(List.of(accredited, outside), Map.of(ORDER, order())));
        String unmarked = PdfText.of(render(List.of(accredited), Map.of(ORDER, order())));

        assertTrue(marked, marked.contains("Vitamin D (Serum) †"));
        assertFalse(marked, marked.contains("Glucose (Serum) †"));
        assertTrue(marked, marked.contains("† Not within the laboratory's accredited scope"));
        assertFalse(unmarked, unmarked.contains("†"));
        for (String size : List.of("Letter", "A4")) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, size);
            byte[] pdf = render(List.of(accredited, outside), Map.of(ORDER, order()));
            float rightMargin = PdfText.pageWidth(pdf, 1) - 30;
            assertTrue(size + ": the legend stays inside the margin", PdfText.rightmostGlyph(pdf, 1) <= rightMargin);
        }
    }

    @Test
    public void lowBarsAreDashedAndHighBarsSolidSoAPhotocopyTellsThemApart() throws Exception {
        String drawn = PdfText
                .content(render(List.of(row(ORDER, "Chemistry", "Sodium", "128", "B")), Map.of(ORDER, order())), 1);

        assertTrue("butt caps keep the dash gaps open: " + drawn, drawn.contains("0 J\n[2 2] 0 d"));
    }

    @Test
    public void correctedReportNamesTheReportItReplaces() throws Exception {
        PatientReportOrder order = order();
        order.setCorrected(true);
        order.setReplacedIssueTime("01/10/26 12:00");
        String text = PdfText.of(render(rows(ORDER), Map.of(ORDER, order)));

        assertTrue(text, text.contains(
                "Corrected report. Replaces the report issued 01/10/26 12:00. Change: See the replaced report."));
    }

    @Test
    public void signOffNamesTheValidatorsThenTheReportEnds() throws Exception {
        PatientReportOrder order = order();
        order.setValidators(List.of("Sarah Chen, 01/10/26 10:15", "Ben Okello, 01/10/26 11:40"));
        ClinicalPatientData row = row(ORDER, "Chemistry", "Glucose", "6.3", null);
        row.setConclusion("Consistent with diabetes");
        PatientResultsPdf.Settings settings = settings(logo(300, 300), null);
        List<String> lines = lines(
                PdfText.of(PatientResultsPdf.render(settings, ENGLISH, List.of(row), Map.of(ORDER, order))));

        assertContains(lines, "GENERAL COMMENTS VALIDATED AND RELEASED BY");
        assertContains(lines, "Consistent with diabetes");
        assertContains(lines, "Sarah Chen, 01/10/26 10:15");
        assertContains(lines, "Ben Okello, 01/10/26 11:40");
        int end = lines.indexOf("End of report");
        assertTrue(lines.toString(), end > 0);
        assertTrue("the report ends after the sign-off", end > lines.stream()
                .filter(line -> line.contains("Ben Okello")).map(lines::indexOf).findFirst().orElse(Integer.MAX_VALUE));
    }

    @Test
    public void theDirectorsSignatureStartsBelowTheValidators() throws Exception {
        PatientReportOrder order = order();
        order.setValidators(List.of("Sarah Chen, 01/10/26 10:15"));
        PatientResultsPdf.Settings settings = new PatientResultsPdf.Settings("Central Public Health Laboratory", null,
                null, null, true, "URAP Number", false, false, logo(600, 200), "Dr Jane Kila", null);

        byte[] pdf = PatientResultsPdf.render(settings, ENGLISH, rows(ORDER), Map.of(ORDER, order));

        float validator = PdfText.baselineOf(pdf, 1, "Sarah Chen");
        assertTrue("validator line found", validator > 0);
        assertTrue(PdfImages.drawnTops(pdf, 1).toString(),
                PdfImages.drawnTops(pdf, 1).stream().anyMatch(top -> top > validator && top < validator + 20));
    }

    @Test
    public void aTypicalOrderFitsOnOnePageOnLetterAndA4() throws Exception {
        for (String size : List.of("Letter", "A4")) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, size);
            List<ClinicalPatientData> rows = new ArrayList<>();
            String[] sections = { "Hematology", "Chemistry", "Serology", "Urinalysis" };
            for (int i = 0; i < 34; i++) {
                ClinicalPatientData row = row(ORDER, sections[i / 9], "Analyte " + i, "12.5", i % 7 == 0 ? "E" : null);
                if (i == 3 || i == 20) {
                    row.setNote("Repeat on a fresh sample if symptoms persist");
                }
                if (i == 14) {
                    row.setFlagCode("EE");
                    row.setCallback(new ReportCallbackLine("Dr Mek", "01/10/26 10:20", "Sarah Chen", "CONFIRMED", 0));
                }
                rows.add(row);
            }
            PatientReportOrder order = order();
            order.setSamples(List.of(new ReportSampleLine("1", "Serum", "01/10/26 08:10", "01/10/26 08:50", null, null),
                    new ReportSampleLine("2", "Urine", "01/10/26 08:20", "01/10/26 08:50", null, null)));
            order.setValidators(List.of("Sarah Chen, 01/10/26 10:15"));

            byte[] pdf = render(rows, Map.of(ORDER, order));
            PdfRegression.save(pdf, "patient-report-v1-" + size);

            assertEquals(size, 1, PdfText.pageCount(pdf));
            String text = PdfText.of(pdf);
            assertTrue(size, text.contains("End of report") && text.contains("Page 1 of 1"));
        }
    }

    @Test
    public void continuationPagesCarryTheCompactHeaderTheStripAndTheColumnHeader() throws Exception {
        ConfigurationProperties.getInstance().setPropertyValue(Property.REPORT_PAPER_SIZE, "Letter");
        List<ClinicalPatientData> rows = new ArrayList<>();
        for (int i = 0; i < 95; i++) {
            ClinicalPatientData row = row(ORDER, "Long Section", "Test " + i, "12.5", "B");
            row.setNote("Note " + i);
            rows.add(row);
        }
        PatientReportOrder order = order();
        order.setSamples(List.of(new ReportSampleLine("1", "Serum", "01/10/26 08:10", "01/10/26 08:50", null, null),
                new ReportSampleLine("2", "Urine", "01/10/26 07:55", "01/10/26 08:50", null, null),
                new ReportSampleLine("3", "Whole blood", "01/10/26 09:05", "01/10/26 09:10", null, null)));
        order.setEarliestCollection("01/10/26 07:55");
        order.setCollectionsDiffer(true);

        byte[] pdf = render(rows, Map.of(ORDER, order));
        int pages = PdfText.pageCount(pdf);

        assertTrue("must paginate", pages > 1);
        for (int page = 1; page <= pages; page++) {
            String text = PdfText.ofPage(pdf, page);
            assertTrue(text, text.contains("Page " + page + " of " + pages));
            assertTrue(text, text.contains("TEST SMP METHOD RESULT FLAG REFERENCE RANGE UNITS COMPLETED"));
            assertTrue(text, text.contains("Legend:"));
            if (page > 1) {
                assertTrue(text, text.contains("Central Public Health Laboratory Results: final report"));
                assertTrue(text, text.contains("Doe, Jane · PAT-0001 · NID-778 · " + ORDER
                        + " · Dr Prescriber · Central Clinic / Outpatients · Collected 01/10/26 07:55 (first of 3 samples)"));
                assertFalse("the card prints on page 1 only", text.contains("SAMPLES"));
            }
        }
        PdfRegression.save(pdf, "patient-report-v1-continuation");
    }

    @Test
    public void eachOrderStartsANewPageNumberedFromOne() throws Exception {
        String second = "DEV0126000000000962";
        List<ClinicalPatientData> rows = new ArrayList<>(rows(ORDER));
        rows.addAll(rows(second));
        Map<String, PatientReportOrder> orders = new LinkedHashMap<>();
        orders.put(ORDER, order());
        orders.put(second, order());

        byte[] pdf = render(rows, orders);

        assertEquals(2, PdfText.pageCount(pdf));
        for (int page = 1; page <= 2; page++) {
            assertTrue(PdfText.ofPage(pdf, page).contains("Page 1 of 1"));
        }
        assertTrue(PdfText.ofPage(pdf, 2).contains(second));
    }

    @Test
    public void logosAndMarksFitTheirSquareSlotsWithoutDistortion() throws Exception {
        PatientReportOrder order = order();
        order.setAccreditationLogos(List.of(logo(300, 300), logo(600, 200)));
        byte[] pdf = PatientResultsPdf.render(settings(logo(300, 300), logo(600, 200)), ENGLISH, rows(ORDER),
                Map.of(ORDER, order));

        List<String> sizes = PdfImages.drawnSizes(pdf, 1);

        assertTrue(sizes.toString(), sizes.contains("48x48"));
        assertTrue(sizes.toString(), sizes.contains("48x16"));
        assertTrue(sizes.toString(), sizes.contains("32x32"));
        assertTrue(sizes.toString(), sizes.contains("32x11"));
    }

    @Test
    public void theRoutinePatientReportDrawsItsRowsWithThisLayout() throws Exception {
        PatientCILNSPClinical_vreduit report = new PatientCILNSPClinical_vreduit();
        report.initializeReport(new ReportForm());
        report.reportItems = new ArrayList<>(rows(ORDER));

        List<String> lines = lines(PdfText.of(report.renderReport()));

        assertContains(lines, "TEST SMP METHOD RESULT FLAG REFERENCE RANGE UNITS COMPLETED");
        assertContains(lines, "Glucose (Serum) 1 6.3 ↑ H 3.9 - 5.8 mmol/L");
    }

    private static byte[] render(List<ClinicalPatientData> rows, Map<String, PatientReportOrder> orders) {
        return PatientResultsPdf.render(settings(), ENGLISH, rows, orders);
    }

    private static PatientResultsPdf.Settings settings() {
        return settings(null, null);
    }

    private static PatientResultsPdf.Settings settings(byte[] leftLogo, byte[] rightLogo) {
        return new PatientResultsPdf.Settings("Central Public Health Laboratory", "PO Box 12, Port Moresby", leftLogo,
                rightLogo, true, "URAP Number", false, false, null, "Dr Jane Kila", "Laboratory Director");
    }

    private static PatientReportOrder order() {
        PatientReportOrder order = new PatientReportOrder();
        order.setSamples(List.of(new ReportSampleLine("1", "Serum", "01/10/26 08:10", "01/10/26 08:50", null, null)));
        order.setCompletedTime("01/10/26 11:40");
        order.setEarliestCollection("01/10/26 08:10");
        return order;
    }

    private static List<ClinicalPatientData> rows(String accession) {
        return List.of(row(accession, "Hematology", "Hemoglobin", "10.2", "B"),
                row(accession, "Biochemistry", "Glucose", "6.3", "E"));
    }

    private static ClinicalPatientData row(String accession, String section, String test, String result, String flag) {
        ClinicalPatientData data = new ClinicalPatientData();
        data.setSubjectNumber("PAT-0001");
        data.setAccessionNumber(accession);
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
        data.setTestSection(section);
        data.setTestName(test + " (Serum)");
        data.setSampleSortOrder("1");
        data.setResult(result);
        data.setFlagCode(flag);
        data.setTestRefRange("Glucose".equals(test) ? "3.9 - 5.8" : "12.0 - 16.0");
        data.setUom("Glucose".equals(test) ? "mmol/L" : "g/dL");
        data.setReleased(true);
        return data;
    }

    private static byte[] logo(int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    private static List<String> lines(String text) {
        return Arrays.asList(text.split("\n"));
    }

    private static void assertContains(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.stream().anyMatch(line -> line.contains(expected)));
    }
}
