package org.openelisglobal.reports.action.implementation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.FollowupRequiredData;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openelisglobal.testsupport.PdfText;

public class StudyNonConformityPdfTest extends BaseWebContextSensitiveTest {

    @Test
    public void notification_putsEachOrderOnItsOwnPageWithItsNonConformities() throws Exception {
        NonConformityReportData noSubjectNumber = nonConformity("DEV0126000000000970", "Reception", "Sample unlabelled",
                "Plasma", null);
        noSubjectNumber.setSubjectNumber(null);
        List<NonConformityReportData> items = List.of(
                nonConformity("DEV0126000000000961", "Hematology", "Haemolysed sample", "Whole blood",
                        "Recollect within 48 hours"),
                nonConformity("DEV0126000000000961", "Biochemistry", "Insufficient volume", "Serum", null),
                noSubjectNumber);

        byte[] pdf = StudyNonConformityPdf.notification(items);

        assertEquals(2, PdfText.pageCount(pdf));
        List<String> first = lines(PdfText.ofPage(pdf, 1));
        assertLine(first, "RAPPORT DE NON CONFORMITE: CLIENT");
        assertLine(first, "Date de survenue: 02/10/2026 Etude: ARV Followup");
        assertLine(first, "Date de prélèvement: 01/10/2026 Heure de prélèvement: 09:15");
        assertLine(first, "SubjetNo: SUBJ-0042 Service: Central Clinic");
        assertLine(first, "Labno: DEV0126000000000961 Prescripteur: Dr Prescriber");
        assertLine(first, "Motifs de non conformité");
        assertLine(first, "Section Date Motif du refus Type d'échantillon Biologiste");
        assertLine(first, "Hematology 02/10/2026 Haemolysed sample Whole blood Dr Biologist");
        assertLine(first, "Note: Recollect within 48 hours");
        assertLine(first, "Biochemistry 02/10/2026 Insufficient volume Serum Dr Biologist");
        assertLine(first, "Commentaire: Patient fasting");
        assertLine(first, "Nom et signature du client: ____________________ Date de transmission: ____________");
        assertLine(first,
                "Conserver une copie au secrétariat du laboratoire Date d'effet: " + DateUtil.getCurrentDateAsText());
        assertFalse("each order has its own page: " + first, String.join(" ", first).contains("Sample unlabelled"));

        List<String> second = lines(PdfText.ofPage(pdf, 2));
        assertLine(second, "SubjetNo: SITE-7 Service: Central Clinic");
        assertLine(second, "Reception 02/10/2026 Sample unlabelled Plasma Dr Biologist");
    }

    @Test
    public void followupRequired_listsEachServicesOrdersWithTheirNotes() throws Exception {
        FollowupRequiredData central = followup("Central Clinic", "DEV0126000000000961",
                "General comment: Call the clinic<br/>Whole blood : Hematology : Dr Biologist : Recollect<br/>",
                "Patient moved");
        FollowupRequiredData north = followup("North Clinic", "DEV0126000000000970",
                "Plasma : Reception : No authorizer : No note<br/>", null);

        String text = PdfText.of(StudyNonConformityPdf.followupRequired("Follow-up required - 01/10/2026 - 31/10/2026",
                List.of(central, north)));

        List<String> lines = lines(text);
        assertLine(lines, "Follow-up required - 01/10/2026 - 31/10/2026");
        assertLine(lines, "Service: Central Clinic");
        assertLine(lines, "Service: North Clinic");
        assertEquals("the column headings open each service: " + lines, 2, occurrences(text,
                "Date de prélèvement Date de réception Lab No Sujet No Site Sujet No Nom du médecin"));
        assertLine(lines, "01/10/2026 08:40 01/10/2026 09:15 DEV0126000000000961 SUBJ-0042 SITE-7 Dr Prescriber");
        assertLine(lines, "General comment: Call the clinic");
        assertLine(lines, "Whole blood : Hematology : Dr Biologist : Recollect");
        assertLine(lines, "Patient moved");
        assertLine(lines, "Plasma : Reception : No authorizer : No note");
        assertEquals("only orders with notes show the heading: " + lines, 2,
                occurrences(text, "Non Conformité Remarque"));
        assertEquals("only an order under investigation shows its follow-up: " + lines, 1,
                occurrences(text, "Suivi Requis Remarque"));
        assertFalse("markup never prints: " + text, text.contains("<br/>"));
        assertLine(lines, DateUtil.getCurrentDateAsText());
    }

    private static NonConformityReportData nonConformity(String accession, String section, String reason,
            String sampleType, String qaNote) {
        NonConformityReportData item = new NonConformityReportData();
        item.setAccessionNumber(accession);
        item.setSubjectNumber("SUBJ-0042");
        item.setSiteSubjectNumber("SITE-7");
        item.setStudy("ARV Followup");
        item.setService("Central Clinic");
        item.setReceivedDate("01/10/2026");
        item.setReceivedHour("09:15");
        item.setNonConformityDate("02/10/2026");
        item.setSection(section);
        item.setNonConformityReason(reason);
        item.setSampleType(sampleType);
        item.setBiologist("Dr Biologist");
        item.setQaNote(qaNote);
        item.setSampleNote("Patient fasting");
        item.setDoctor("Dr Prescriber");
        return item;
    }

    private static FollowupRequiredData followup(String service, String labNo, String nonConformityNotes,
            String underInvestigationNotes) {
        FollowupRequiredData item = new FollowupRequiredData();
        item.setOrgname(service);
        item.setLabNo(labNo);
        item.setSubjectNumber("SUBJ-0042");
        item.setSiteSubjectNumber("SITE-7");
        item.setCollectiondate("01/10/2026 08:40");
        item.setReceivedDate("01/10/2026 09:15");
        item.setDoctor("Dr Prescriber");
        item.setNonConformityNotes(nonConformityNotes);
        item.setUnderInvestigationNotes(underInvestigationNotes);
        return item;
    }

    private static List<String> lines(String text) {
        return Arrays.stream(text.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    private static void assertLine(List<String> lines, String expected) {
        assertTrue(expected + " in " + lines, lines.contains(expected));
    }

    private static int occurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
