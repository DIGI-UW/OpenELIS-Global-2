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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

public class AnalyzerTestNameCache {

    protected AnalyzerTypeService analyzerTypeService = SpringContext.getBean(AnalyzerTypeService.class);
    protected AnalyzerTestMappingService analyzerTestMappingService = SpringContext
            .getBean(AnalyzerTestMappingService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);

    private static class SingletonHelper {
        private static final AnalyzerTestNameCache INSTANCE = new AnalyzerTestNameCache();
    }

    public static final String SYSMEX_XT2000_NAME = "Sysmex XT 2000";
    public static final String COBAS_INTEGRA400_NAME = "Cobas Integra";
    public static final String FACSCALIBUR = "Facscalibur";
    public static final String EVOLIS = "Evolis";
    public static final String COBAS_TAQMAN = "Cobas Taqman";
    public static final String FACSCANTO = "FacsCanto";
    public static final String COBAS_DBS = "CobasDBS";
    public static final String COBAS_C311 = "Cobas C311";
    // Legacy index: keyed by AnalyzerType name (e.g., "Sysmex XT 2000", "Generic
    // HL7")
    // Used by legacy readers where type = analyzer (1:1 relationship)
    private final HashMap<String, Map<String, MappedTestName>> analyzerNameToTestNameMap = new HashMap<>();
    // Per-analyzer index: keyed by Analyzer.id (e.g., "6" for BC-5380)
    // Provides correct isolation when multiple analyzers share a generic type
    // (OGC-492)
    private final HashMap<String, Map<String, MappedTestName>> analyzerIdToTestNameMap = new HashMap<>();
    private Map<String, String> analyzerNameToIdMap;
    private Map<String, String> requestTODBName = new HashMap<>();
    private boolean isMapped = false;

    private AnalyzerTestNameCache() {
        requestTODBName.put("sysmex", SYSMEX_XT2000_NAME);
        requestTODBName.put("cobas_integra", COBAS_INTEGRA400_NAME);
        requestTODBName.put("facscalibur", FACSCALIBUR);
        requestTODBName.put("evolis", EVOLIS);
        requestTODBName.put("cobas_taqman", COBAS_TAQMAN);
        requestTODBName.put("facscanto", FACSCANTO);
        requestTODBName.put("cobasDBS", COBAS_DBS);
        requestTODBName.put("cobasc311", COBAS_C311);
    }

    public static AnalyzerTestNameCache getInstance() {
        return SingletonHelper.INSTANCE;
    }

    public String getDBNameForActionName(String actionName) {
        return requestTODBName.get(actionName);
    }

    public List<String> getAnalyzerNames() {
        insureMapsLoaded();
        List<String> nameList = new ArrayList<>();
        nameList.addAll(analyzerNameToIdMap.keySet());
        return nameList;
    }

    public MappedTestName getMappedTest(String analyzerName, String analyzerTestName) {
        Map<String, MappedTestName> testMap = getMappedTestsForAnalyzer(analyzerName);

        if (testMap != null) {
            return testMap.get(analyzerTestName);
        }

        return null;
    }

    /**
     * Per-analyzer lookup — uses analyzer ID (not type name) for correct isolation
     * when multiple analyzers share a generic plugin type (OGC-492).
     */
    public MappedTestName getMappedTestByAnalyzerId(String analyzerId, String testCode) {
        insureMapsLoaded();
        Map<String, MappedTestName> testMap = analyzerIdToTestNameMap.get(analyzerId);
        if (testMap != null) {
            return testMap.get(testCode);
        }
        return null;
    }

    /**
     * Register a plugin's analyzer type in the cache.
     *
     * @param analyzerName   The plugin/analyzer name
     * @param analyzerTypeId The AnalyzerType ID (not a physical Analyzer ID)
     */
    public void registerPluginAnalyzer(String analyzerName, String analyzerTypeId) {
        requestTODBName.put(analyzerName, analyzerName);
        if (isMapped) {
            analyzerNameToIdMap.put(analyzerName, analyzerTypeId);
        }
    }

    private synchronized void insureMapsLoaded() {
        if (!isMapped) {
            loadMaps();
            isMapped = true;
        }
    }

    public Map<String, MappedTestName> getMappedTestsForAnalyzer(String analyzerName) {
        insureMapsLoaded();
        return analyzerNameToTestNameMap.get(analyzerName);
    }

    public synchronized void reloadCache() {
        isMapped = false;
    }

    /**
     * Load test mappings into two indexes: 1. Legacy type-based index
     * (analyzerNameToTestNameMap) keyed by AnalyzerType name 2. Per-analyzer index
     * (analyzerIdToTestNameMap) keyed by Analyzer ID (OGC-492)
     */
    private void loadMaps() {
        List<AnalyzerType> typeList = analyzerTypeService.getAll();
        analyzerNameToTestNameMap.clear();
        analyzerIdToTestNameMap.clear();

        analyzerNameToIdMap = new HashMap<>();

        // Build type name → type ID map (legacy index)
        Map<String, AnalyzerType> typeIdToType = new HashMap<>();
        for (AnalyzerType type : typeList) {
            analyzerNameToIdMap.put(type.getName(), type.getId());
            analyzerNameToTestNameMap.put(type.getName(), new HashMap<String, MappedTestName>());
            typeIdToType.put(type.getId(), type);
        }

        // Load test mappings into both indexes
        List<AnalyzerTestMapping> mappingList = analyzerTestMappingService.getAll();

        for (AnalyzerTestMapping mapping : mappingList) {
            MappedTestName mappedTestName = createMappedTestName(testService, mapping);

            String typeId = mapping.getAnalyzerTypeId();
            if (typeId == null) {
                continue;
            }
            AnalyzerType type = typeIdToType.get(typeId);
            if (type == null) {
                continue;
            }

            // Legacy type-based index
            Map<String, MappedTestName> testMap = analyzerNameToTestNameMap.get(type.getName());
            if (testMap != null) {
                testMap.put(mapping.getAnalyzerTestName(), mappedTestName);
            }

            // Per-analyzer index (keyed by analyzer_id when available)
            String analyzerId = mapping.getAnalyzerId();
            if (analyzerId != null && !analyzerId.isEmpty()) {
                analyzerIdToTestNameMap.computeIfAbsent(analyzerId, k -> new HashMap<>())
                        .put(mapping.getAnalyzerTestName(), mappedTestName);
            }
        }
    }

    private MappedTestName createMappedTestName(TestService testService, AnalyzerTestMapping mapping) {

        MappedTestName mappedTest = new MappedTestName();
        mappedTest.setAnalyzerTestName(mapping.getAnalyzerTestName());
        mappedTest.setTestId(mapping.getTestId());
        // Use actual analyzer_id when available, fall back to type_id for legacy
        // (OGC-492)
        mappedTest.setAnalyzerId(
                mapping.getAnalyzerId() != null && !mapping.getAnalyzerId().isEmpty() ? mapping.getAnalyzerId()
                        : mapping.getAnalyzerTypeId());
        if (mapping.getTestId() != null) {
            Test test = new Test();
            test.setId(mapping.getTestId());
            testService.getData(test);
            mappedTest.setOpenElisTestName(TestServiceImpl.getUserLocalizedTestName(test));
        } else {
            mappedTest.setTestId("-1");
            mappedTest.setOpenElisTestName(MessageUtil.getMessage("warning.configuration.needed"));
        }

        return mappedTest;
    }

    public MappedTestName getEmptyMappedTestName(String analyzerName, String analyzerTestName) {
        insureMapsLoaded();
        MappedTestName mappedTest = new MappedTestName();
        mappedTest.setAnalyzerTestName(analyzerTestName);
        mappedTest.setTestId(null);
        mappedTest.setOpenElisTestName(analyzerTestName);
        mappedTest.setAnalyzerId(analyzerNameToIdMap.get(analyzerName));

        return mappedTest;
    }

    public String getAnalyzerIdForName(String analyzerName) {
        insureMapsLoaded();

        return analyzerNameToIdMap.get(analyzerName);
    }
}
