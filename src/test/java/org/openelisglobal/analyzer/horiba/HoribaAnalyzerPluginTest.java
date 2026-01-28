/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzer.horiba;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Horiba ABX analyzer plugins (Pentra 60 and Micros 60).
 *
 * <p>
 * Feature: 011-madagascar-analyzer-integration Milestones: M9 (Pentra 60), M10
 * (Micros 60)
 *
 * <p>
 * Tests follow TDD approach - tests written before implementation.
 *
 * <p>
 * Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
 */
public class HoribaAnalyzerPluginTest {

    private HoribaPentra60Analyzer pentra60Analyzer;
    private HoribaMicros60Analyzer micros60Analyzer;
    private List<String> pentra60Lines;
    private List<String> micros60Lines;

    @Before
    public void setUp() throws Exception {
        pentra60Analyzer = new HoribaPentra60Analyzer();
        micros60Analyzer = new HoribaMicros60Analyzer();
        pentra60Lines = loadTestFixture("testdata/astm/horiba-pentra60-cbc.astm");
        micros60Lines = loadTestFixture("testdata/astm/horiba-micros60-cbc.astm");
    }

    private List<String> loadTestFixture(String resourcePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comment lines (starting with #)
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    // ==================== Pentra 60 Tests ====================

    @Test
    public void testPentra60IsTargetAnalyzer_withPentra60Message_returnsTrue() {
        assertTrue("Pentra 60 analyzer should identify Pentra 60 messages",
                pentra60Analyzer.isTargetAnalyzer(pentra60Lines));
    }

    @Test
    public void testPentra60IsTargetAnalyzer_withMicros60Message_returnsFalse() {
        assertFalse("Pentra 60 analyzer should NOT identify Micros 60 messages",
                pentra60Analyzer.isTargetAnalyzer(micros60Lines));
    }

    @Test
    public void testPentra60IsTargetAnalyzer_withEmptyList_returnsFalse() {
        assertFalse("Pentra 60 analyzer should return false for empty list",
                pentra60Analyzer.isTargetAnalyzer(new ArrayList<>()));
    }

    @Test
    public void testPentra60IsTargetAnalyzer_withNullList_returnsFalse() {
        assertFalse("Pentra 60 analyzer should return false for null", pentra60Analyzer.isTargetAnalyzer(null));
    }

    @Test
    public void testPentra60IsAnalyzerResult_withResultMessage_returnsTrue() {
        assertTrue("Pentra 60 should identify result messages (R segments)",
                pentra60Analyzer.isAnalyzerResult(pentra60Lines));
    }

    @Test
    public void testPentra60GetAnalyzerLineInserter_returnsNonNull() {
        assertNotNull("Pentra 60 should return a non-null line inserter", pentra60Analyzer.getAnalyzerLineInserter());
    }

    @Test
    public void testPentra60GetAnalyzerLineInserter_returnsCorrectType() {
        assertTrue("Pentra 60 should return HoribaPentra60AnalyzerLineInserter",
                pentra60Analyzer.getAnalyzerLineInserter() instanceof HoribaPentra60AnalyzerLineInserter);
    }

    // ==================== Micros 60 Tests ====================

    @Test
    public void testMicros60IsTargetAnalyzer_withMicros60Message_returnsTrue() {
        assertTrue("Micros 60 analyzer should identify Micros 60 messages",
                micros60Analyzer.isTargetAnalyzer(micros60Lines));
    }

    @Test
    public void testMicros60IsTargetAnalyzer_withPentra60Message_returnsFalse() {
        assertFalse("Micros 60 analyzer should NOT identify Pentra 60 messages",
                micros60Analyzer.isTargetAnalyzer(pentra60Lines));
    }

    @Test
    public void testMicros60IsTargetAnalyzer_withEmptyList_returnsFalse() {
        assertFalse("Micros 60 analyzer should return false for empty list",
                micros60Analyzer.isTargetAnalyzer(new ArrayList<>()));
    }

    @Test
    public void testMicros60IsTargetAnalyzer_withNullList_returnsFalse() {
        assertFalse("Micros 60 analyzer should return false for null", micros60Analyzer.isTargetAnalyzer(null));
    }

    @Test
    public void testMicros60IsAnalyzerResult_withResultMessage_returnsTrue() {
        assertTrue("Micros 60 should identify result messages (R segments)",
                micros60Analyzer.isAnalyzerResult(micros60Lines));
    }

    @Test
    public void testMicros60GetAnalyzerLineInserter_returnsNonNull() {
        assertNotNull("Micros 60 should return a non-null line inserter", micros60Analyzer.getAnalyzerLineInserter());
    }

    @Test
    public void testMicros60GetAnalyzerLineInserter_returnsCorrectType() {
        assertTrue("Micros 60 should return HoribaMicros60AnalyzerLineInserter",
                micros60Analyzer.getAnalyzerLineInserter() instanceof HoribaMicros60AnalyzerLineInserter);
    }

    // ==================== ASTM Header Parsing Tests ====================

    @Test
    public void testPentra60HeaderParsing_extractsCorrectManufacturer() {
        String header = pentra60Lines.get(0); // H|\^&|||ABX^PENTRA60^V2.0...
        assertTrue("Header should contain ABX manufacturer", header.contains("ABX"));
    }

    @Test
    public void testPentra60HeaderParsing_extractsCorrectModel() {
        String header = pentra60Lines.get(0);
        assertTrue("Header should contain PENTRA60 model", header.contains("PENTRA60"));
    }

    @Test
    public void testMicros60HeaderParsing_extractsCorrectManufacturer() {
        String header = micros60Lines.get(0); // H|\^&|||ABX^MICROS60^V1.5...
        assertTrue("Header should contain ABX manufacturer", header.contains("ABX"));
    }

    @Test
    public void testMicros60HeaderParsing_extractsCorrectModel() {
        String header = micros60Lines.get(0);
        assertTrue("Header should contain MICROS60 model", header.contains("MICROS60"));
    }

    // ==================== Result Record Parsing Tests ====================

    @Test
    public void testPentra60ResultCount_has20Results() {
        long resultCount = pentra60Lines.stream().filter(line -> line.startsWith("R|")).count();
        assertEquals("Pentra 60 CBC should have 20 result records", 20, resultCount);
    }

    @Test
    public void testMicros60ResultCount_has16Results() {
        long resultCount = micros60Lines.stream().filter(line -> line.startsWith("R|")).count();
        assertEquals("Micros 60 CBC should have 16 result records", 16, resultCount);
    }

    @Test
    public void testPentra60PatientRecord_extractsPatientId() {
        String patientRecord = pentra60Lines.stream().filter(line -> line.startsWith("P|")).findFirst().orElse("");
        assertTrue("Patient record should contain patient ID", patientRecord.contains("PAT-2026-001"));
    }

    @Test
    public void testPentra60OrderRecord_extractsSampleId() {
        String orderRecord = pentra60Lines.stream().filter(line -> line.startsWith("O|")).findFirst().orElse("");
        assertTrue("Order record should contain sample ID", orderRecord.contains("SAMPLE-2026-0001"));
    }

    @Test
    public void testMicros60PatientRecord_extractsPatientId() {
        String patientRecord = micros60Lines.stream().filter(line -> line.startsWith("P|")).findFirst().orElse("");
        assertTrue("Patient record should contain patient ID", patientRecord.contains("PAT-2026-002"));
    }

    @Test
    public void testMicros60OrderRecord_extractsSampleId() {
        String orderRecord = micros60Lines.stream().filter(line -> line.startsWith("O|")).findFirst().orElse("");
        assertTrue("Order record should contain sample ID", orderRecord.contains("SAMPLE-2026-0002"));
    }

    // ==================== Cross-analyzer Identification Tests ====================

    @Test
    public void testAnalyzersAreDistinct_pentraDoesNotMatchMicros() {
        // Both analyzers should correctly identify only their own messages
        boolean pentraMatchesPentra = pentra60Analyzer.isTargetAnalyzer(pentra60Lines);
        boolean pentraMatchesMicros = pentra60Analyzer.isTargetAnalyzer(micros60Lines);

        assertTrue("Pentra should match Pentra messages", pentraMatchesPentra);
        assertFalse("Pentra should NOT match Micros messages", pentraMatchesMicros);
    }

    @Test
    public void testAnalyzersAreDistinct_microsDoesNotMatchPentra() {
        // Both analyzers should correctly identify only their own messages
        boolean microsMatchesMicros = micros60Analyzer.isTargetAnalyzer(micros60Lines);
        boolean microsMatchesPentra = micros60Analyzer.isTargetAnalyzer(pentra60Lines);

        assertTrue("Micros should match Micros messages", microsMatchesMicros);
        assertFalse("Micros should NOT match Pentra messages", microsMatchesPentra);
    }
}
