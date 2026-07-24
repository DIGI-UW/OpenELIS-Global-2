package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerPluginConfigDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerPluginConfigServiceTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AnalyzerPluginConfigDAO analyzerPluginConfigDAO;

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Mock
    private AnalyzerResultValueOptionService analyzerResultValueOptionService;

    @InjectMocks
    private AnalyzerPluginConfigServiceImpl service;

    @Before
    public void setUp() {
        when(analyzerPluginConfigDAO.update(any(AnalyzerPluginConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void testUpsert_WithValidConfig_Succeeds() {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-1");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));
        when(analyzerService.findActiveByListenPort(17001)).thenReturn(Optional.empty());

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("connectionRole", "SERVER");
        config.put("serverListenPort", 17001);
        config.put("aggregationMode", "BY_SESSION");
        config.put("aggregationWindowSeconds", 60);
        config.put("transforms", Map.of("codeA", Map.of("type", "PASS_THROUGH")));

        AnalyzerPluginConfig result = service.upsert("101", config, "1");

        assertNotNull(result);
        assertTrue(result.getConfig().contains("\"serverListenPort\":17001"));
        verify(analyzerPluginConfigDAO).update(existing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsert_WithInvalidAggregationWindow_ThrowsException() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("aggregationMode", "BY_SESSION");
        config.put("aggregationWindowSeconds", 301);

        service.upsert("101", config, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsert_WithInvalidTransformType_ThrowsException() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("transforms", Map.of("codeA", Map.of("type", "UNKNOWN_TYPE")));

        service.upsert("101", config, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsert_WithPortConflict_ThrowsException() {
        Analyzer conflicting = new Analyzer();
        conflicting.setId("202");
        conflicting.setName("Other Active Analyzer");
        when(analyzerService.findActiveByListenPort(16000)).thenReturn(Optional.of(conflicting));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("connectionRole", "SERVER");
        config.put("serverListenPort", 16000);

        service.upsert("101", config, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsert_WithServerRoleMissingPort_ThrowsException() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("connectionRole", "SERVER");

        service.upsert("101", config, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpsert_WithClientRoleMissingFields_ThrowsException() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("connectionRole", "CLIENT");
        config.put("clientTargetIp", "10.20.30.40");

        service.upsert("101", config, "1");
    }

    @Test
    public void testHasAtLeastOneActiveQcRule_WithActiveRule_ReturnsTrue() {
        when(analyzerQcRuleService.hasAtLeastOneActiveRule("101")).thenReturn(true);

        assertTrue(service.hasAtLeastOneActiveQcRule("101"));
    }

    @Test
    public void testHasAtLeastOneActiveQcRule_WithNoRules_ReturnsFalse() {
        when(analyzerQcRuleService.hasAtLeastOneActiveRule("101")).thenReturn(false);

        assertFalse(service.hasAtLeastOneActiveQcRule("101"));
    }

    @Test
    public void testApplyConfigDefaults_MergesWithExisting() {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-2");
        existing.setAnalyzerId("101");
        existing.setConfig("{\"override\":\"existing\",\"already\":\"set\"}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("override", "default");
        defaults.put("newDefault", List.of("A", "B"));

        service.applyConfigDefaults("101", defaults, "1");

        assertTrue(existing.getConfig().contains("\"override\":\"existing\""));
        assertTrue(existing.getConfig().contains("\"newDefault\":[\"A\",\"B\"]"));
        assertTrue(existing.getConfig().contains("\"already\":\"set\""));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyProfileDefaults_PersistsMappingReviewDefaults() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-profile");
        existing.setAnalyzerId("101");
        existing.setConfig("{\"connectionRole\":\"SERVER\"}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("profileMeta", Map.of("id", "genexpert-astm", "version", "2.1.0", "displayName", "GeneXpert ASTM"));
        profile.put("configDefaults", Map.of("aggregationMode", "BY_SESSION", "qcRules",
                List.of(Map.of("ruleType", "FIELD_EQUALS", "targetField", "Q.3", "operand", "QC"))));
        profile.put("default_test_mappings", List
                .of(Map.of("test_code", "MTB", "test_name_hint", "Mycobacterium tuberculosis", "loinc", "38379-4")));
        profile.put("result_value_mappings",
                List.of(Map.of("analyzer_value", "Detected", "openelis_value", "POSITIVE", "test_code", "MTB")));

        service.applyProfileDefaults("101", profile, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> profileMappings = (List<Map<String, Object>>) persisted.get("default_test_mappings");
        List<Map<String, Object>> resultMappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");
        Map<String, Object> savedProfile = (Map<String, Object>) persisted.get("profile");

        assertEquals("SERVER", persisted.get("connectionRole"));
        assertEquals("BY_SESSION", persisted.get("aggregationMode"));
        assertEquals("MTB", profileMappings.get(0).get("test_code"));
        assertEquals("38379-4", profileMappings.get(0).get("loinc"));
        assertEquals("Detected", resultMappings.get(0).get("analyzerValue"));
        assertEquals("POSITIVE", resultMappings.get(0).get("openelisValue"));
        assertEquals("MTB", resultMappings.get(0).get("testCode"));
        assertEquals(true, resultMappings.get(0).get("active"));
        assertEquals("genexpert-astm", savedProfile.get("id"));
        assertEquals("2.1.0", savedProfile.get("version"));
        assertEquals(true, savedProfile.get("qcApplicable"));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyProfileDefaults_ProfileWithoutDefaultRulesStillRequiresQc() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-profile-no-rules");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("profileMeta", Map.of("id", "genexpert-hl7", "version", "1.2.0", "displayName", "GeneXpert HL7"));
        profile.put("analyzer_name", "Cepheid GeneXpert (HL7 Mode)");
        profile.put("configDefaults", Map.of());

        service.applyProfileDefaults("101", profile, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        Map<String, Object> savedProfile = (Map<String, Object>) persisted.get("profile");

        assertEquals(true, savedProfile.get("qcApplicable"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyProfileDefaults_HonorsExplicitQcOptOut() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-profile-qc-opt-out");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("profileMeta",
                Map.of("id", "non-qc-profile", "version", "1.0.0", "displayName", "Non-QC", "qcApplicable", false));
        profile.put("analyzer_name", "Non-QC analyzer");
        profile.put("configDefaults", Map.of());

        service.applyProfileDefaults("101", profile, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        Map<String, Object> savedProfile = (Map<String, Object>) persisted.get("profile");

        assertEquals(false, savedProfile.get("qcApplicable"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyProfileDefaults_DerivesResultValueMappingsFromTestValues() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-derived");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));
        when(analyzerResultValueOptionService.findOptions("101", "MTB"))
                .thenReturn(List.of(resultOption("result-option-detected", "9001", "Detected"),
                        resultOption("result-option-not-detected", "9002", "Not Detected")));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("default_test_mappings",
                List.of(Map.of("test_code", "MTB", "loinc", "85362-2", "values", List.of("DETECTED", "NOT DETECTED"))));

        service.applyProfileDefaults("101", profile, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> resultMappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");

        assertEquals(2, resultMappings.size());
        assertEquals("MTB", resultMappings.get(0).get("testCode"));
        assertEquals("DETECTED", resultMappings.get(0).get("analyzerValue"));
        assertEquals("result-option-detected", resultMappings.get(0).get("openelisResultOptionId"));
        assertEquals("9001", resultMappings.get(0).get("openelisValue"));
        assertEquals("Detected", resultMappings.get(0).get("openelisLabel"));
        assertEquals("BOUND", resultMappings.get(0).get("bindingStatus"));
        assertEquals("NOT DETECTED", resultMappings.get(1).get("analyzerValue"));
        assertEquals("result-option-not-detected", resultMappings.get(1).get("openelisResultOptionId"));
        assertEquals("BOUND", resultMappings.get(1).get("bindingStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testApplyProfileDefaults_LeavesAmbiguousCatalogMatchUnbound() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-ambiguous");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));
        when(analyzerResultValueOptionService.findOptions("101", "MTB"))
                .thenReturn(List.of(resultOption("result-option-1", "9001", "Detected"),
                        resultOption("result-option-2", "DETECTED", "Detected")));

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("default_test_mappings",
                List.of(Map.of("test_code", "MTB", "loinc", "85362-2", "values", List.of("DETECTED"))));

        service.applyProfileDefaults("101", profile, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> resultMappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");

        assertEquals(1, resultMappings.size());
        assertEquals("LEGACY_UNBOUND", resultMappings.get(0).get("bindingStatus"));
        assertFalse(resultMappings.get(0).containsKey("openelisResultOptionId"));
    }

    @Test
    public void testUpdateResultValueMappings_PreservesOtherPluginConfig() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-3");
        existing.setAnalyzerId("101");
        existing.setConfig(
                "{\"connectionRole\":\"SERVER\",\"serverListenPort\":17001,\"pendingResultValues\":[{\"id\":\"rv-1\",\"status\":\"PENDING\"}]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));
        when(analyzerResultValueOptionService.requireValidOption("101", "MTB", "result-option-1"))
                .thenReturn(resultOption("result-option-1", "9001", "Detected"));

        List<Map<String, Object>> mappings = List.of(Map.of("analyzerValue", "Detected", "openelisResultOptionId",
                "result-option-1", "testCode", "MTB", "active", true));

        Map<String, Object> updated = service.updateResultValueMappings("101", mappings, "1");
        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);

        assertEquals("SERVER", persisted.get("connectionRole"));
        assertEquals(17001, persisted.get("serverListenPort"));
        List<Map<String, Object>> updatedMappings = (List<Map<String, Object>>) updated.get("resultValueMappings");
        List<Map<String, Object>> persistedMappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");
        assertEquals("result-option-1", updatedMappings.get(0).get("openelisResultOptionId"));
        assertEquals("9001", persistedMappings.get(0).get("openelisValue"));
        assertEquals("Detected", persistedMappings.get(0).get("openelisLabel"));
        assertEquals("BOUND", persistedMappings.get(0).get("bindingStatus"));
        assertTrue(persisted.containsKey("pendingResultValues"));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateResultValueMappings_PreservesInactiveLegacyMappingWithoutBinding() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-inactive");
        existing.setAnalyzerId("101");
        existing.setConfig("{\"resultValueMappings\":[]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        List<Map<String, Object>> mappings = List.of(
                Map.of("analyzerValue", "Detected", "openelisResultOptionId", "result-option-1", "testCode", "MTB",
                        "active", true),
                Map.of("analyzerValue", "Obsolete", "openelisValue", "OLD", "testCode", "MTB", "active", false));
        when(analyzerResultValueOptionService.requireValidOption("101", "MTB", "result-option-1"))
                .thenReturn(resultOption("result-option-1", "9001", "Detected"));

        service.updateResultValueMappings("101", mappings, "1");

        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> persistedMappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");
        assertEquals(2, persistedMappings.size());
        assertEquals(false, persistedMappings.get(1).get("active"));
        assertEquals("OLD", persistedMappings.get(1).get("openelisValue"));
        verify(analyzerResultValueOptionService).requireValidOption("101", "MTB", "result-option-1");
    }

    @Test
    public void testResolvePendingResultValue_MapsPendingValueAndAddsMapping() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-4");
        existing.setAnalyzerId("101");
        existing.setConfig(
                "{\"pendingResultValues\":[{\"id\":\"rv-1\",\"analyzerValue\":\"Trace\",\"testCode\":\"MTB\",\"status\":\"PENDING\",\"seenCount\":2}],\"resultValueMappings\":[]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));
        when(analyzerResultValueOptionService.requireValidOption("101", "MTB", "result-option-1"))
                .thenReturn(resultOption("result-option-1", "9001", "Indeterminate"));

        Map<String, Object> resolved = service.resolvePendingResultValue("101", "rv-1",
                Map.of("openelisResultOptionId", "result-option-1"), "1");
        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> pending = (List<Map<String, Object>>) persisted.get("pendingResultValues");
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");

        assertEquals("MAPPED", resolved.get("status"));
        assertEquals("MAPPED", pending.get(0).get("status"));
        assertEquals("Trace", mappings.get(0).get("analyzerValue"));
        assertEquals("result-option-1", mappings.get(0).get("openelisResultOptionId"));
        assertEquals("9001", mappings.get(0).get("openelisValue"));
        assertEquals("Indeterminate", mappings.get(0).get("openelisLabel"));
        assertEquals("BOUND", mappings.get(0).get("bindingStatus"));
        assertEquals("MTB", mappings.get(0).get("testCode"));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }

    @Test
    public void testGetResultValueMappings_MarksLegacyStringMappingUnbound() {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-legacy");
        existing.setAnalyzerId("101");
        existing.setConfig(
                "{\"resultValueMappings\":[{\"analyzerValue\":\"Detected\",\"testCode\":\"MTB\",\"openelisValue\":\"POSITIVE\",\"active\":true}]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        List<Map<String, Object>> mappings = service.getResultValueMappings("101");

        assertEquals("LEGACY_UNBOUND", mappings.get(0).get("bindingStatus"));
    }

    @Test
    public void testUpdateResultValueMappings_RejectsNewFreeTextTarget() {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-free-text");
        existing.setAnalyzerId("101");
        existing.setConfig("{}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.updateResultValueMappings("101", List.of(
                Map.of("analyzerValue", "Detected", "testCode", "MTB", "openelisValue", "POSITIVE", "active", true)),
                "1"));
    }

    private org.openelisglobal.analyzer.form.AnalyzerResultValueOption resultOption(String id, String value,
            String label) {
        org.openelisglobal.analyzer.form.AnalyzerResultValueOption option = new org.openelisglobal.analyzer.form.AnalyzerResultValueOption();
        option.setId(id);
        option.setValue(value);
        option.setLabel(label);
        option.setTestId("501");
        option.setComponentId("component-primary");
        return option;
    }
}
