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

    protected AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
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
    private final HashMap<String, Map<String, MappedTestName>> analyzerNameToTestNameMap = new HashMap<>();
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
        // This will look for a mapping for the analyzer and if it is found will then
        // look for a mapping for the test name
        Map<String, MappedTestName> testMap = getMappedTestsForAnalyzer(analyzerName);

        if (testMap != null) {
            return testMap.get(analyzerTestName);
        }

        return null;
    }

    public void registerPluginAnalyzer(String analyzerName, String analyzerId) {
        requestTODBName.put(analyzerName, analyzerName);
        if (isMapped) {
            analyzerNameToIdMap.put(analyzerName, analyzerId);
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

    private void loadMaps() {
        List<Analyzer> analyzerList = analyzerService.getAll();
        analyzerNameToTestNameMap.clear();

        analyzerNameToIdMap = new HashMap<>();

        for (Analyzer analyzer : analyzerList) {
            analyzerNameToIdMap.put(analyzer.getName(), analyzer.getId());
            analyzerNameToTestNameMap.put(analyzer.getName(), new HashMap<String, MappedTestName>());
        }

        List<AnalyzerTestMapping> mappingList = analyzerTestMappingService.getAll();

        for (AnalyzerTestMapping mapping : mappingList) {
            MappedTestName mappedTestName = createMappedTestName(testService, mapping);

            Analyzer analyzer = new Analyzer();
            analyzer.setId(mapping.getAnalyzerId());
            analyzer = analyzerService.get(analyzer.getId());

            Map<String, MappedTestName> testMap = analyzerNameToTestNameMap.get(analyzer.getName());
            if (testMap != null) {
                testMap.put(mapping.getAnalyzerTestName(), mappedTestName);
            }
        }
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
