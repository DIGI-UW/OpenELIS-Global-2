package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reports.action.implementation.reportBeans.VLReportData;
import org.openelisglobal.testsupport.PdfText;

public class StudyVlResultsPdfTest extends BaseWebContextSensitiveTest {

    private static final StudyVlResultsPdf.Settings SETTINGS = new StudyVlResultsPdf.Settings("Charge Virale",
            StudyVlResultsPdf.Images.NONE);

    @Test
    public void describesTheExaminationTheViralLoadAndTheDetectionThresholds() throws Exception {
        String text = PdfText.of(StudyVlResultsPdf.render(List.of(order()), SETTINGS));

        List<String> lines = lines(text);
        assertLine(lines, "Charge Virale");
        for (String field : new String[] { "Sujetno: SUBJ-0042", "Labno: DEV0126000000000961", "Grossesse: Non",
                "Allaitement: Oui", "Date de validation: 05/10/2026", "Site: Central Clinic" }) {
            assertTrue(field + " in " + lines, text.contains(field));
        }
        assertLine(lines, "Type d’Examen : Recherche de l’ARN du virus HIV-1 par amplification génétique (PCR) "
                + "et par quantification");
        assertLine(lines, "Type de prélèvement : Plasma");
        assertLine(lines, "Etat de l’échantillon : Normal");
        assertLine(lines, "Virologie Résultats nombre de copies /mL Résultats Log /mL");
        assertLine(lines, "HIV-1 12500 4.10");
        assertTrue("every interpretation prints: " + lines, text.contains("le patient est dit en échec virologique"));
        assertLine(lines, "Seuil de détection de la technique CV/PL : 20 copies /mL");
        assertLine(lines, "Valeur charge virale : < LL=virus HIV-1 indétectable dans le sang périphérique");
        assertFalse(text, text.contains("RAPPORT DE NON-CONFORMITE CLIENT"));
    }

    @Test
    public void marksAMissingResultWithAnX() throws Exception {
        VLReportData order = order();
        order.setAmpli2(null);
        order.setAmpli2lo(null);

        assertLine(lines(PdfText.of(StudyVlResultsPdf.render(List.of(order), SETTINGS))), "HIV-1 X X");
    }

    @Test
    public void putsAViralLoadNonConformityOnAChecklistPage() throws Exception {
        VLReportData order = order();
        order.setVirologyVlQaEvent("Hémolysé");
        order.setAllQaEvents("sample.type.edtaTube:qa_event.hemolytic");
        order.setReceptionQaEvent("Fiche incomplète");

        byte[] pdf = StudyVlResultsPdf.render(List.of(order), SETTINGS);

        assertEquals(2, PdfText.pageCount(pdf));
        assertLine(lines(PdfText.ofPage(pdf, 1)), "Etat de l’échantillon : Hémolysé");
        String page = PdfText.ofPage(pdf, 2);
        List<String> checklist = lines(page);
        assertTrue("the checklist page identifies the patient: " + checklist,
                page.contains("Sujetno: SUBJ-0042") && page.contains("Labno: DEV0126000000000961"));
        assertLine(checklist, "RAPPORT DE NON-CONFORMITE CLIENT");
        assertLine(checklist, "Echantillon Hémolysé X");
        assertLine(checklist, "Section: Saisie Réception X Biochimie Immunologie(CD4) Charge virale X");
        for (int number = 1; number <= 2; number++) {
            assertLine(lines(PdfText.ofPage(pdf, number)), "Seuil de détection de la technique CV/PL : 20 copies /mL");
        }
    }

    private static VLReportData order() {
        VLReportData order = new VLReportData();
        order.setSubjectno("SUBJ-0042");
        order.setAccessionNumber("DEV0126000000000961");
        order.setGender("F");
        order.setBirth_date("12/03/1992");
        order.setAge("34");
        order.setVlPregnancy("pregnancy=Non");
        order.setVlSuckle("suckle=Oui");
        order.setCollectiondate("01/10/2026 08:40");
        order.setReceptiondate("02/10/2026 09:15");
        order.setDoctor("Dr Prescriber");
        order.setServicename("Central Clinic");
        order.setCompleationdate("03/10/2026");
        order.setReleasedate("05/10/2026");
        order.setSampleTypeName("Plasma");
        order.setvih("HIV-1");
        order.setAmpli2("12500");
        order.setAmpli2lo("4.10\n");
        return order;
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
