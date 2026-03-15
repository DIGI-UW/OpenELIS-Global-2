package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

public class ExcelAnalyzerReaderTest {

    private FileImportConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new FileImportConfiguration();
        configuration.setAnalyzerId(1);
        configuration.setHasHeader(true);
        configuration.setFileFormat("EXCEL");

        Map<String, String> columnMappings = new HashMap<>();
        columnMappings.put("Sample Name", "sampleId");
        columnMappings.put("Assay", "testCode");
        columnMappings.put("CT", "result");
        configuration.setColumnMappings(columnMappings);
    }

    @Test
    public void testRead_WithXlsContent_ParsesRowsIntoLineBuffer() throws Exception {
        ExcelAnalyzerReader reader = new ExcelAnalyzerReader(configuration);
        byte[] content = createWorkbookBytes(true);

        boolean result = reader.readStream(new ByteArrayInputStream(content));

        assertFalse("Read returns false when no plugin matches in unit test context", result);
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) ReflectionTestUtils.getField(reader, "lines");
        assertTrue("Reader should parse at least one data row from XLS", lines != null && !lines.isEmpty());
    }

    @Test
    public void testRead_WithXlsxContent_ParsesRowsIntoLineBuffer() throws Exception {
        ExcelAnalyzerReader reader = new ExcelAnalyzerReader(configuration);
        byte[] content = createWorkbookBytes(false);

        boolean result = reader.readStream(new ByteArrayInputStream(content));

        assertFalse("Read returns false when no plugin matches in unit test context", result);
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) ReflectionTestUtils.getField(reader, "lines");
        assertTrue("Reader should parse at least one data row from XLSX", lines != null && !lines.isEmpty());
    }

    @Test
    public void testRead_DataLinesPreservePositionalAlignment() throws Exception {
        // Mapping only sampleId (pos 0), testCode (pos 1), result (pos 2) — no
        // interpretation, position, etc.
        // The output should still have empty tabs for the absent PREFERRED_FIELD_ORDER
        // positions
        ExcelAnalyzerReader reader = new ExcelAnalyzerReader(configuration);
        byte[] content = createWorkbookBytes(false);

        reader.readStream(new ByteArrayInputStream(content));

        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) ReflectionTestUtils.getField(reader, "lines");
        // No header line prepended — only data lines
        assertTrue("Should have data lines", lines != null && lines.size() >= 1);

        // lines[0] is the first data row (no header prepended)
        String dataLine = lines.get(0);
        String[] tokens = dataLine.split("\t", -1);

        // PREFERRED_FIELD_ORDER has 7 fields: sampleId, testCode, result,
        // interpretation, position, testDate, testTime
        // Data line should have at least 7 tab-separated tokens (some empty) to
        // preserve positions
        assertTrue("Data line should have at least 7 positional tokens, got " + tokens.length, tokens.length >= 7);

        // Position 0 = sampleId, 1 = testCode, 2 = result (from column mapping)
        assertEquals("SAMPLE-001", tokens[0]);
        assertEquals("HIV-VL", tokens[1]);
        assertEquals("34.5", tokens[2]);

        // Position 3 = interpretation (not mapped, should be empty)
        assertEquals("interpretation position should be empty", "", tokens[3]);
    }

    @Test
    public void testRead_NoHeaderPrepended() throws Exception {
        ExcelAnalyzerReader reader = new ExcelAnalyzerReader(configuration);
        byte[] content = createWorkbookBytes(false);

        reader.readStream(new ByteArrayInputStream(content));

        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) ReflectionTestUtils.getField(reader, "lines");
        assertFalse("Lines should not be empty", lines == null || lines.isEmpty());

        // First line should be data, not header column names
        String firstLine = lines.get(0);
        assertFalse("First line should not contain Excel header 'Sample Name'", firstLine.contains("Sample Name"));
        assertTrue("First line should contain data value 'SAMPLE-001'", firstLine.contains("SAMPLE-001"));
    }

    @Test
    public void testRead_EmptyCellsPreservedInParsedRecord() throws Exception {
        // Add interpretation mapping so we can test empty cell handling
        Map<String, String> mappings = new HashMap<>();
        mappings.put("Sample Name", "sampleId");
        mappings.put("Assay", "testCode");
        mappings.put("CT", "result");
        mappings.put("Interpretation", "interpretation");
        configuration.setColumnMappings(mappings);

        ExcelAnalyzerReader reader = new ExcelAnalyzerReader(configuration);
        byte[] content = createWorkbookWithEmptyCells();

        reader.readStream(new ByteArrayInputStream(content));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> records = (List<Map<String, String>>) ReflectionTestUtils.getField(reader,
                "parsedRecords");
        assertFalse("Should have parsed records", records == null || records.isEmpty());

        // Even empty cells should appear in parsedRecord
        Map<String, String> record = records.get(0);
        assertEquals("SAMPLE-001", record.get("sampleId"));
        assertEquals("HIV-VL", record.get("testCode"));
        // Empty CT cell should be preserved as empty string
        assertTrue("Empty cell should be in parsedRecord", record.containsKey("result"));
        // Interpretation should also be present
        assertTrue("Interpretation should be in parsedRecord", record.containsKey("interpretation"));
        assertEquals("Negative", record.get("interpretation"));
    }

    private byte[] createWorkbookBytes(boolean xls) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (xls) {
                try (HSSFWorkbook workbook = new HSSFWorkbook()) {
                    populateSheet(workbook.createSheet("QS"));
                    workbook.write(out);
                }
            } else {
                try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                    populateSheet(workbook.createSheet("QS"));
                    workbook.write(out);
                }
            }
            return out.toByteArray();
        }
    }

    private byte[] createWorkbookWithEmptyCells() throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Results");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Sample Name");
            header.createCell(1).setCellValue("Assay");
            header.createCell(2).setCellValue("CT");
            header.createCell(3).setCellValue("Interpretation");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("SAMPLE-001");
            row.createCell(1).setCellValue("HIV-VL");
            row.createCell(2).setCellValue(""); // Empty CT (negative result)
            row.createCell(3).setCellValue("Negative");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void populateSheet(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Sample Name");
        header.createCell(1).setCellValue("Assay");
        header.createCell(2).setCellValue("CT");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("SAMPLE-001");
        row.createCell(1).setCellValue("HIV-VL");
        row.createCell(2).setCellValue("34.5");
    }
}
