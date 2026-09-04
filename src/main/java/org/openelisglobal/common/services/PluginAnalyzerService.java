/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.common.services;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PluginAnalyzerService {

    private static PluginAnalyzerService INSTANCE;
    protected static final String TEST_MAPPING_FILE_PATH = "/var/lib/openelis-global/analyzer/analyzer-test-map.csv";
    protected static final String CSV_TEST_MAP_COULMN_ANALYSER = "ANALYZER";
    protected static final String CSV_TEST_MAP_COULMN_ANALYSER_TEST_NAME = "ANALYZER_TEST_NAME";
    protected static final String CSV_TEST_MAP_COULMN_LOINC = "LOINC_CODE";
    protected static final String CSV_TEST_MAP_COULMN_ACTUAL_TEST_NAME = "ACTUAL_TEST_NAME";

    @Autowired
    private AnalyzerTypeService analyzerTypeService;
    @Autowired
    private TestService testService;

    private Map<String, AnalyzerImporterPlugin> pluginByAnalyzerId = new HashMap<>();

    private List<AnalyzerImporterPlugin> analyzerPlugins = new ArrayList<>();

    public void registerAnalyzerPlugin(AnalyzerImporterPlugin plugin) {
        analyzerPlugins.add(plugin);
    }

    public List<AnalyzerImporterPlugin> getAnalyzerPlugins() {
        return analyzerPlugins;
    }

    /**
     * Returns plugin list with generic plugins (GenericASTM, GenericHL7) first. Use
     * when analyzer_configuration.prefer_generic_plugin is true so generic is tried
     * before legacy.
     */
    public List<AnalyzerImporterPlugin> getAnalyzerPluginsWithGenericFirst() {
        List<AnalyzerImporterPlugin> generic = new ArrayList<>();
        List<AnalyzerImporterPlugin> legacy = new ArrayList<>();
        for (AnalyzerImporterPlugin p : analyzerPlugins) {
            if (p.isGenericPlugin()) {
                generic.add(p);
            } else {
                legacy.add(p);
            }
        }
        generic.addAll(legacy);
        return generic;
    }

    @PostConstruct
    private void registerInstance() {
        INSTANCE = this;
    }

    public static PluginAnalyzerService getInstance() {
        return INSTANCE;
    }

    public AnalyzerImporterPlugin getPluginByAnalyzerId(String analyzerId) {
        return pluginByAnalyzerId.get(analyzerId);
    }

    public void registerAnalyzer(AnalyzerImporterPlugin analyzer) {
        registerAnalyzer(analyzer, Optional.empty());
    }

    public void registerAnalyzer(AnalyzerImporterPlugin analyzer, Optional<String> analyzerId) {
        registerAnalyzerPlugin(analyzer);
        if (analyzerId.isPresent()) {
            pluginByAnalyzerId.put(analyzerId.get(), analyzer);
        }
    }

    /** Returns the existing AnalyzerType ID without writing analyzer mappings. */
    public String addAnalyzerDatabaseParts(String name, String description, List<TestMapping> nameMappings) {
        return resolveAnalyzerTypeId(name);
    }

    public String addAnalyzerDatabaseParts(String name, String description, List<TestMapping> nameMappings,
            boolean hasSetupPage) {
        return resolveAnalyzerTypeId(name);
    }

    /**
     * Resolve AnalyzerType ID for a plugin name. Tries multiple matching strategies
     * since legacy plugin names don't always match AnalyzerType names exactly.
     */
    private String resolveAnalyzerTypeId(String pluginName) {
        // Strategy 1: exact name match
        AnalyzerType type = analyzerTypeService.getAnalyzerTypeByName(pluginName);
        if (type != null) {
            return type.getId();
        }

        // Strategy 2: strip "Analyzer" suffix (e.g., "CobasC111Analyzer" → "Cobas
        // C111")
        // PluginRegistryService derives names like "Cobas C111" from
        // "CobasC111Analyzer"
        // Legacy plugins pass "CobasC111Analyzer" as the name
        for (AnalyzerType candidate : analyzerTypeService.getAll()) {
            if (candidate.getPluginClassName() == null) {
                continue;
            }
            String simpleName = candidate.getPluginClassName()
                    .substring(candidate.getPluginClassName().lastIndexOf('.') + 1);
            if (simpleName.equals(pluginName) || simpleName.equals(pluginName + "Analyzer")) {
                return candidate.getId();
            }
            // Also match stripped: "Mindray" → "MindrayAnalyzer" class
            String stripped = simpleName.replaceAll("Analyzer$", "");
            if (stripped.equals(pluginName)) {
                return candidate.getId();
            }
        }

        return null;
    }

    public void loadNamingMappingsFromCSV(List<PluginAnalyzerService.TestMapping> nameMapping, String analyzerName) {
        File file = new File(TEST_MAPPING_FILE_PATH);
        if (!file.exists()) {
            LogEvent.logDebug(this.getClass().getName(), "loadNamingMappingsFromCSV",
                    "CSV file not found: " + TEST_MAPPING_FILE_PATH);
            return; // Exit if file doesn't exist
        }

        try (FileReader reader = new FileReader(file);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String analyser = record.get(CSV_TEST_MAP_COULMN_ANALYSER).trim();
                String analyserTestName = record.get(CSV_TEST_MAP_COULMN_ANALYSER_TEST_NAME).trim();
                String loincCode = record.get(CSV_TEST_MAP_COULMN_LOINC).trim();
                String actualTestName = record.get(CSV_TEST_MAP_COULMN_ACTUAL_TEST_NAME).trim();
                if (analyzerName.equals(analyser)) {
                    nameMapping.add(new PluginAnalyzerService.TestMapping(analyserTestName, actualTestName, loincCode));
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "loadNamingMappingsFromCSV",
                    "Unable to Load analyzer test map for  " + analyzerName);
        }
    }

    public void loadLoincMappingsFromCSV(Map<String, String> testToLoincMap, String analyzerName) {
        File file = new File(TEST_MAPPING_FILE_PATH);
        if (!file.exists()) {
            LogEvent.logDebug(this.getClass().getName(), "loadLoincMappingsFromCSV",
                    "CSV file not found: " + TEST_MAPPING_FILE_PATH);
            return; // Exit if file doesn't exist
        }

        try (FileReader reader = new FileReader(file);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String analyser = record.get(CSV_TEST_MAP_COULMN_ANALYSER).trim();
                String testName = record.get(CSV_TEST_MAP_COULMN_ANALYSER_TEST_NAME).trim();
                String loincCode = record.get(CSV_TEST_MAP_COULMN_LOINC).trim();
                if (analyzerName.equals(analyser)) {
                    testToLoincMap.put(testName, loincCode);
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "loadLoincMappingsFromCSV",
                    "Unable to Load LOINC mappings for " + analyzerName);
        }
    }

    public void loadLoincTestsMappingsFromCSV(HashMap<String, List<Test>> testToLoincMap, String analyzerName) {
        File file = new File(TEST_MAPPING_FILE_PATH);
        if (!file.exists()) {
            LogEvent.logDebug(this.getClass().getName(), "loadLoincTestsMappingsFromCSV",
                    "CSV file not found: " + TEST_MAPPING_FILE_PATH);
            return; // Exit if file doesn't exist
        }

        try (FileReader reader = new FileReader(file);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String analyser = record.get(CSV_TEST_MAP_COULMN_ANALYSER).trim();
                String loincCode = record.get(CSV_TEST_MAP_COULMN_LOINC).trim();
                if (analyzerName.equals(analyser)) {
                    testToLoincMap.put(loincCode, testService.getTestsByLoincCode(loincCode));
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "loadLoincTestsMappingsFromCSV",
                    "Unable to Load LOINC mappings for " + analyzerName);
        }
    }

    public static class TestMapping {
        private final String analyzerTestName;
        private final String dbbTestName;
        private final String dbbTestLoincCode;

        public TestMapping(String analyzerTestName, String dbbTestName) {
            this.analyzerTestName = analyzerTestName;
            this.dbbTestName = dbbTestName;
            this.dbbTestLoincCode = "";
        }

        public TestMapping(String analyzerTestName, String dbbTestName, String dbbTestLoincCode) {
            this.analyzerTestName = analyzerTestName;
            this.dbbTestName = dbbTestName;
            this.dbbTestLoincCode = dbbTestLoincCode;
        }

        public String getAnalyzerTestName() {
            return analyzerTestName;
        }

        public String getDbbTestName() {
            return dbbTestName;
        }

        public String getDbbTestLoincCode() {
            return dbbTestLoincCode;
        }
    }
}
