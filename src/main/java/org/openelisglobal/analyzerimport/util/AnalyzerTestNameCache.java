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
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;

public class AnalyzerTestNameCache {

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
    private volatile Map<String, Map<String, MappedTestName>> analyzerNameToTestNameMap = new HashMap<>();
    // Per-analyzer index: keyed by Analyzer.id (e.g., "6" for BC-5380)
    // Provides correct isolation when multiple analyzers share a generic type
    // (OGC-492)
    private volatile Map<String, Map<String, MappedTestName>> analyzerIdToTestNameMap = new HashMap<>();
    private volatile Map<String, String> analyzerNameToIdMap;
    private Map<String, String> requestTODBName = new HashMap<>();
    private volatile boolean initialLoadDone = false;

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
        ensureInitialLoad();
        List<String> nameList = new ArrayList<>();
        nameList.addAll(analyzerNameToIdMap.keySet());
        return nameList;
    }

    public synchronized MappedTestName getMappedTest(String analyzerName, String analyzerTestName) {
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
    public synchronized MappedTestName getMappedTestByAnalyzerId(String analyzerId, String testCode) {
        ensureInitialLoad();
        Map<String, MappedTestName> testMap = analyzerIdToTestNameMap.get(analyzerId);
        if (testMap != null) {
            MappedTestName result = testMap.get(testCode);
            if (result == null) {
                org.openelisglobal.common.log.LogEvent.logWarn("AnalyzerTestNameCache", "getMappedTestByAnalyzerId",
                        "Cache HIT for analyzer " + analyzerId + " (" + testMap.size() + " codes: " + testMap.keySet()
                                + ") but testCode '" + testCode + "' not found");
            }
            return result;
        }
        org.openelisglobal.common.log.LogEvent.logWarn("AnalyzerTestNameCache", "getMappedTestByAnalyzerId",
                "Cache MISS: no entry for analyzerId=" + analyzerId + " (cache has "
                        + analyzerIdToTestNameMap.keySet().size() + " analyzers: " + analyzerIdToTestNameMap.keySet()
                        + ")");
        return null;
    }

    /**
     * Register an analyzer name in the request-to-DB name map. Used by legacy
     * plugins to register their name for URL-based lookups.
     */
    public void registerAnalyzerName(String analyzerName) {
        requestTODBName.put(analyzerName, analyzerName);
    }

    private synchronized void ensureInitialLoad() {
        if (!initialLoadDone) {
            loadMaps();
            initialLoadDone = true;
            org.openelisglobal.common.log.LogEvent.logInfo("AnalyzerTestNameCache", "ensureInitialLoad",
                    "Initial cache load: " + analyzerIdToTestNameMap.size() + " analyzers, "
                            + analyzerIdToTestNameMap.values().stream().mapToInt(Map::size).sum() + " total mappings");
        }
    }

    public synchronized Map<String, MappedTestName> getMappedTestsForAnalyzer(String analyzerName) {
        ensureInitialLoad();
        return analyzerNameToTestNameMap.get(analyzerName);
    }

    /**
     * Eagerly reload the cache. Called after test mappings change (e.g., analyzer
     * creation). Replaces the old lazy-invalidation pattern that set a flag and
     * hoped the next caller would reload — which had a race condition when loads
     * overlapped with invalidations.
     */
    public synchronized void reloadCache() {
        loadMaps();
        org.openelisglobal.common.log.LogEvent.logInfo("AnalyzerTestNameCache", "reloadCache",
                "Cache reloaded: " + analyzerIdToTestNameMap.size() + " analyzers, "
                        + analyzerIdToTestNameMap.values().stream().mapToInt(Map::size).sum() + " total mappings");
    }

    /**
     * Load test mappings keyed by analyzer. Two indexes: - analyzerIdToTestNameMap:
     * analyzerId → testCode → MappedTestName (primary) - analyzerNameToTestNameMap:
     * analyzerName → testCode → MappedTestName (for legacy readers that use name
     * constants like SYSMEX_XT2000_NAME)
     */
    private void loadMaps() {
        // Build entirely new maps, then swap references atomically.
        // This eliminates the window where maps are cleared but not yet rebuilt,
        // which caused concurrent readers to see empty/inconsistent state.
        HashMap<String, Map<String, MappedTestName>> newNameMap = new HashMap<>();
        HashMap<String, Map<String, MappedTestName>> newIdMap = new HashMap<>();
        HashMap<String, String> newNameToIdMap = new HashMap<>();

        // Build analyzer ID → name map
        AnalyzerService analyzerServiceLocal = SpringContext.getBean(AnalyzerService.class);
        List<Analyzer> analyzerList = analyzerServiceLocal.getAll();
        Map<String, String> idToName = new HashMap<>();
        for (Analyzer analyzer : analyzerList) {
            idToName.put(analyzer.getId(), analyzer.getName());
            newNameToIdMap.put(analyzer.getName(), analyzer.getId());
        }

        // Load all test mappings — PK is now (analyzer_id, analyzer_test_name)
        List<AnalyzerTestMapping> mappingList = analyzerTestMappingService.getAll();

        for (AnalyzerTestMapping mapping : mappingList) {
            String analyzerId = mapping.getAnalyzerId();
            if (analyzerId == null || analyzerId.isEmpty()) {
                continue;
            }

            MappedTestName mappedTestName = createMappedTestName(testService, mapping);

            // Primary index: by analyzer ID
            newIdMap.computeIfAbsent(analyzerId, k -> new HashMap<>()).put(mapping.getAnalyzerTestName(),
                    mappedTestName);

            // Secondary index: by analyzer name (for legacy readers)
            String analyzerName = idToName.get(analyzerId);
            if (analyzerName != null) {
                newNameMap.computeIfAbsent(analyzerName, k -> new HashMap<>()).put(mapping.getAnalyzerTestName(),
                        mappedTestName);
            }
        }

        // Atomic swap — readers see either the old complete map or the new complete map
        analyzerIdToTestNameMap = newIdMap;
        analyzerNameToTestNameMap = newNameMap;
        analyzerNameToIdMap = newNameToIdMap;
    }

    private MappedTestName createMappedTestName(TestService testService, AnalyzerTestMapping mapping) {

        MappedTestName mappedTest = new MappedTestName();
        mappedTest.setAnalyzerTestName(mapping.getAnalyzerTestName());
        mappedTest.setTestId(mapping.getTestId());
        mappedTest.setAnalyzerId(mapping.getAnalyzerId());
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
        ensureInitialLoad();
        MappedTestName mappedTest = new MappedTestName();
        mappedTest.setAnalyzerTestName(analyzerTestName);
        mappedTest.setTestId(null);
        mappedTest.setOpenElisTestName(analyzerTestName);
        mappedTest.setAnalyzerId(analyzerNameToIdMap.get(analyzerName));

        return mappedTest;
    }

    public String getAnalyzerIdForName(String analyzerName) {
        ensureInitialLoad();

        return analyzerNameToIdMap.get(analyzerName);
    }
}
