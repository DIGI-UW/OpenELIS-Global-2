package org.openelisglobal.coldstorage.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.openelisglobal.coldstorage.service.dto.FreezerDailyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerMonthlyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerWeeklyLogData;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The freezer temperature log: daily readings grouped by month, weekly
 * summaries grouped by month, or monthly summaries grouped by year.
 */
final class FreezerTemperatureReportPdf {

    record Heading(String reportType, String facilityName, String freezerName, String startDate, String endDate,
            String reportDate) {
    }

    private static final String NO_VALUE = "—";
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font GROUP_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    private FreezerTemperatureReportPdf() {
    }

    static byte[] daily(Heading heading, List<FreezerDailyLogData> rows) {
        return render(heading, rows, FreezerDailyLogData::getMonthYear, Function.identity(),
                new float[] { 140, 100, 100, 342, 100 },
                new String[] { "Date/Time", "Temperature (°C)", "Humidity (%)", "Status", "Alert" },
                row -> new String[] { row.getRecordedAt(), decimal(row.getTemperature(), 1),
                        decimal(row.getHumidity(), 1), status(row.getStatus()),
                        Boolean.TRUE.equals(row.getAlertTriggered()) ? "YES" : NO_VALUE });
    }

    static byte[] weekly(Heading heading, List<FreezerWeeklyLogData> rows) {
        return render(heading, rows, FreezerWeeklyLogData::getMonthYear, Function.identity(),
                new float[] { 180, 60, 85, 80, 80, 80, 60, 60, 97 },
                new String[] { "Week Period", "Readings", "Avg Temp (°C)", "Min (°C)", "Max (°C)", "Humidity (%)",
                        "Normal", "Warning", "Critical/Alert" },
                row -> new String[] { row.getWeekPeriod(), String.valueOf(row.getReadingCount()),
                        decimal(row.getAvgTemperature(), 2), decimal(row.getMinTemperature(), 2),
                        decimal(row.getMaxTemperature(), 2), decimal(row.getAvgHumidity(), 1),
                        String.valueOf(row.getNormalCount()), String.valueOf(row.getWarningCount()),
                        row.getCriticalCount() + " / " + row.getAlertCount() });
    }

    static byte[] monthly(Heading heading, List<FreezerMonthlyLogData> rows) {
        return render(heading, rows, FreezerMonthlyLogData::getYear, year -> "Year " + year,
                new float[] { 130, 50, 70, 95, 90, 90, 75, 50, 50, 82 },
                new String[] { "Month", "Days", "Readings", "Avg Temp (°C)", "Min Temp (°C)", "Max Temp (°C)",
                        "Humidity (%)", "Normal", "Warn", "Crit/Alert" },
                row -> new String[] { row.getMonthYear(), String.valueOf(row.getDaysMonitored()),
                        String.valueOf(row.getReadingCount()), decimal(row.getAvgTemperature(), 2),
                        decimal(row.getMinTemperature(), 2), decimal(row.getMaxTemperature(), 2),
                        decimal(row.getAvgHumidity(), 1), String.valueOf(row.getNormalCount()),
                        String.valueOf(row.getWarningCount()), row.getCriticalCount() + "/" + row.getAlertCount() });
    }

    /**
     * Each run of rows with the same group key gets the key as a heading and its
     * own table, in the order the rows arrive.
     */
    private static <T, K> byte[] render(Heading heading, List<T> rows, Function<T, K> groupKey,
            Function<K, String> groupTitle, float[] widths, String[] headers, Function<T, String[]> cells) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize().rotate(), 30, 30, 30, 40);
        PdfExportSupport.openWithPageNumbers(document, out, "report.label.page");
        addHeading(document, heading);

        PdfPTable table = null;
        K currentGroup = null;
        for (T row : rows) {
            K group = groupKey.apply(row);
            if (table == null || !Objects.equals(group, currentGroup)) {
                if (table != null) {
                    document.add(table);
                }
                currentGroup = group;
                Paragraph title = new Paragraph(groupTitle.apply(group), GROUP_FONT);
                title.setSpacingBefore(12);
                title.setSpacingAfter(6);
                document.add(title);
                table = new PdfPTable(widths);
                table.setWidthPercentage(100);
                table.setHeaderRows(1);
                PdfExportSupport.addHeaderRow(table, HEADER_FONT, 4, headers);
            }
            for (String cell : cells.apply(row)) {
                table.addCell(new Phrase(cell, CELL_FONT));
            }
        }
        if (table != null) {
            document.add(table);
        }

        Paragraph compliance = new Paragraph("Regulatory Compliance Statement", LABEL_FONT);
        compliance.setSpacingBefore(18);
        document.add(compliance);
        document.add(new Paragraph(
                "This report complies with CAP, CLIA, FDA, and WHO guidelines for temperature-controlled storage monitoring.",
                TEXT_FONT));
        document.add(new Paragraph(
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                TEXT_FONT));
        document.close();
        return out.toByteArray();
    }

    private static void addHeading(Document document, Heading heading) {
        document.add(new Paragraph("Freezer Temperature Monitoring Report", TITLE_FONT));
        document.add(new Paragraph(heading.reportType().toUpperCase() + " TEMPERATURE LOG", SUBTITLE_FONT));
        PdfPTable details = new PdfPTable(new float[] { 80, 300, 100, 182 });
        details.setWidthPercentage(100);
        details.setSpacingBefore(8);
        details.addCell(plain("Facility:", LABEL_FONT));
        details.addCell(plain(heading.facilityName(), TEXT_FONT));
        details.addCell(plain("Report Date:", LABEL_FONT));
        details.addCell(plain(heading.reportDate(), TEXT_FONT));
        details.addCell(plain("Freezer Unit:", LABEL_FONT));
        details.addCell(plain(heading.freezerName(), TEXT_FONT));
        details.addCell(plain("Period:", LABEL_FONT));
        details.addCell(plain(heading.startDate() + " to " + heading.endDate(), TEXT_FONT));
        document.add(details);
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static String decimal(BigDecimal value, int places) {
        return value == null ? NO_VALUE : String.format("%." + places + "f", value);
    }

    private static String status(String status) {
        return status == null || "NORMAL".equals(status) ? "Normal" : status;
    }
}
