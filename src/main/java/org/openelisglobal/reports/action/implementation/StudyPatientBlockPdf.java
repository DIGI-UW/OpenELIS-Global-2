package org.openelisglobal.reports.action.implementation;

import java.util.Objects;
import org.openelisglobal.reports.action.implementation.reportBeans.ARVReportData;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The patient and order block of the study (RetroCI) reports: subject and lab
 * numbers, the patient's details, the order's dates, and the prescriber and
 * site in a box. Its labels are French in every language, as the study printed
 * them.
 */
final class StudyPatientBlockPdf {

    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 9);

    private StudyPatientBlockPdf() {
    }

    /**
     * The block's values; pregnancy and breastfeeding print blank when a report
     * does not record them.
     */
    record Patient(String subjectNumber, String labNo, String gender, String pregnancy, String birthDate, String age,
            String breastfeeding, String collected, String received, String doctor, String site, String completed,
            String validated) {
        static Patient of(ARVReportData order) {
            return new Patient(order.getSubjectNumber(), order.getLabNo(), order.getGender(), null,
                    order.getBirth_date(), order.getAge(), null, order.getCollectiondate(), order.getReceptiondate(),
                    order.getDoctor(), order.getOrgname(), order.getCompleationdate(), order.getReleasedate());
        }
    }

    static void add(Document document, ARVReportData order) {
        add(document, Patient.of(order));
    }

    static void add(Document document, Patient patient) {
        PdfPTable block = new PdfPTable(new float[] { 140, 140, 95, 95 });
        block.setWidthPercentage(100);
        block.setSpacingBefore(8);
        block.setSpacingAfter(8);
        block.addCell(field("Sujetno", patient.subjectNumber()));
        block.addCell(field("Labno", patient.labNo()));
        block.addCell(field("Sexe", patient.gender()));
        block.addCell(
                "F".equalsIgnoreCase(patient.gender()) ? field("Grossesse", patient.pregnancy()) : field(null, null));
        block.addCell(field("Date Naiss.", patient.birthDate()));
        block.addCell(field("Age", patient.age()));
        block.addCell("F".equalsIgnoreCase(patient.gender()) ? field("Allaitement", patient.breastfeeding())
                : field(null, null));
        block.addCell(field(null, null));
        block.addCell(field("Date de Prél.", patient.collected()));
        block.addCell(field("Date de Réception", day(patient.received())));
        PdfPCell prescriber = new PdfPCell();
        prescriber.setColspan(2);
        prescriber.setRowspan(2);
        prescriber.addElement(labelled("Prescripteur", patient.doctor()));
        prescriber.addElement(labelled("Site", patient.site()));
        prescriber.addElement(labelled("Adresse", null));
        block.addCell(prescriber);
        block.addCell(field("Date de Réalisation", patient.completed()));
        block.addCell(field("Date de validation", patient.validated()));
        document.add(block);
    }

    /** The reception date without its time. */
    private static String day(String receptionDate) {
        return receptionDate != null && receptionDate.length() > 10 ? receptionDate.substring(0, 10) : receptionDate;
    }

    private static PdfPCell field(String label, String value) {
        PdfPCell cell = new PdfPCell(label == null ? new Phrase("") : labelled(label, value));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    private static Phrase labelled(String label, String value) {
        Phrase phrase = new Phrase(label + ": ", LABEL_FONT);
        phrase.add(new Phrase(Objects.toString(value, ""), VALUE_FONT));
        return phrase;
    }
}
