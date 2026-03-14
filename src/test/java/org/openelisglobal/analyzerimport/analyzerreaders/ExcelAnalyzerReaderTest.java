package org.openelisglobal.analyzerimport.analyzerreaders;

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
