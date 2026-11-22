package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.PdfReportText;
import org.openelisglobal.internationalization.MessageUtil;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The layout the activity and rejection reports share: site, title, selection
 * and date range, one table, and a line for the reviewer to sign.
 */
final class ManagementReportPdf {

    private static final Font META_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    private ManagementReportPdf() {
    }

    static byte[] render(Rectangle pageSize, String title, String selection, Report.DateRange dateRange,
            List<String> headers, float[] widths, List<List<String>> rows) {
        List<String> metaLines = new ArrayList<>();
        String siteName = ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
        if (!GenericValidator.isBlankOrNull(siteName)) {
            metaLines.add(siteName);
        }
        metaLines.add(selection);
        metaLines.add(MessageUtil.getMessage("report.from") + " " + dateRange.getLowDateStr() + " "
                + MessageUtil.getMessage("report.to") + " " + dateRange.getHighDateStr());
        metaLines.add(MessageUtil.getMessage("report.printed") + ": " + DateUtil.getCurrentDateAsText());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(pageSize, 36, 36, 36, 48);
        ReportHeaderPdf.openRepeating(document, out, title, List.of(), metaLines.toArray(String[]::new));

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, headers.toArray(String[]::new));
        for (List<String> row : rows) {
            for (String value : row) {
                table.addCell(new Phrase(PdfReportText.plain(value), CELL_FONT));
            }
        }
        document.add(table);

        Paragraph signOff = new Paragraph(MessageUtil.getMessage("report.dateReviewedReceived"), META_FONT);
        signOff.setSpacingBefore(18);
        document.add(signOff);
        document.close();
        return out.toByteArray();
    }

    /**
     * A column value shown only where it changes, as the templates printed them: on
     * an order's first row, or when it differs from the row above.
     */
    static String whenChanged(String value, String valueAbove, boolean firstOfOrder) {
        return firstOfOrder || !Objects.equals(value, valueAbove) ? value : "";
    }
}
