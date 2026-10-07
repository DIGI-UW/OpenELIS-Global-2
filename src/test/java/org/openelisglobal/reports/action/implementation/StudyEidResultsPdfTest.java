package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.EIDReportData;
import org.openelisglobal.testsupport.PdfText;

public class StudyEidResultsPdfTest extends BaseWebContextSensitiveTest {

    private static final StudyEidResultsPdf.Settings SETTINGS = new StudyEidResultsPdf.Settings("Diagnostic Précoce",
            StudyEidResultsPdf.Images.NONE);

    @Test
    public void versionOne_describesTheExaminationAndTheResult() throws Exception {
        String text = PdfText.of(StudyEidResultsPdf.versionOne(List.of(order()), SETTINGS));

        List<String> lines = lines(text);
        assertLine(lines, "Diagnostic Précoce");
        for (String field : new String[] { "Numéro DBS : DBS-0042", "Labno : DEV0126000000000961",
                "Date Prél. : 01/10/2026", "Numéro Enfant Site : SITE-7", "Date de réception : 02/10/2026",
                "Age : -- Mois / 17 Semaines", "Date de Réalisation du test : 03/10/2026", "Médecin : Dr Prescriber",
                "Service : Central Clinic" }) {
            assertTrue(field + " in " + lines, text.contains(field));
        }
        assertLine(lines, "Type d’Examen : Recherche de l’ADN du virus HIV-1 par amplification génétique (PCR)");
        assertLine(lines, "Etat de l’échantillon : Normal (3 spots de sang collectés selon les procédures nationales)");
        assertLine(lines, "Virologie Résultats");
        assertLine(lines, "HIV-1 Négatif");
        assertLine(lines, "Rang de la PCR 1");
        assertTrue("every interpretation prints: " + lines,
                text.contains("En cas de résultats discordants entre la PCR1 et la PCR2"));
        assertTrue("every precaution prints: " + lines,
                text.contains("La performance de ce test a été évaluée uniquement sur HIV-1 group O et N"));
        assertFalse(text, text.contains("RAPPORT DE NON-CONFORMITE CLIENT"));
    }

    @Test
    public void versionOne_putsAnEarlyInfantDiagnosisNonConformityOnAChecklistPage() throws Exception {
        EIDReportData order = order();
        order.setVirologyEidQaEvent("Enfant > 18 mois");
        order.setAllQaEvents("-1:qa_event.adult;-1:qa_event.DBS_3");
        order.setReceptionQaEvent("Fiche incomplète");

        byte[] pdf = StudyEidResultsPdf.versionOne(List.of(order), SETTINGS);

        assertEquals(2, PdfText.pageCount(pdf));
        assertLine(lines(PdfText.ofPage(pdf, 1)),
                "Etat de l’échantillon : Enfant > 18 mois (3 spots de sang collectés selon les procédures nationales)");
        String page = PdfText.ofPage(pdf, 2);
        List<String> checklist = lines(page);
        assertTrue("the checklist page identifies the child: " + checklist,
                page.contains("Numéro DBS : DBS-0042") && page.contains("Labno : DEV0126000000000961"));
        assertLine(checklist, "RAPPORT DE NON-CONFORMITE CLIENT");
        assertLine(checklist, "Age de l’enfant > 18 mois X");
        assertLine(checklist, "DBS: Nombre de spot rempli < 3 X");
        assertLine(checklist, "Section: Saisie Réception X Biochimie Immunologie(CD4) Charge virale");
        assertLine(checklist, "Diagnostic précoce (EID) X Sérologie VIH Hématologie");
        assertLine(checklist, "Prière refaire le prélèvement sur :");
        assertLine(checklist, "Tube EDTA Tube sec X Carte DBS Whatman 903");
        org.openelisglobal.testsupport.PdfRegression.save(pdf, "eid-checklist");
    }

    @Test
    public void versionTwo_listsThePcrResultWithTheChildsClinic() throws Exception {
        EIDReportData order = order();
        order.setHiv_status("Positif");
        order.setPcr_type("Second PCR");

        String text = PdfText.of(StudyEidResultsPdf.versionTwo(List.of(order), SETTINGS));

        List<String> lines = lines(text);
        for (String field : new String[] { "Numero DBS Enfant: DBS-0042", "Labno: DEV0126000000000961",
                "Date Prél. : 01/10/2026 08:40", "Numéro CDV ou PTME: PTME-12", "Date de réception : 02/10/2026 09:15",
                "District sanitaire: Abidjan Sud", "Structure sanitaire: Clinique Centrale",
                "Service: Central Clinic" }) {
            assertTrue(field + " in " + lines, text.contains(field));
        }
        assertLine(lines, "Résultats PCR – dépistage Précoce Enfant VIH :");
        assertLine(lines, "Technique utilisée : DNA PCR");
        assertLine(lines, "Résultat du diagnostic PCR : Positif");
        assertLine(lines, "Rang de la PCR: 2");
        assertLine(lines, "Commentaire Laboratoire:");
        assertLine(lines, "LEXIQUE");
        assertLine(lines, DateUtil.getCurrentDateAsText());
    }

    @Test
    public void thePcrRankNamesAnUnknownOrUnusualRank() throws Exception {
        EIDReportData unknown = order();
        unknown.setPcr_type(null);
        EIDReportData third = order();
        third.setPcr_type("Third PCR");

        assertLine(lines(PdfText.of(StudyEidResultsPdf.versionTwo(List.of(unknown), SETTINGS))),
                "Rang de la PCR: Inconnu");
        assertLine(lines(PdfText.of(StudyEidResultsPdf.versionTwo(List.of(third), SETTINGS))),
                "Rang de la PCR: Third PCR");
    }

    private static EIDReportData order() {
        EIDReportData order = new EIDReportData();
        order.setSubjectno("DBS-0042");
        order.setSitesubjectno("SITE-7");
        order.setBirth_date("01/06/2026");
        order.setAgeWeek("17");
        order.setGender("F");
        order.setCollectiondate("01/10/2026 08:40");
        order.setAccessionNumber("DEV0126000000000961");
        order.setServicename("Central Clinic");
        order.setDoctor("Dr Prescriber");
        order.setCompleationdate("03/10/2026");
        order.setReceptiondate("02/10/2026 09:15");
        order.setPTME("PTME-12");
        order.setClinicDistrict("Abidjan Sud");
        order.setClinic("Clinique Centrale");
        order.setHiv_status("Négatif");
        order.setPcr_type("First PCR");
        return order;
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }
}
