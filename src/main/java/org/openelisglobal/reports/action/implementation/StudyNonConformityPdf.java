package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.FollowupRequiredData;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The study (RetroCI) non-conformity documents: the notification sent to the
 * client for each order, and the orders needing follow-up by service. Their
 * labels are French in every language, as the study printed them.
 */
final class StudyNonConformityPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    private StudyNonConformityPdf() {
    }

    /**
     * One page per order, its non-conformities in arrival order; the items arrive
     * grouped by order.
     */
    static byte[] notification(List<NonConformityReportData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 90);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new ClientFooter());
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size()
                    && Objects.equals(items.get(end).getAccessionNumber(), items.get(start).getAccessionNumber())) {
                end++;
            }
            if (start > 0) {
                document.newPage();
            }
            addOrder(document, items.subList(start, end));
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    private static void addOrder(Document document, List<NonConformityReportData> events) {
        NonConformityReportData order = events.get(0);
        ReportHeaderPdf.add(document, "RAPPORT DE NON CONFORMITE: CLIENT", ReportHeaderPdf.siteNameLines());
        PdfPTable details = new PdfPTable(2);
        details.setWidthPercentage(100);
        details.setSpacingBefore(12);
        String subjectNumber = order.getSubjectNumber() != null ? order.getSubjectNumber()
                : order.getSiteSubjectNumber();
        addField(details, "Date de survenue", order.getNonConformityDate());
        addField(details, "Etude", order.getStudy());
        addField(details, "Date de prélèvement", order.getReceivedDate());
        addField(details, "Heure de prélèvement", order.getReceivedHour());
        addField(details, "SubjetNo", subjectNumber);
        addField(details, "Service", order.getService());
        addField(details, "Labno", order.getAccessionNumber());
        addField(details, "Prescripteur", order.getDoctor());
        document.add(details);

        Paragraph heading = new Paragraph("Motifs de non conformité", HEADING_FONT);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(6);
        document.add(heading);
        PdfPTable table = new PdfPTable(new float[] { 90, 70, 150, 110, 110 });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, "Section", "Date", "Motif du refus", "Type d'échantillon",
                "Biologiste");
        for (NonConformityReportData event : events) {
            for (String value : new String[] { event.getSection(), event.getNonConformityDate(),
                    event.getNonConformityReason(), event.getSampleType(), event.getBiologist() }) {
                table.addCell(new Phrase(Objects.toString(value, ""), CELL_FONT));
            }
            if (!GenericValidator.isBlankOrNull(event.getQaNote())) {
                PdfPCell note = new PdfPCell(labelled("Note", event.getQaNote(), CELL_FONT));
                note.setColspan(5);
                table.addCell(note);
            }
        }
        document.add(table);
        Paragraph comment = new Paragraph(labelled("Commentaire", order.getSampleNote(), TEXT_FONT));
        comment.setSpacingBefore(8);
        document.add(comment);
    }

    /**
     * The orders needing follow-up, by service; the items arrive sorted by service.
     */
    static byte[] followupRequired(String title, List<FollowupRequiredData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out);
        ReportHeaderPdf.add(document, title, ReportHeaderPdf.siteNameLines());
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size() && Objects.equals(items.get(end).getOrgname(), items.get(start).getOrgname())) {
                end++;
            }
            addService(document, items.subList(start, end));
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    private static void addService(Document document, List<FollowupRequiredData> orders) {
        PdfPTable table = new PdfPTable(new float[] { 98, 92, 112, 72, 72, 94 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(14);
        table.setHeaderRows(2);
        PdfPCell service = PdfExportSupport.headerCell("Service: " + Objects.toString(orders.get(0).getOrgname(), ""),
                LABEL_FONT, 4);
        service.setColspan(6);
        service.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(service);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, "Date de prélèvement", "Date de réception", "Lab No",
                "Sujet No", "Site Sujet No", "Nom du médecin");
        for (FollowupRequiredData order : orders) {
            for (String value : new String[] { order.getCollectiondate(), order.getReceivedDate(), order.getLabNo(),
                    order.getSubjectNumber(), order.getSiteSubjectNumber(), order.getDoctor() }) {
                table.addCell(new Phrase(Objects.toString(value, "").trim(), CELL_FONT));
            }
            addNotes(table, "Non Conformité Remarque", order.getNonConformityNotes());
            addNotes(table, "Suivi Requis Remarque", order.getUnderInvestigationNotes());
        }
        document.add(table);
    }

    /**
     * Notes are stored with {@code <br/>
     * } between their lines.
     */
    private static void addNotes(PdfPTable table, String heading, String notes) {
        if (notes == null) {
            return;
        }
        Paragraph text = new Paragraph(heading, LABEL_FONT);
        Arrays.stream(notes.split("<br\\s*/?>")).map(String::trim).filter(line -> !line.isEmpty())
                .forEach(line -> text.add(new Phrase("\n" + line, CELL_FONT)));
        PdfPCell cell = new PdfPCell(text);
        cell.setColspan(6);
        cell.setPaddingLeft(12);
        table.addCell(cell);
    }

    private static void addField(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell(labelled(label, value, TEXT_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        table.addCell(cell);
    }

    private static Phrase labelled(String label, String value, Font valueFont) {
        Phrase phrase = new Phrase(label + ": ", LABEL_FONT);
        phrase.add(new Phrase(Objects.toString(value, ""), valueFont));
        return phrase;
    }

    /** The client's signature and transmission lines at the foot of every page. */
    private static final class ClientFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float signatureLine = document.bottom() - 30;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Nom et signature du client: ____________________", TEXT_FONT), document.left(),
                    signatureLine, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Date de transmission: ____________", TEXT_FONT), document.right(), signatureLine, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Conserver une copie au secrétariat du laboratoire", TEXT_FONT), document.left(),
                    signatureLine - 16, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Date d'effet: " + DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(),
                    signatureLine - 16, 0);
        }
    }
}
