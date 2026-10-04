package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.ARVReportData;
import org.openelisglobal.testsupport.PdfText;

public class StudyArvResultsPdfTest extends BaseWebContextSensitiveTest {

    private static final StudyArvResultsPdf.Settings SETTINGS = new StudyArvResultsPdf.Settings("Suivi ARV",
            StudyArvResultsPdf.Images.NONE);

    @Test
    public void versionOne_drawsOnlyThePanelsAnOrderHasResultsFor() throws Exception {
        ARVReportData order = order();
        order.setCd4per("28");
        order.setCd4("640");

        String text = PdfText.of(StudyArvResultsPdf.versionOne(List.of(order), SETTINGS));

        List<String> lines = lines(text);
        assertLine(lines, "Suivi ARV");
        assertTrue(text, text.contains("Sujetno: SUBJ-0042"));
        assertLine(lines, "Immunologie : Phénotypage lymphocytaire");
        assertLine(lines, "Méthode: Cytométrie de flux");
        assertLine(lines, "CD4(%) CD4#(cell/µl)");
        assertLine(lines, "28 640");
        assertFalse(text, text.contains("Hématologie : Hémogramme"));
        assertFalse(text, text.contains("Biochimie:"));
        assertFalse(text, text.contains("Sérologie VIH"));
        assertFalse("an order without non-conformities has no checklist: " + text,
                text.contains("RAPPORT DE NON-CONFORMITE CLIENT"));
    }

    @Test
    public void versionOne_listsTheResultsOfEachPanelWithTheSampleState() throws Exception {
        ARVReportData order = fullOrder();
        order.setHematologyQaEvent("Hémolysé");

        String text = PdfText.of(StudyArvResultsPdf.versionOne(List.of(order), SETTINGS));

        List<String> lines = lines(text);
        assertLine(lines, "Hématologie : Hémogramme");
        assertLine(lines, "Etat de l'échantillon:Hémolysé");
        assertLine(lines, "GB(10^3/ul) GR(10^6/ul) Hb(g/dl) Hct(%) VGM(fl) CCMH(fl) TCMH(pg)");
        assertLine(lines, "6.2 4.5 12.9 38 88 33 29");
        assertLine(lines, "Plaq(10^3/ul) N(%) L(%) M(%) Eo(%) Ba(%)");
        assertLine(lines, "250 55 35 6 3 1");
        assertLine(lines, "Créatinine(mg/l) SGPT(UI/L) SGOT(UI/L) Glycémie(g/l)");
        assertLine(lines, "9.1 22 25 0.9");
        assertLine(lines, "Etat de l'échantillon:Normal");
        assertLine(lines, "Positif VIH1");
        assertLine(lines, "Valeurs de Référence");

        order.setVih("En cours");
        String pending = PdfText.of(StudyArvResultsPdf.versionOne(List.of(order), SETTINGS));
        assertFalse("a serology still in progress is left out: " + pending, pending.contains("Sérologie VIH"));
    }

    @Test
    public void versionOne_putsAnOrdersNonConformitiesOnTheirOwnPage() throws Exception {
        ARVReportData order = fullOrder();
        order.setHematologyQaEvent("Hémolysé");
        order.setAllQaEvents("sample.type.edtaTube:qa_event.hemolytic");

        byte[] pdf = StudyArvResultsPdf.versionOne(List.of(order), SETTINGS);

        assertEquals(2, PdfText.pageCount(pdf));
        List<String> checklist = lines(PdfText.ofPage(pdf, 2));
        assertLine(checklist, "RAPPORT DE NON-CONFORMITE CLIENT");
        assertLine(checklist, "Echantillon Hémolysé X");
        for (int page = 1; page <= 2; page++) {
            assertLine(lines(PdfText.ofPage(pdf, page)), "Valeurs de Référence");
        }
    }

    @Test
    public void versionTwo_listsEachResultWithItsReferenceValues() throws Exception {
        ARVReportData order = fullOrder();
        order.setVgm(null);
        order.setGlyc(null);

        List<String> lines = lines(PdfText.of(StudyArvResultsPdf.versionTwo(List.of(order), SETTINGS)));

        assertLine(lines, "Suivi ARV");
        assertLine(lines, "H Valeurs de référence F");
        assertLine(lines, "Globules blancs (10^3/ul) 6.2 4 - 10 4 - 10");
        assertLine(lines, "VGM(fl) X 80 - 95 80 - 95");
        assertLine(lines, "TCMH(pg) 29 25 - 27 25 - 27");
        assertLine(lines, "CCMH(%) 33 32 - 36 32 - 36");
        assertLine(lines, "Glycémie(g/l) X 0.60 - 1.1 g/l 0.60 – 1.1 g/l");
        assertLine(lines, "CD4#(cel/ul) 640 500 - 1600");
        assertLine(lines, "Ampli2 < LL copies/ml <LL");
    }

    @Test
    public void versionTwo_showsSerologyAndPcrOnlyForAnOrderThatHasThem() throws Exception {
        ARVReportData order = fullOrder();
        String hidden = PdfText.of(StudyArvResultsPdf.versionTwo(List.of(order), SETTINGS));
        order.setShowSerologie(Boolean.TRUE);
        order.setShowPCR(Boolean.TRUE);
        String shown = PdfText.of(StudyArvResultsPdf.versionTwo(List.of(order), SETTINGS));

        assertFalse(hidden, hidden.contains("Statut sérologique"));
        assertFalse(hidden, hidden.contains("PCR"));
        assertLine(lines(shown), "Statut sérologique Positif VIH1");
        assertLine(lines(shown), "PCR (Réalisé sur Cobas Taqman - Roche) : Négatif");
    }

    private static ARVReportData order() {
        ARVReportData order = new ARVReportData();
        order.setLabNo("DEV0126000000000961");
        order.setSubjectNumber("SUBJ-0042");
        order.setBirth_date("12/03/1992");
        order.setAge("34");
        order.setGender("F");
        order.setCollectiondate("01/10/2026 08:40");
        order.setReceptiondate("01/10/2026");
        order.setOrgname("Central Clinic");
        order.setDoctor("Dr Prescriber");
        return order;
    }

    private static ARVReportData fullOrder() {
        ARVReportData order = order();
        order.setGb("6.2");
        order.setGr("4.5");
        order.setHb("12.9");
        order.setHct("38");
        order.setVgm("88");
        order.setCcmh("33");
        order.setTcmh("29");
        order.setPlq("250");
        order.setNper("55");
        order.setLper("35");
        order.setMper("6");
        order.setEoper("3");
        order.setBper("1");
        order.setCd4per("28");
        order.setCd4("640");
        order.setCreatininemie("9.1");
        order.setSgpt("22");
        order.setSgot("25");
        order.setGlyc("0.9");
        order.setVih("Positif VIH1");
        order.setAmpli2("< LL");
        order.setAmpli2lo("< LL");
        order.setPcr("Négatif");
        return order;
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
