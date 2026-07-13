package org.openelisglobal.inventory.report;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Renders a {@link ReportTable} into one of the 3 formats
 * {@code InventoryReports.jsx} offers. One writer per format (not one per
 * report type) since every report type reduces to the same headers+rows shape —
 * mirrors {@code AuditTrailReportRestController}'s itext/CSV style, generalized
 * to an arbitrary table instead of the audit-trail-specific columns.
 */
public final class InventoryReportWriter {

    private InventoryReportWriter() {
    }

    public static void writeCsv(ReportTable table, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.println(String.join(",",
                table.getHeaders().stream().map(InventoryReportWriter::csvEscape).toArray(String[]::new)));
        for (java.util.List<String> row : table.getRows()) {
            writer.println(String.join(",", row.stream().map(InventoryReportWriter::csvEscape).toArray(String[]::new)));
        }
        writer.flush();
    }

    public static void writePdf(ReportTable table, OutputStream out) throws IOException {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 8);

            document.add(new Phrase(table.getTitle() + "\n\n", titleFont));

            int columnCount = table.getHeaders().size();
            PdfPTable pdfTable = new PdfPTable(columnCount);
            pdfTable.setWidthPercentage(100);

            for (String header : table.getHeaders()) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(51, 102, 179));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4);
                pdfTable.addCell(cell);
            }

            for (java.util.List<String> row : table.getRows()) {
                for (String value : row) {
                    pdfTable.addCell(new Phrase(value != null ? value : "", cellFont));
                }
            }

            if (table.getRows().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No data for the selected filters", cellFont));
                emptyCell.setColspan(columnCount);
                pdfTable.addCell(emptyCell);
            }

            document.add(pdfTable);
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Error generating PDF report", e);
        }
    }

    public static void writeExcel(ReportTable table, OutputStream out) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(table.getTitle()));

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < table.getHeaders().size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(table.getHeaders().get(col));
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (java.util.List<String> row : table.getRows()) {
                Row excelRow = sheet.createRow(rowIndex++);
                for (int col = 0; col < row.size(); col++) {
                    excelRow.createCell(col).setCellValue(row.get(col));
                }
            }

            for (int col = 0; col < table.getHeaders().size(); col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
        }
    }

    private static String sanitizeSheetName(String title) {
        String sanitized = title.replaceAll("[\\[\\]:*?/\\\\]", "").trim();
        return sanitized.length() > 31 ? sanitized.substring(0, 31) : sanitized;
    }

    /**
     * Same CSV-formula-injection guard (CWE-1236) as
     * {@code AuditTrailReportRestController.csvEscape}.
     */
    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                value = "'" + value;
            }
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
