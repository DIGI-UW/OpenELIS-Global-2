package org.openelisglobal.analyzerimport.analyzerreaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PlateGridNormalizerTest {

    // --- Existing tests (Tecan tab-delimited) ---

    @Test
    public void testIsPlateGridFormat_WithTecanGrid_ReturnsTrue() {
        List<String> lines = Arrays.asList("Application\tMagellan", "Instrument\tInfinite F50", "Method\tHIV_ELISA_450",
                "", "<>\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12",
                "A\t2.345\t0.048\t1.234\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "B\t2.401\t0.047\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "C\t0.051\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "D\t1.567\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "E\t0.098\t0.101\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "F\t0.099\t0.103\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "G\t0.050\t0.049\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "H\t0.048\t0.051\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0");
        assertTrue(PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testIsPlateGridFormat_WithWellPerRow_ReturnsFalse() {
        List<String> lines = Arrays.asList("WellPosition\tSampleID\tOD_450", "A01\tTCN-001\t2.345",
                "A02\tTCN-002\t0.048");
        assertFalse(PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testNormalizeToWellPerRow_ProducesCorrectRowCount() {
        List<String> lines = Arrays.asList("Application\tMagellan", "", "<>\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12",
                "A\t2.345\t0.048\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "B\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "C\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "D\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "E\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "F\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "G\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0",
                "H\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0\t0.0");
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, "\t");
        assertEquals(97, result.size());
        assertEquals("WellPosition\tSampleID\tOD_450", result.get(0));
        assertEquals("A1\t\t2.345", result.get(1));
        assertEquals("A2\t\t0.048", result.get(2));
    }

    // --- Configurable result column name ---

    @Test
    public void testNormalizeToWellPerRow_CustomResultColumnName() {
        List<String> lines = buildSingleGridLines("\t", false);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, "\t", "Abs");
        assertEquals("WellPosition\tSampleID\tAbs", result.get(0));
    }

    @Test
    public void testNormalizeToWellPerRow_DefaultResultColumnWhenNull() {
        List<String> lines = buildSingleGridLines("\t", false);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, "\t", null);
        assertEquals("WellPosition\tSampleID\tOD_450", result.get(0));
    }

    // --- BOM handling ---

    @Test
    public void testStripBom_RemovesBomFromFirstLine() {
        List<String> lines = Arrays.asList("\uFEFFApplication,Magellan", "Instrument,Infinite F50");
        List<String> stripped = PlateGridNormalizer.stripBom(lines);
        assertEquals("Application,Magellan", stripped.get(0));
        assertEquals("Instrument,Infinite F50", stripped.get(1));
    }

    @Test
    public void testStripBom_NoBomUnchanged() {
        List<String> lines = Arrays.asList("Application,Magellan", "Instrument,Infinite F50");
        List<String> stripped = PlateGridNormalizer.stripBom(lines);
        assertEquals("Application,Magellan", stripped.get(0));
    }

    // --- Comma delimiter plate grid (Magellan ASCII) ---

    @Test
    public void testIsPlateGridFormat_WithCommaDelimiter() {
        List<String> lines = buildSingleGridLines(",", false);
        assertTrue("Comma-delimited plate grid should be detected", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testNormalizeToWellPerRow_CommaDelimiter() {
        List<String> lines = buildSingleGridLines(",", false);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ",");
        assertEquals(97, result.size());
        assertEquals("A1\t\t2.345", result.get(1));
    }

    // --- Semicolon delimiter with comma decimals (French locale / Multiskan) ---

    @Test
    public void testIsPlateGridFormat_WithSemicolonDelimiter() {
        List<String> lines = buildSingleGridLines(";", true);
        assertTrue("Semicolon-delimited plate grid should be detected", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testNormalizeToWellPerRow_FrenchLocale_CommaDecimalsConverted() {
        List<String> lines = buildSingleGridLines(";", true);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ";", "Abs");
        assertEquals("WellPosition\tSampleID\tAbs", result.get(0));
        // Comma decimal "2,345" should be normalized to "2.345"
        assertEquals("A1\t\t2.345", result.get(1));
        assertEquals("A2\t\t0.048", result.get(2));
    }

    // --- Dual grid (Multiskan FC SkanIt export) ---

    @Test
    public void testIsPlateGridFormat_DualGrid_Detected() {
        List<String> lines = buildDualGridLines();
        assertTrue("Dual-grid format should be detected as plate grid", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testNormalizeToWellPerRow_DualGrid_SampleIdsExtracted() {
        List<String> lines = buildDualGridLines();
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ";", "Abs");
        assertEquals(97, result.size());
        assertEquals("WellPosition\tSampleID\tAbs", result.get(0));
        // First row: A1=Blanc (QC blank), A2=NC, A3=PC, A4=E001
        assertEquals("A1\tBlanc\t0.045", result.get(1));
        assertEquals("A2\tNC\t0.089", result.get(2));
        assertEquals("A3\tPC\t2.345", result.get(3));
        assertEquals("A4\tE001\t1.234", result.get(4));
    }

    @Test
    public void testNormalizeToWellPerRow_DualGrid_AllWellsHaveSampleIds() {
        List<String> lines = buildDualGridLines();
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ";", "Abs");
        // Every data line (after header) should have a non-empty SampleID
        for (int i = 1; i < result.size(); i++) {
            String[] parts = result.get(i).split("\t", -1);
            assertEquals("Each row should have 3 tab-separated fields", 3, parts.length);
            assertFalse("SampleID should not be empty at row " + i, parts[1].isEmpty());
        }
    }

    @Test
    public void testNormalizeToWellPerRow_DualGrid_QcWellsIdentifiable() {
        List<String> lines = buildDualGridLines();
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ";", "Abs");
        // Check QC wells are identifiable by their sample ID names
        String a1 = result.get(1); // Blanc
        String a2 = result.get(2); // NC
        String a3 = result.get(3); // PC
        assertTrue("A1 should be Blanc (blank)", a1.contains("Blanc"));
        assertTrue("A2 should be NC (negative control)", a2.contains("NC"));
        assertTrue("A3 should be PC (positive control)", a3.contains("PC"));
    }

    // --- Multiskan fixture file ---

    @Test
    public void testMultiskanFixture_DetectedAsPlateGrid() throws Exception {
        Path path = Paths.get("src/test/resources/testdata/files/multiskan-fc-dual-grid.csv");
        assertTrue("Multiskan fixture should exist", Files.exists(path));
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue("Multiskan fixture should be detected as plate grid", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testMultiskanFixture_DualGridNormalization() throws Exception {
        Path path = Paths.get("src/test/resources/testdata/files/multiskan-fc-dual-grid.csv");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ";", "Abs");
        assertEquals("Should produce 97 lines (header + 96 wells)", 97, result.size());

        // Verify sample IDs from second grid are present
        String a1 = result.get(1);
        assertTrue("A1 should have sample ID 'Blanc'", a1.startsWith("A1\tBlanc\t"));

        // Verify comma decimals are normalized to period decimals
        String a3 = result.get(3); // PC with OD 2,345 → 2.345
        assertTrue("A3 OD should have period decimal", a3.endsWith("2.345"));
    }

    // --- Comma decimal normalization ---

    @Test
    public void testNormalizeCommaDecimal_NumericWithComma() {
        assertEquals("2.345", PlateGridNormalizer.normalizeCommaDecimal("2,345"));
        assertEquals("0.048", PlateGridNormalizer.normalizeCommaDecimal("0,048"));
    }

    @Test
    public void testNormalizeCommaDecimal_NonNumericUnchanged() {
        assertEquals("Blanc", PlateGridNormalizer.normalizeCommaDecimal("Blanc"));
        assertEquals("NC", PlateGridNormalizer.normalizeCommaDecimal("NC"));
        assertEquals("E001", PlateGridNormalizer.normalizeCommaDecimal("E001"));
    }

    @Test
    public void testNormalizeCommaDecimal_PeriodDecimalUnchanged() {
        assertEquals("2.345", PlateGridNormalizer.normalizeCommaDecimal("2.345"));
    }

    // --- Overflow handling ---

    @Test
    public void testNormalizeToWellPerRow_OverflowValue_PreservedAsIs() {
        List<String> lines = Arrays.asList("<>,1,2,3,4,5,6,7,8,9,10,11,12",
                "A,Overflow,0.048,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "B,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "C,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "D,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "E,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "F,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "G,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "H,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0");
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ",");
        // "Overflow" is not numeric, so it passes through unchanged
        assertEquals("A1\t\tOverflow", result.get(1));
    }

    // --- Tecan fixture file ---

    @Test
    public void testTecanFixture_DetectedAsPlateGrid() throws Exception {
        Path path = Paths.get("src/test/resources/testdata/files/tecan-f50-plate-grid.csv");
        assertTrue("Tecan fixture should exist", Files.exists(path));
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue("Tecan fixture should be detected as plate grid", PlateGridNormalizer.isPlateGridFormat(lines));
    }

    @Test
    public void testTecanFixture_NormalizesCorrectly() throws Exception {
        Path path = Paths.get("src/test/resources/testdata/files/tecan-f50-plate-grid.csv");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<String> result = PlateGridNormalizer.normalizeToWellPerRow(lines, ",", "OD_450");
        assertEquals("Should produce 97 lines", 97, result.size());
        assertEquals("WellPosition\tSampleID\tOD_450", result.get(0));
        assertEquals("A1\t\t2.345", result.get(1));
        // G12 has "Overflow" — should be preserved
        assertEquals("G12\t\tOverflow", result.get(84)); // row G (7th), col 12 → index 72+12=84
    }

    @Test
    public void testTecanFixture_BomHandled() throws Exception {
        // Simulate a BOM-prefixed Tecan file
        List<String> lines = Arrays.asList("\uFEFFApplication,Magellan", "", "<>,1,2,3,4,5,6,7,8,9,10,11,12",
                "A,2.345,0.048,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "B,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "C,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "D,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "E,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "F,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "G,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0",
                "H,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0");
        List<String> stripped = PlateGridNormalizer.stripBom(lines);
        assertTrue("BOM-stripped Tecan should be detected as plate grid",
                PlateGridNormalizer.isPlateGridFormat(stripped));
    }

    // --- Helper methods ---

    private List<String> buildSingleGridLines(String delim, boolean commaDecimal) {
        String d = delim;
        String v1 = commaDecimal ? "2,345" : "2.345";
        String v2 = commaDecimal ? "0,048" : "0.048";
        String z = commaDecimal ? "0,0" : "0.0";
        return Arrays.asList("Application" + d + "Magellan", "",
                "<>" + d + "1" + d + "2" + d + "3" + d + "4" + d + "5" + d + "6" + d + "7" + d + "8" + d + "9" + d
                        + "10" + d + "11" + d + "12",
                "A" + d + v1 + d + v2 + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "B" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "C" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "D" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "E" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "F" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "G" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z,
                "H" + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z + d + z);
    }

    /**
     * Build a dual-grid fixture (Multiskan FC SkanIt-style). First grid: OD values
     * (Abs) with semicolons and comma decimals. Second grid: Sample IDs
     * (Echantillon).
     */
    private List<String> buildDualGridLines() {
        return Arrays.asList("Instrument;Multiskan FC", "Protocol;ELISA HIV", "Date;06/03/2026", "",
                // First grid header (Abs)
                "Abs", ";1;2;3;4;5;6;7;8;9;10;11;12",
                "A;0,045;0,089;2,345;1,234;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678",
                "B;0,901;1,234;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678;0,901;1,234",
                "C;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678;0,901;1,234;0,567;0,890",
                "D;1,123;0,456;0,789;1,012;0,345;0,678;0,901;1,234;0,567;0,890;1,123;0,456",
                "E;0,789;1,012;0,345;0,678;0,901;1,234;0,567;0,890;1,123;0,456;0,789;1,012",
                "F;0,345;0,678;0,901;1,234;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678",
                "G;0,901;1,234;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678;0,901;1,234",
                "H;0,567;0,890;1,123;0,456;0,789;1,012;0,345;0,678;0,901;1,234;0,567;0,890", "",
                // Second grid header (Echantillon)
                "Echantillon", ";1;2;3;4;5;6;7;8;9;10;11;12",
                "A;Blanc;NC;PC;E001;E002;E003;E004;E005;E006;E007;E008;E009",
                "B;E010;E011;E012;E013;E014;E015;E016;E017;E018;E019;E020;E021",
                "C;E022;E023;E024;E025;E026;E027;E028;E029;E030;E031;E032;E033",
                "D;E034;E035;E036;E037;E038;E039;E040;E041;E042;E043;E044;E045",
                "E;E046;E047;E048;E049;E050;E051;E052;E053;E054;E055;E056;E057",
                "F;E058;E059;E060;E061;E062;E063;E064;E065;E066;E067;E068;E069",
                "G;E070;E071;E072;E073;E074;E075;E076;E077;E078;E079;E080;E081",
                "H;E082;E083;E084;E085;E086;E087;E088;E089;E090;E091;E092;E093");
    }
}
