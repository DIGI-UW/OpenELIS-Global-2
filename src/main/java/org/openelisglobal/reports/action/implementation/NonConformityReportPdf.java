package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.NonConformityBy.CountReportItem;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The non-conformity reports: each order's non-conformities by received date,
 * or the counts by section and reason. Page numbers and the supervisor's
 * signature line follow the site settings for reports.
 */
final class NonConformityReportPdf {

    private static final Font PERIOD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    private NonConformityReportPdf() {
    }

    static byte[] byDate(String title, String period, List<NonConformityReportData> items,
            boolean showSiteSubjectNumber) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(PageSize.A4.rotate(), out, title, period);
        String[] headers = { MessageUtil.getMessage("report.section"), MessageUtil.getMessage("report.date"),
                MessageUtil.getMessage("report.reasonForRejection"),
                MessageUtil.getMessage("report.typeOfSample").trim(), MessageUtil.getMessage("report.biologist"),
                MessageUtil.getMessage("report.note") };
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size()
                    && Objects.equals(items.get(end).getAccessionNumber(), items.get(start).getAccessionNumber())) {
                end++;
            }
            NonConformityReportData order = items.get(start);
            PdfPTable details = new PdfPTable(2);
            details.setWidthPercentage(100);
            details.setSpacingBefore(12);
            details.addCell(field("report.orderNo", order.getAccessionNumber()));
            details.addCell(field("report.receptionDate", order.getReceivedDate()));
            details.addCell(field("report.subjectNo", order.getSubjectNumber()));
            details.addCell(
                    showSiteSubjectNumber ? field("report.siteSubjectNo", order.getSiteSubjectNumber()) : blank());
            document.add(details);

            PdfPTable table = new PdfPTable(new float[] { 92, 78, 135, 117, 90, 236 });
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, headers);
            for (NonConformityReportData item : items.subList(start, end)) {
                for (String value : new String[] { item.getSection(), item.getNonConformityDate(),
                        item.getNonConformityReason(), item.getSampleType(), item.getBiologist(), item.getQaNote() }) {
                    table.addCell(new Phrase(value == null ? "" : value, CELL_FONT));
                }
            }
            document.add(table);
            Paragraph comments = new Paragraph();
            comments.add(new Phrase(MessageUtil.getMessage("report.comments") + ": ", LABEL_FONT));
            comments.add(new Phrase(order.getSampleNote() == null ? "" : order.getSampleNote(), TEXT_FONT));
            document.add(comments);
            start = end;
        }
        return close(document, out);
    }

    /**
     * The items arrive sorted by section then reason, one per non-conformity; each
     * reason's count is its run within the section.
     */
    static byte[] bySectionAndReason(String title, String period, List<CountReportItem> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(PageSize.A4, out, title, period);
        String total = MessageUtil.getMessage("report.total");
        PdfPTable table = new PdfPTable(new float[] { 270, 50 });
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
        table.setSpacingBefore(12);
        int groups = 0;
        int start = 0;
        while (start < items.size()) {
            String group = items.get(start).getGroup();
            int groupEnd = start;
            while (groupEnd < items.size() && Objects.equals(items.get(groupEnd).getGroup(), group)) {
                groupEnd++;
            }
            groups++;
            PdfPCell groupCell = new PdfPCell(new Phrase(group, LABEL_FONT));
            groupCell.setColspan(2);
            table.addCell(groupCell);
            int category = start;
            while (category < groupEnd) {
                String reason = items.get(category).getCategory();
                int categoryEnd = category;
                while (categoryEnd < groupEnd && Objects.equals(items.get(categoryEnd).getCategory(), reason)) {
                    categoryEnd++;
                }
                table.addCell(new Phrase(reason, CELL_FONT));
                table.addCell(new Phrase(String.valueOf(categoryEnd - category), CELL_FONT));
                category = categoryEnd;
            }
            table.addCell(new Phrase(total + ": " + group, LABEL_FONT));
            table.addCell(new Phrase(String.valueOf(groupEnd - start), LABEL_FONT));
            start = groupEnd;
        }
        if (groups > 1) {
            table.addCell(new Phrase(total, LABEL_FONT));
            table.addCell(new Phrase(String.valueOf(items.size()), LABEL_FONT));
        }
        document.add(table);
        return close(document, out);
    }

    private static Document open(Rectangle pageSize, ByteArrayOutputStream out, String title, String period) {
        Document document = new Document(pageSize, 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out);
        ReportHeaderPdf.add(document, title, ReportHeaderPdf.siteNameLines());
        Paragraph periodLine = new Paragraph(period, PERIOD_FONT);
        periodLine.setSpacingBefore(6);
        document.add(periodLine);
        return document;
    }

    private static byte[] close(Document document, ByteArrayOutputStream out) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.SIGNATURES_ON_NONCONFORMITY_REPORTS,
                "true")) {
            Paragraph signature = new Paragraph(MessageUtil.getMessage("report.supervisorSign"), TEXT_FONT);
            signature.setSpacingBefore(24);
            document.add(signature);
        }
        document.close();
        return out.toByteArray();
    }

    private static PdfPCell field(String labelKey, String value) {
        Phrase phrase = new Phrase();
        phrase.add(new Phrase(MessageUtil.getMessage(labelKey) + ": ", LABEL_FONT));
        phrase.add(new Phrase(value == null ? "" : value, TEXT_FONT));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static PdfPCell blank() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }
}
