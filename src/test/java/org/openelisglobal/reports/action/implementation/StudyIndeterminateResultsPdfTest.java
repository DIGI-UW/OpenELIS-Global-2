package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.IndeterminateReportData;
import org.openelisglobal.testsupport.PdfText;

public class StudyIndeterminateResultsPdfTest extends BaseWebContextSensitiveTest {

    private static final String STUDY = "Résultats Indéterminés";

    @Test
    public void versionOne_listsTheAlgorithmTestsTheOrderHasResultsFor() throws Exception {
        IndeterminateReportData order = order("DEV0126000000000961", "Central Clinic", "Dr Prescriber");
        order.setIntegral("Positif");
        order.setMurex("Négatif");
        order.setWb1("Indéterminé");

        String text = PdfText.of(StudyIndeterminateResultsPdf.versionOne(List.of(order), STUDY));

        List<String> lines = lines(text);
        assertLine(lines, STUDY);
        assertTrue(text, text.contains("Sujetno: SUBJ-0042"));
        assertTrue(text, text.contains("Labno: DEV0126000000000961"));
        assertLine(lines, "Résultats des tests de l’algorithme utilisé");
        assertLine(lines, "Test Résultat");
        assertLine(lines, "Enzygnost HIV Integral II Positif");
        assertLine(lines, "Murex HIV-1.2.O Négatif");
        assertLine(lines, "New Lav Blot I Indéterminé");
        assertFalse("a test without a result is left out: " + lines, text.contains("Vironstika"));
        assertLine(lines, "Conclusion U Le biologiste");
        assertLine(lines, "LEXIQUE");
        assertLine(lines, DateUtil.getCurrentDateAsText());
    }

    @Test
    public void versionTwo_givesTheSerologyStatus() throws Exception {
        String text = PdfText.of(StudyIndeterminateResultsPdf
                .versionTwo(List.of(order("DEV0126000000000961", "Central Clinic", "Dr Prescriber")), STUDY));

        List<String> lines = lines(text);
        assertLine(lines, "SEROLOGIE");
        assertLine(lines, "Sérologie VIH (Algorithme en parallèle : Enzygnost Integral II et Murex 1.2.0)");
        assertLine(lines, "Statut sérologique U");
        assertLine(lines, "Le Biologiste");
        assertLine(lines, "LEXIQUE");
        assertLine(lines, DateUtil.getCurrentDateAsText());
    }

    @Test
    public void byLocation_groupsTheOrdersByServiceDoctorAndReceptionDay() throws Exception {
        List<IndeterminateReportData> orders = List.of(order("DEV0126000000000961", "Central Clinic", "Dr A"),
                order("DEV0126000000000962", "Central Clinic", "Dr A"),
                order("DEV0126000000000970", "North Clinic", "Dr B"));

        String text = PdfText.of(StudyIndeterminateResultsPdf.byLocation(orders, STUDY));

        List<String> lines = lines(text);
        assertLine(lines, "SEROLOGIE");
        assertEquals("one heading per group: " + lines, 2,
                lines.stream().filter(line -> line.startsWith("Service ")).count());
        assertLine(lines, "Service Central Clinic");
        assertLine(lines, "Médecin Dr A");
        assertLine(lines, "Date de réception 02/10/2026 09:15");
        assertLine(lines, "Lab No Sujet No Date de prélèvement Result Sérologie VIH");
        assertLine(lines, "DEV0126000000000961 SUBJ-0042 01/10/2026 08:40 U");
        assertLine(lines, "DEV0126000000000962 SUBJ-0042 01/10/2026 08:40 U");
        assertLine(lines, "Service North Clinic");
        assertLine(lines, "LEXIQUE");
        assertLine(lines, DateUtil.getCurrentDateAsText());
    }

    @Test
    public void byLocation_repeatsReportIdentityAboveTheTableOnEveryPage() throws Exception {
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        String originalPaper = config.getPropertyValue(Property.REPORT_PAPER_SIZE);
        String originalSite = config.getPropertyValue(Property.SiteName);
        try {
            config.setPropertyValue(Property.SiteName, "Indeterminate Test Laboratory");
            List<IndeterminateReportData> orders = new ArrayList<>();
            for (int i = 0; i < 140; i++) {
                orders.add(order(String.format("LAB-%03d", i), "Central Clinic", "Dr A"));
            }
            for (String paper : List.of("A4", "Letter")) {
                config.setPropertyValue(Property.REPORT_PAPER_SIZE, paper);
                byte[] pdf = StudyIndeterminateResultsPdf.byLocation(orders, STUDY);
                int pages = PdfText.pageCount(pdf);
                assertTrue("the report must overflow on " + paper, pages > 1);
                for (int page = 1; page <= pages; page++) {
                    String text = PdfText.ofPage(pdf, page);
                    List<String> lines = lines(text);
                    assertLine(lines, "Indeterminate Test Laboratory");
                    assertLine(lines, STUDY);
                    assertLine(lines, "SEROLOGIE");
                    assertLine(lines, "Sérologie VIH (Algorithme en parallèle : Enzygnost Integral II et Murex 1.2.0)");
                    assertLine(lines, "LEXIQUE");
                    assertLine(lines, DateUtil.getCurrentDateAsText());
                    assertTrue("report identity precedes the table: " + text,
                            text.indexOf("SEROLOGIE") < text.indexOf("Service Central Clinic"));
                }
                List<String> allLines = lines(PdfText.of(pdf));
                for (IndeterminateReportData order : orders) {
                    assertEquals("each order appears once: " + order.getLabNo(), 1,
                            allLines.stream().filter(line -> line.startsWith(order.getLabNo() + " ")).count());
                }
                String output = System.getProperty("reporting.pdf.output");
                if (output != null) {
                    Files.createDirectories(Path.of(output));
                    Files.write(Path.of(output, "indeterminate-location-" + paper + ".pdf"), pdf);
                }
            }
        } finally {
            config.setPropertyValue(Property.REPORT_PAPER_SIZE, originalPaper);
            config.setPropertyValue(Property.SiteName, originalSite);
        }
    }

    @Test
    public void byLocation_groupsInterleavedOrdersInFirstSeenGroupOrder() throws Exception {
        IndeterminateReportData laterReception = order("LAB-LATER", "Central Clinic", "Dr A");
        laterReception.setReceivedDate("03/10/2026 09:15");
        List<IndeterminateReportData> orders = List.of(order("LAB-A1", "Central Clinic", "Dr A"),
                order("LAB-B1", "North Clinic", "Dr B"), order("LAB-A2", "Central Clinic", "Dr A"),
                order("LAB-C1", "Central Clinic", "Dr C"), laterReception, order("LAB-B2", "North Clinic", "Dr B"));

        List<String> lines = lines(PdfText.of(StudyIndeterminateResultsPdf.byLocation(orders, STUDY)));

        assertEquals("one heading per service, doctor and reception timestamp", 4,
                lines.stream().filter(line -> line.startsWith("Service ")).count());
        assertEquals(List.of("Service Central Clinic", "Service North Clinic", "Service Central Clinic",
                "Service Central Clinic"), lines.stream().filter(line -> line.startsWith("Service ")).toList());
        assertEquals(List.of("LAB-A1", "LAB-A2", "LAB-B1", "LAB-B2", "LAB-C1", "LAB-LATER"),
                lines.stream().filter(line -> line.startsWith("LAB-")).map(line -> line.split(" ")[0]).toList());
        assertEquals(List.of("Médecin Dr A", "Médecin Dr B", "Médecin Dr C", "Médecin Dr A"),
                lines.stream().filter(line -> line.startsWith("Médecin ")).toList());
        assertLine(lines, "Date de réception 03/10/2026 09:15");
    }

    @Test
    public void byLocation_doesNotMergeDifferentGroupsContainingSeparators() throws Exception {
        List<IndeterminateReportData> orders = List.of(order("LAB-A", "Clinic|Annex", "Dr A"),
                order("LAB-B", "Clinic", "Annex|Dr A"));

        List<String> lines = lines(PdfText.of(StudyIndeterminateResultsPdf.byLocation(orders, STUDY)));

        assertEquals(2, lines.stream().filter(line -> line.startsWith("Service ")).count());
        assertLine(lines, "Service Clinic|Annex");
        assertLine(lines, "Médecin Dr A");
        assertLine(lines, "Service Clinic");
        assertLine(lines, "Médecin Annex|Dr A");
    }

    private static IndeterminateReportData order(String labNo, String service, String doctor) {
        IndeterminateReportData order = new IndeterminateReportData();
        order.setSubjectNumber("SUBJ-0042");
        order.setBirth_date("12/03/1992");
        order.setAge("34");
        order.setGender("F");
        order.setCollectiondate("01/10/2026 08:40");
        order.setReceivedDate("02/10/2026 09:15");
        order.setOrgname(service);
        order.setDoctor(doctor);
        order.setLabNo(labNo);
        order.setFinalResult("U");
        return order;
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
