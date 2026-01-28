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

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.springframework.stereotype.Component;

/**
 * Analyzer plugin for Horiba ABX Micros 60 hematology analyzer.
 *
 * <p>
 * Feature: 011-madagascar-analyzer-integration Milestone: M10 (Micros 60)
 *
 * <p>
 * The Micros 60 is a 3-Part Differential hematology analyzer that communicates
 * via ASTM LIS2-A2 protocol over RS232 serial connection. It produces 18 CBC
 * parameters including the 3-part differential (LYM, MXD, NEU).
 *
 * <p>
 * Identification: ASTM Header contains "ABX^MICROS60" or "MICROS"
 *
 * <p>
 * Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
 */
@Component
public class HoribaMicros60Analyzer implements AnalyzerImporterPlugin {

    /** Analyzer name used for database registration and identification */
    public static final String ANALYZER_NAME = "Horiba ABX Micros 60";

    /** Description for database registration */
    public static final String ANALYZER_DESCRIPTION = "Horiba ABX Micros 60 3-Part Differential Hematology Analyzer (ASTM/RS232)";

    /** Primary ASTM header identifier */
    private static final String HEADER_ID_PRIMARY = "MICROS60";

    /** Fallback ASTM header identifier */
    private static final String HEADER_ID_FALLBACK = "MICROS";

    /** Manufacturer identifier in ASTM header */
    private static final String MANUFACTURER_ID = "ABX";

    /**
     * Register analyzer with PluginAnalyzerService on Spring initialization.
     */
    @PostConstruct
    public void register() {
        List<PluginAnalyzerService.TestMapping> testMappings = createTestMappings();
        PluginAnalyzerService.getInstance().addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings,
                true);
        PluginAnalyzerService.getInstance().registerAnalyzer(this);
    }

    /**
     * Create test mappings for Micros 60 CBC parameters.
     *
     * <p>
     * Maps ASTM test codes to OpenELIS test names via LOINC codes. Reference:
     * specs/011-madagascar-analyzer-integration/research.md Section 12
     *
     * @return List of test mappings
     */
    private List<PluginAnalyzerService.TestMapping> createTestMappings() {
        List<PluginAnalyzerService.TestMapping> mappings = new ArrayList<>();

        // CBC parameters
        mappings.add(new PluginAnalyzerService.TestMapping("WBC", "White Blood Cells", "6690-2"));
        mappings.add(new PluginAnalyzerService.TestMapping("RBC", "Red Blood Cells", "789-8"));
        mappings.add(new PluginAnalyzerService.TestMapping("HGB", "Hemoglobin", "718-7"));
        mappings.add(new PluginAnalyzerService.TestMapping("HCT", "Hematocrit", "4544-3"));
        mappings.add(new PluginAnalyzerService.TestMapping("MCV", "MCV", "787-2"));
        mappings.add(new PluginAnalyzerService.TestMapping("MCH", "MCH", "785-6"));
        mappings.add(new PluginAnalyzerService.TestMapping("MCHC", "MCHC", "786-4"));
        mappings.add(new PluginAnalyzerService.TestMapping("PLT", "Platelet Count", "777-3"));
        mappings.add(new PluginAnalyzerService.TestMapping("RDW", "RDW", "788-0"));
        mappings.add(new PluginAnalyzerService.TestMapping("MPV", "MPV", "32623-1"));

        // 3-Part Differential
        mappings.add(new PluginAnalyzerService.TestMapping("LYM%", "Lymphocyte %", "736-9"));
        mappings.add(new PluginAnalyzerService.TestMapping("LYM#", "Lymphocyte Count", "731-0"));
        mappings.add(new PluginAnalyzerService.TestMapping("MXD%", "Mixed Cell %", "5909-7"));
        mappings.add(new PluginAnalyzerService.TestMapping("MXD#", "Mixed Cell Count", "26511-6"));
        mappings.add(new PluginAnalyzerService.TestMapping("NEU%", "Neutrophil %", "770-8"));
        mappings.add(new PluginAnalyzerService.TestMapping("NEU#", "Neutrophil Count", "751-8"));

        return mappings;
    }

    /**
     * Check if the ASTM message is from a Horiba Micros 60 analyzer.
     *
     * <p>
     * Identification strategy: 1. Look for "MICROS60" in ASTM H-segment (primary)
     * 2. Look for "ABX" + "MICROS" in H-segment (fallback)
     *
     * @param lines ASTM message lines
     * @return true if message is from Micros 60
     */
    @Override
    public boolean isTargetAnalyzer(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        // Find and check the H (Header) segment
        for (String line : lines) {
            if (line != null && line.startsWith("H|")) {
                // Primary check: contains "MICROS60"
                if (line.toUpperCase().contains(HEADER_ID_PRIMARY)) {
                    return true;
                }
                // Fallback: contains "ABX" and "MICROS" but not "PENTRA"
                if (line.toUpperCase().contains(MANUFACTURER_ID) && line.toUpperCase().contains(HEADER_ID_FALLBACK)
                        && !line.toUpperCase().contains("PENTRA")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the message contains analyzer results (R segments).
     *
     * @param lines ASTM message lines
     * @return true if message contains result records
     */
    @Override
    public boolean isAnalyzerResult(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        // Check for R (Result) segments
        return lines.stream().anyMatch(line -> line != null && line.startsWith("R|"));
    }

    /**
     * Get the line inserter for processing Micros 60 results.
     *
     * @return HoribaMicros60AnalyzerLineInserter instance
     */
    @Override
    public AnalyzerLineInserter getAnalyzerLineInserter() {
        return new HoribaMicros60AnalyzerLineInserter();
    }

    @Override
    public boolean connect() {
        // Registration happens via @PostConstruct
        return true;
    }
}
