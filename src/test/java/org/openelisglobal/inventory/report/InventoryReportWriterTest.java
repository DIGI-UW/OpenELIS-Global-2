package org.openelisglobal.inventory.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
        PdfReader reader = new PdfReader(body);
        String page = PdfTextExtractor.getTextFromPage(reader, 1);
        reader.close();
        assertTrue(page, page.contains("Transaction History"));
        assertTrue(page, page.contains("Quantity Change"));
        assertTrue(page, page.contains("REAGENT_A") && page.contains("-5") && page.contains("RECEIPT"));
    }

    @Test
    public void writeExcel_typesQuantityColumnsAsNumbersAndIdentifiersAsText() throws Exception {
        ReportTable table = new ReportTable("Transaction History",
                List.of("Item Code", "Lot Number", "Quantity Change", "Quantity After"), Set.of(2, 3));
        table.addRow(List.of("REAGENT_A", "000123", "-5", "20"));
        table.addRow(List.of("100", "LOT-2", "10", "30"));
        table.addRow(List.of("REAGENT_A", "000123", "1.50", "31.50"));
        table.addRow(List.of("TOTAL (3 items)", "", "6.50", ""));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InventoryReportWriter.writeExcel(table, out);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row scratch = sheet.createRow(sheet.getLastRowNum() + 1);
            Cell sum = scratch.createCell(0);
            sum.setCellFormula("SUM(C2:C4)");
            assertEquals(6.5, evaluator.evaluate(sum).getNumberValue(), 0.0001);
            // The TOTAL row's empty Quantity After cell must not count as a 0.
            Cell count = scratch.createCell(1);
            count.setCellFormula("COUNT(D2:D5)");
            assertEquals(3.0, evaluator.evaluate(count).getNumberValue(), 0.0);

            assertEquals("Transaction History", sheet.getSheetName());
            assertEquals("Quantity Change", sheet.getRow(0).getCell(2).getStringCellValue());
            assertEquals(-5.0, sheet.getRow(1).getCell(2).getNumericCellValue(), 0.0);
            assertEquals("000123", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("100", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }
}
