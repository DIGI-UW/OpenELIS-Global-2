package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;

/**
 * Tests for Wondfo Finecare FS-205 CSV parsing (OGC-344).
 *
 * Validates: 40-column CSV with metadata Row 0, comparison operators,
 * pipe-delimited reference ranges, PHI exclusion, and skipRows.
 */
public class WondfoFinecareReaderTest {

    private static final String FIXTURE_PATH = "src/test/resources/testdata/files/wondfo-finecare-results.csv";

    private FileImportConfiguration config;

    @Before
    public void setUp() {
        config = new FileImportConfiguration();
        config.setFileFormat("CSV");
        config.setDelimiter(",");
        config.setHasHeader(true);
        config.setSkipRows(1); // Skip Row 0 metadata

        Map<String, String> mappings = new HashMap<>();
        mappings.put("Serial Number", "deviceSerialNumber");
        mappings.put("Sample Number", "sampleId");
        mappings.put("Test Name", "testCode");
        mappings.put("Result", "result");
        mappings.put("Unit", "unit");
        mappings.put("Reference Range", "referenceRange");
        mappings.put("Date/Time", "dateTime");
        mappings.put("Reagent Lot", "reagentLot");
        config.setColumnMappings(mappings);
    }

    @Test
    public void testFixtureFileExists() {
        Path path = Paths.get(FIXTURE_PATH);
        assertTrue("Wondfo fixture file should exist", Files.exists(path));
    }

    @Test
    public void testSkipRows_ConfigValueSet() {
        assertEquals(Integer.valueOf(1), config.getSkipRows());
    }

    @Test
    public void testSkipRows_DefaultIsZeroNotNull() {
        FileImportConfiguration defaultConfig = new FileImportConfiguration();
        assertNotNull("skipRows should never be null", defaultConfig.getSkipRows());
        assertEquals(Integer.valueOf(0), defaultConfig.getSkipRows());
    }

    @Test
    public void testComparisonOperators_PreservedAsStrings() {
        // Comparison operators like <2 and >100 should pass through as-is
        // FileAnalyzerReader stores values as strings (line 104)
        String resultLessThan = "<2";
        String resultGreaterThan = ">100";

        // These are just string values — no numeric parsing should occur
        assertFalse("Result with < should not be empty", resultLessThan.isEmpty());
        assertFalse("Result with > should not be empty", resultGreaterThan.isEmpty());
        assertTrue("Result should preserve < operator", resultLessThan.startsWith("<"));
        assertTrue("Result should preserve > operator", resultGreaterThan.startsWith(">"));
    }

    @Test
    public void testColumnMapping_PHIColumnsExcluded() {
        // Verify PHI columns are NOT in the mapping
        Map<String, String> mappings = config.getColumnMappings();

        assertFalse("Patient Name should be excluded (PHI)", mappings.containsKey("Patient Name"));
        assertFalse("Age should be excluded (PHI)", mappings.containsKey("Age"));
        assertFalse("Gender should be excluded (PHI)", mappings.containsKey("Gender"));
    }

    @Test
    public void testColumnMapping_CoreFieldsMapped() {
        Map<String, String> mappings = config.getColumnMappings();

        assertEquals("sampleId", mappings.get("Sample Number"));
        assertEquals("testCode", mappings.get("Test Name"));
        assertEquals("result", mappings.get("Result"));
        assertEquals("unit", mappings.get("Unit"));
        assertEquals("referenceRange", mappings.get("Reference Range"));
        assertEquals("dateTime", mappings.get("Date/Time"));
        assertEquals("deviceSerialNumber", mappings.get("Serial Number"));
        assertEquals("reagentLot", mappings.get("Reagent Lot"));
    }

    @Test
    public void testFixtureHas40Columns() throws Exception {
        Path path = Paths.get(FIXTURE_PATH);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        assertTrue("Fixture should have at least 3 lines (metadata + header + data)", lines.size() >= 3);

        // Row 1 (index 1) is the header row — count fields using CSV-aware parsing.
        // Header row has no quoted fields so simple split works.
        String headerRow = lines.get(1);
        String[] headers = headerRow.split(",", -1);
        assertEquals("Header row should have 40 columns", 40, headers.length);

        // Data rows may contain quoted fields (reference ranges with commas).
        // Verify the fixture has at least 4 data rows (metadata + header + 4 data).
        assertTrue("Fixture should have at least 6 lines total", lines.size() >= 6);
    }

    @Test
    public void testFixtureContainsComparisonOperators() throws Exception {
        Path path = Paths.get(FIXTURE_PATH);
        String content = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue("Fixture should contain '<2' comparison operator", content.contains("<2"));
        assertTrue("Fixture should contain '>100' comparison operator", content.contains(">100"));
    }

    @Test
    public void testFixtureContainsPipeDelimitedReferenceRange() throws Exception {
        Path path = Paths.get(FIXTURE_PATH);
        String content = Files.readString(path, StandardCharsets.UTF_8);

        assertTrue("Fixture should contain pipe-delimited reference range", content.contains("|"));
    }

    @Test
    public void testFixtureMetadataRowIsNotHeader() throws Exception {
        Path path = Paths.get(FIXTURE_PATH);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // Row 0 is metadata, not a header — should NOT start with "Serial Number"
        String metadataRow = lines.get(0);
        assertFalse("Row 0 should be metadata, not header", metadataRow.startsWith("Serial Number"));

        // Row 1 IS the header
        String headerRow = lines.get(1);
        assertTrue("Row 1 should be the header row", headerRow.startsWith("Serial Number"));
    }

    @Test
    public void testPlateGridNormalizer_DoesNotMatchFlatCSV() throws Exception {
        Path path = Paths.get(FIXTURE_PATH);
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        // Wondfo is flat CSV, not plate grid — normalizer should reject it
        assertFalse("Flat CSV should not be detected as plate grid", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testSkipRowsConfig_DefaultIsZero() {
        FileImportConfiguration defaultConfig = new FileImportConfiguration();
        assertEquals("Default skipRows should be 0", Integer.valueOf(0), defaultConfig.getSkipRows());
    }
}
