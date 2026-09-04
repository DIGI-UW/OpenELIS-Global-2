package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerPluginConfigDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerPluginConfigServiceTest {

    @Mock
    private AnalyzerPluginConfigDAO analyzerPluginConfigDAO;

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
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("aggregationMode", "BY_SESSION");
        config.put("aggregationWindowSeconds", 60);
        config.put("transforms", Map.of("codeA", Map.of("type", "PASS_THROUGH")));

        AnalyzerPluginConfig result = service.upsert("101", config, "1");

        assertNotNull(result);
        assertTrue(result.getConfig().contains("\"aggregationWindowSeconds\":60"));
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

}
