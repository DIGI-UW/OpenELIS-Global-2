package org.openelisglobal.inventory.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class InventoryReportWriterTest {

    private String csvOf(ReportTable table) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InventoryReportWriter.writeCsv(table, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private List<String> csvLines(ReportTable table) throws Exception {
        return List.of(csvOf(table).split("\\R"));
    }

    private ReportTable transactionHistoryTable() {
        ReportTable table = new ReportTable("Transaction History",
                List.of("Date", "Item Code", "Transaction Type", "Quantity Change", "Quantity After"));
        table.addRow(List.of("2026-08-09 10:00", "REAGENT_A", "CONSUMPTION", "-5", "20"));
        table.addRow(List.of("2026-08-09 11:00", "REAGENT_A", "RECEIPT", "10", "30"));
        return table;
    }

    @Test
    public void writeCsv_negativeQuantityStaysNumeric() throws Exception {
        List<String> lines = csvLines(transactionHistoryTable());

        assertEquals("2026-08-09 10:00,REAGENT_A,CONSUMPTION,-5,20", lines.get(1));
        assertEquals("2026-08-09 11:00,REAGENT_A,RECEIPT,10,30", lines.get(2));
    }

    @Test
    public void writeCsv_negativeDecimalAndDayCountStayNumeric() throws Exception {
        ReportTable table = new ReportTable("Expiration Forecast", List.of("Item Code", "Days Until Expiration"));
        table.addRow(List.of("REAGENT_A", "-12"));
        table.addRow(List.of("REAGENT_B", "-3.50"));
        table.addRow(List.of("REAGENT_C", "+7"));

        List<String> lines = csvLines(table);

        assertEquals("REAGENT_A,-12", lines.get(1));
        assertEquals("REAGENT_B,-3.50", lines.get(2));
        assertEquals("REAGENT_C,+7", lines.get(3));
    }

    @Test
    public void writeCsv_formulaIsNeutralized() throws Exception {
        ReportTable table = new ReportTable("Stock Levels", List.of("Item Name"));
        table.addRow(List.of("=cmd|'/C calc'!A0"));
        table.addRow(List.of("@SUM(A1:A2)"));
        table.addRow(List.of("-2+3+cmd|'/C calc'!A0"));

        List<String> lines = csvLines(table);

        assertEquals("'=cmd|'/C calc'!A0", lines.get(1));
        assertEquals("'@SUM(A1:A2)", lines.get(2));
        assertEquals("'-2+3+cmd|'/C calc'!A0", lines.get(3));
    }

    @Test
    public void writeCsv_formulaHiddenBehindLeadingWhitespaceIsNeutralized() throws Exception {
        ReportTable table = new ReportTable("Stock Levels", List.of("Item Name"));
        table.addRow(List.of(" =1+1"));
        table.addRow(List.of("\t=1+1"));
        table.addRow(List.of("\r=1+1"));

        String csv = csvOf(table);

        assertTrue(csv, csv.contains("' =1+1"));
        assertTrue(csv, csv.contains("'\t=1+1"));
        assertTrue(csv, csv.contains("\"'\r=1+1\""));
    }

    @Test
    public void writeCsv_quotesValuesContainingDelimiters() throws Exception {
        ReportTable table = new ReportTable("Stock Levels", List.of("Item Name"));
        table.addRow(List.of("Reagent \"A\", 500mL"));

        assertEquals("\"Reagent \"\"A\"\", 500mL\"", csvLines(table).get(1));
    }

    @Test
    public void writePdf_producesPdfBytes() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InventoryReportWriter.writePdf(transactionHistoryTable(), out);

        byte[] body = out.toByteArray();
        assertEquals("%PDF", new String(body, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    public void writeExcel_writesEveryCellAsGiven() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InventoryReportWriter.writeExcel(transactionHistoryTable(), out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals("Transaction History", workbook.getSheetAt(0).getSheetName());
            assertEquals("Quantity Change", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("-5", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
        }
    }
}
