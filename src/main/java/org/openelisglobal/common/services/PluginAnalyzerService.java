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
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
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
    private AnalyzerTestMappingService analyzerMappingService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private TestService testService;

    private List<AnalyzerTestMapping> existingMappings;
    private Map<String, AnalyzerImporterPlugin> pluginByAnalyzerId = new HashMap<>();

    private List<AnalyzerImporterPlugin> analyzerPlugins = new ArrayList<>();

    public void registerAnalyzerPlugin(AnalyzerImporterPlugin plugin) {
        analyzerPlugins.add(plugin);
    }

    public List<AnalyzerImporterPlugin> getAnalyzerPlugins() {
        return analyzerPlugins;
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

    public String addAnalyzerDatabaseParts(String name, String description, List<TestMapping> nameMappings) {
        Analyzer analyzer = analyzerService.getAnalyzerByName(name);
        if (analyzer != null && analyzer.getId() != null) {
            analyzer.setActive(true);
            registerAanlyzerInCache(name, analyzer.getId());
        } else {
            if (analyzer == null) {
                analyzer = new Analyzer();
                analyzer.setActive(true);
                analyzer.setName(name);
            }
            analyzer.setDescription(description);
        }

        List<AnalyzerTestMapping> testMappings = createTestMappings(nameMappings);
        if (!testMappings.isEmpty() && existingMappings == null) {
            existingMappings = analyzerMappingService.getAll();
        }

        analyzer.setSysUserId("1");

        try {
            analyzerService.persistData(analyzer, testMappings, existingMappings);
            registerAanlyzerInCache(name, analyzer.getId());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return analyzer.getId();
    }

    public String addAnalyzerDatabaseParts(String name, String description, List<TestMapping> nameMappings,
            boolean hasSetupPage) {
        Analyzer analyzer = analyzerService.getAnalyzerByName(name);
        if (analyzer != null && analyzer.getId() != null) {
            analyzer.setActive(true);
            analyzer.setHasSetupPage(hasSetupPage);
            registerAanlyzerInCache(name, analyzer.getId());
        } else {
            if (analyzer == null) {
                analyzer = new Analyzer();
                analyzer.setActive(true);
                analyzer.setName(name);
                analyzer.setHasSetupPage(hasSetupPage);
            }
            analyzer.setDescription(description);
        }
        loadNamingMappingsFromCSV(nameMappings, name);

        List<AnalyzerTestMapping> testMappings = createTestMappings(nameMappings);
        if (!testMappings.isEmpty() && existingMappings == null) {
            existingMappings = analyzerMappingService.getAll();
        }

        analyzer.setSysUserId("1");

        try {
            analyzerService.persistData(analyzer, testMappings, existingMappings);
            registerAanlyzerInCache(name, analyzer.getId());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
        }
        return analyzer.getId();
    }

    private List<AnalyzerTestMapping> createTestMappings(List<TestMapping> nameMappings) {
        ArrayList<AnalyzerTestMapping> testMappings = new ArrayList<>();
        for (TestMapping names : nameMappings) {
            List<Test> tests = testService.getTestsByLoincCode(names.getDbbTestLoincCode());
            if (tests == null || tests.size() == 0) {
                testMappings.add(createAnalyzerTestMapping(names, getIdForTestName(names.getDbbTestName())));
            } else {
                for (Test test : tests) {
                    testMappings.add(createAnalyzerTestMapping(names, test.getId()));
                }
            }
        }
        return testMappings;
    }

    private AnalyzerTestMapping createAnalyzerTestMapping(TestMapping names, String testId) {
        AnalyzerTestMapping analyzerMapping = new AnalyzerTestMapping();
        analyzerMapping.setAnalyzerTestName(names.getAnalyzerTestName());
        analyzerMapping.setTestId(testId);
        return analyzerMapping;
    }

    private String getIdForTestName(String dbbTestName) {
        List<Test> tests = testService.getTestsByName(dbbTestName);
        Test test;
        if (tests != null && !tests.isEmpty()) {
            test = tests.get(0);
            if (test != null) {
                return test.getId();
            }
        }
        LogEvent.logError(this.getClass().getSimpleName(), "getIdForTestName",
                "Unable to find test " + dbbTestName + " in test catalog");
        return null;
    }

    private void registerAanlyzerInCache(String name, String id) {
        AnalyzerTestNameCache.getInstance().registerPluginAnalyzer(name, id);
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
