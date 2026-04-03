package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private AnalyzerPluginConfigDAO analyzerPluginConfigDAO;

    @Mock
    private AnalyzerService analyzerService;

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
        AnalyzerPluginConfig config = new AnalyzerPluginConfig();
        config.setAnalyzerId("101");
        config.setConfig("{\"qcRules\":[{\"isActive\":false},{\"isActive\":true}]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(config));

        assertTrue(service.hasAtLeastOneActiveQcRule("101"));
    }

    @Test
    public void testHasAtLeastOneActiveQcRule_WithNoRules_ReturnsFalse() {
        AnalyzerPluginConfig config = new AnalyzerPluginConfig();
        config.setAnalyzerId("101");
        config.setConfig("{\"qcRules\":[]}");
        when(analyzerPluginConfigDAO.findByAnalyzerId("101")).thenReturn(Optional.of(config));

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
}
