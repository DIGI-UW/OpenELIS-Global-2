package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
    public void testUpdateResultValueMappings_PreservesOtherPluginConfig() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-3");
        existing.setAnalyzerId("101");
        existing.setConfig(
                "{\"connectionRole\":\"SERVER\",\"serverListenPort\":17001,\"pendingResultValues\":[{\"id\":\"rv-1\",\"status\":\"PENDING\"}]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        List<Map<String, Object>> mappings = List.of(
                Map.of("analyzerValue", "Detected", "openelisValue", "POSITIVE", "testCode", "MTB", "active", true));

        Map<String, Object> updated = service.updateResultValueMappings("101", mappings, "1");
        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);

        assertEquals("SERVER", persisted.get("connectionRole"));
        assertEquals(17001, persisted.get("serverListenPort"));
        assertEquals(mappings, updated.get("resultValueMappings"));
        assertEquals(mappings, persisted.get("resultValueMappings"));
        assertTrue(persisted.containsKey("pendingResultValues"));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }

    @Test
    public void testResolvePendingResultValue_MapsPendingValueAndAddsMapping() throws Exception {
        AnalyzerPluginConfig existing = new AnalyzerPluginConfig();
        existing.setId("cfg-4");
        existing.setAnalyzerId("101");
        existing.setConfig(
                "{\"pendingResultValues\":[{\"id\":\"rv-1\",\"analyzerValue\":\"Trace\",\"testCode\":\"MTB\",\"status\":\"PENDING\",\"seenCount\":2}],\"resultValueMappings\":[]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(existing));

        Map<String, Object> resolved = service.resolvePendingResultValue("101", "rv-1",
                Map.of("openelisValue", "INDETERMINATE"), "1");
        Map<String, Object> persisted = objectMapper.readValue(existing.getConfig(), MAP_TYPE);
        List<Map<String, Object>> pending = (List<Map<String, Object>>) persisted.get("pendingResultValues");
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) persisted.get("resultValueMappings");

        assertEquals("MAPPED", resolved.get("status"));
        assertEquals("MAPPED", pending.get(0).get("status"));
        assertEquals("Trace", mappings.get(0).get("analyzerValue"));
        assertEquals("INDETERMINATE", mappings.get(0).get("openelisValue"));
        assertEquals("MTB", mappings.get(0).get("testCode"));
        verify(analyzerPluginConfigDAO).update(eq(existing));
    }
}
