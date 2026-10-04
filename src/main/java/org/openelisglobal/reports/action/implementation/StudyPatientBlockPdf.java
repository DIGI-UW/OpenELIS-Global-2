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

    static void add(Document document, ARVReportData order) {
        PdfPTable block = new PdfPTable(new float[] { 140, 140, 95, 95 });
        block.setWidthPercentage(100);
        block.setSpacingBefore(8);
        block.setSpacingAfter(8);
        block.addCell(field("Sujetno", order.getSubjectNumber()));
        block.addCell(field("Labno", order.getLabNo()));
        block.addCell(field("Sexe", order.getGender()));
        block.addCell(field("Grossesse", null));
        block.addCell(field("Date Naiss.", order.getBirth_date()));
        block.addCell(field("Age", order.getAge()));
        block.addCell(field("Allaitement", null));
        block.addCell(field(null, null));
        block.addCell(field("Date de Prél.", order.getCollectiondate()));
        block.addCell(field("Date de Réception", day(order.getReceptiondate())));
        PdfPCell prescriber = new PdfPCell();
        prescriber.setColspan(2);
        prescriber.setRowspan(2);
        prescriber.addElement(labelled("Prescripteur", order.getDoctor()));
        prescriber.addElement(labelled("Site", order.getOrgname()));
        prescriber.addElement(labelled("Adresse", null));
        block.addCell(prescriber);
        block.addCell(field("Date de Réalisation", order.getCompleationdate()));
        block.addCell(field("Date de validation", order.getReleasedate()));
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
