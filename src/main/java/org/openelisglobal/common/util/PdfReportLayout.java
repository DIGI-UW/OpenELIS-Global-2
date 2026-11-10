package org.openelisglobal.common.util;

import java.io.OutputStream;
import org.openelisglobal.internationalization.MessageUtil;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Repeating report identity in reserved space, independent of table pagination.
 */
public final class PdfReportLayout {
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8);

    private PdfReportLayout() {
    }

    public static PdfPTable heading(String title, Font titleFont, Font metaFont, String... lines) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        addLine(table, title, titleFont);
        for (String line : lines) {
            addLine(table, line, metaFont);
        }
        return table;
    }

    public static void addLine(PdfPTable table, String line, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(line == null ? "" : line, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(3);
        table.addCell(cell);
    }

    public static PdfWriter open(Document document, OutputStream out, PdfPTable header, boolean pageNumbers,
            String... footerLines) {
        header.setTotalWidth(document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
        header.setLockedWidth(true);
        float top = 30 + header.getTotalHeight() + 8;
        float bottom = Math.max(document.bottomMargin(), 42 + footerLines.length * 12);
        if (top + bottom >= document.getPageSize().getHeight() - 36) {
            throw new IllegalArgumentException("Report heading leaves no room for content");
        }
        document.setMargins(document.leftMargin(), document.rightMargin(), top, bottom);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PdfPageEventHelper() {
            private PdfTemplate total;

            @Override
            public void onOpenDocument(PdfWriter writer, Document document) {
                if (pageNumbers) {
                    total = writer.getDirectContent().createTemplate(32, 12);
                }
            }

            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                header.writeSelectedRows(0, -1, document.left(), document.getPageSize().getTop() - 30,
                        writer.getDirectContent());
                float y = 30 + footerLines.length * 12;
                for (String line : footerLines) {
                    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                            new Phrase(line, FOOTER_FONT), document.left(), y, 0);
                    y -= 12;
                }
                if (pageNumbers) {
                    String count = MessageUtil.getMessage("report.label.page") + " " + writer.getPageNumber() + " "
                            + MessageUtil.getMessage("report.about") + " ";
                    float totalWidth = 24;
                    ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                            new Phrase(count, FOOTER_FONT), document.right() - totalWidth - 3, 12, 0);
                    writer.getDirectContent().addTemplate(total, document.right() - totalWidth, 10);
                }
            }

            @Override
            public void onCloseDocument(PdfWriter writer, Document document) {
                if (pageNumbers) {
                    ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                            new Phrase(String.valueOf(writer.getPageNumber() - 1), FOOTER_FONT), 0, 2, 0);
                }
            }
        });
        document.open();
        return writer;
    }
}
