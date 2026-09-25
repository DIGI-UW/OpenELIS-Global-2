package org.openelisglobal.common.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import org.openelisglobal.internationalization.MessageUtil;

/**
 * Pieces every compliance export shares: the row cap, the validated date
 * window, the page-numbered document, the CAP-style heading block and the blue
 * table header row. Used by the E-Sig log export (OGC-703), the QC inspector
 * report (OGC-706) and the system audit trail export.
 *
 * <p>
 * Fonts and page geometry stay with each report — they differ per document and
 * are the part a reader actually sees as that report's layout.
 */
public final class PdfExportSupport {

    /** Row cap on a flat tabular export. Scope-bounded reports rarely reach it. */
    public static final int MAX_EXPORT_ROWS = 10000;

    private static final long MAX_EXPORT_DATE_RANGE_DAYS = 366;
    private static final BaseColor HEADER_BACKGROUND = new BaseColor(51, 102, 179);

    private PdfExportSupport() {
    }

    /** A validated export window: start of the first day to end of the last. */
    public record ExportWindow(Timestamp start, Timestamp end) {
    }

    /**
     * Parse and bound an ISO date window. The parameter names are only used to word
     * the rejection so it names the request parameter the caller declared.
     *
     * @throws IllegalArgumentException on an unparseable date, a reversed window,
     *                                  or a span over one year
     */
    public static ExportWindow parseWindow(String startDate, String endDate, String startParam, String endParam) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(startDate);
            to = LocalDate.parse(endDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + e.getMessage());
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(startParam + " must not be after " + endParam);
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_EXPORT_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range must not exceed 1 year");
        }
        return new ExportWindow(Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.atTime(LocalTime.MAX)));
    }

    /**
     * Open the document onto the stream with a page number on the footer of every
     * page, worded by the given message key.
     */
    public static void openWithPageNumbers(Document document, OutputStream out, String pageMessageKey)
            throws DocumentException {
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageNumberFooter(pageMessageKey));
        document.open();
    }

    /**
     * The report heading: title line then one meta line per entry, each written
     * verbatim (callers supply their own line breaks).
     */
    public static void addHeading(Document document, String title, Font titleFont, Font metaFont, String... metaLines)
            throws DocumentException {
        document.add(new Phrase(title + "\n", titleFont));
        for (String line : metaLines) {
            document.add(new Phrase(line, metaFont));
        }
    }

    /** The blue, centred header row shared by the export tables. */
    public static void addHeaderRow(PdfPTable table, Font font, float padding, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(HEADER_BACKGROUND);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(padding);
            table.addCell(cell);
        }
    }

    /** The configured site name for the report heading, never null. */
    public static String labName(ConfigurationProperties configurationProperties) {
        String name = configurationProperties.getPropertyValue(ConfigurationProperties.Property.SiteName);
        return name == null ? "" : name;
    }

    /** Footer with page number on every page of a PDF export (CAP layout). */
    private static class PageNumberFooter extends PdfPageEventHelper {
        private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8);

        private final String pageMessageKey;

        PageNumberFooter(String pageMessageKey) {
            this.pageMessageKey = pageMessageKey;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Phrase footer = new Phrase(MessageUtil.getMessage(pageMessageKey) + " " + writer.getPageNumber(),
                    FOOTER_FONT);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2, document.bottom() - 12, 0);
        }
    }
}
