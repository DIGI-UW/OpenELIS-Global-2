package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AstmAnalyzerConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFieldExtractionConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFlagMappingDAO;
import org.openelisglobal.analyzer.dao.AstmQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.common.exception.LIMSRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class AstmConfigServiceTest {

    @Mock
    private AstmAnalyzerConfigDAO configDAO;

    @Mock
    private AstmFieldExtractionConfigDAO extractionDAO;

    @Mock
    private AstmFlagMappingDAO flagMappingDAO;

    @Mock
    private AstmQcRuleDAO qcRuleDAO;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AstmConfigServiceImpl service;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(analyzer);
        when(configDAO.findByAnalyzerId("1")).thenReturn(Optional.empty());
        when(configDAO.getAll()).thenReturn(new ArrayList<>());
        when(extractionDAO.findByAnalyzerId("1")).thenReturn(new ArrayList<>());
        when(flagMappingDAO.findByAnalyzerId("1")).thenReturn(new ArrayList<>());
    }

    @Test
    public void updateConfig_ServerRoleWithPort_SavesConfig() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("aggregationMode", "PER_MESSAGE");

        AstmAnalyzerConfig result = service.updateConfig("1", payload);

        assertEquals("SERVER", result.getConnectionRole());
        verify(configDAO, times(2)).insert(any(AstmAnalyzerConfig.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_ServerRoleWithoutPort_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", null);

        service.updateConfig("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_ClientRoleWithoutTarget_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "CLIENT");
        payload.put("clientTargetIp", "");

        service.updateConfig("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_BySessionWithoutWindow_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6500);
        payload.put("aggregationMode", "BY_SESSION");

        service.updateConfig("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_BySessionWindowOutOfRange_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6500);
        payload.put("aggregationMode", "BY_SESSION");
        payload.put("aggregationWindowSeconds", 301);

        service.updateConfig("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_ServerPortConflictWithActiveAnalyzer_ThrowsValidationError() {
        AstmAnalyzerConfig existing = new AstmAnalyzerConfig();
        Analyzer other = new Analyzer();
        other.setId("2");
        other.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        existing.setAnalyzer(other);
        existing.setConnectionRole("SERVER");
        existing.setServerListenPort(6001);
        when(configDAO.getAll()).thenReturn(List.of(existing));

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);

        service.updateConfig("1", payload);
    }

    @Test
    public void updateConfig_ServerPortConflictWithInactiveAnalyzer_AllowsSave() {
        AstmAnalyzerConfig existing = new AstmAnalyzerConfig();
        Analyzer other = new Analyzer();
        other.setId("2");
        other.setStatus(Analyzer.AnalyzerStatus.SETUP);
        existing.setAnalyzer(other);
        existing.setConnectionRole("SERVER");
        existing.setServerListenPort(6001);
        when(configDAO.getAll()).thenReturn(List.of(existing));

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);

        service.updateConfig("1", payload);

        verify(configDAO, times(2)).insert(any(AstmAnalyzerConfig.class));
    }

    @Test
    public void updateConfig_ClientRoleSkipsPortConflictCheck() {
        AstmAnalyzerConfig existing = new AstmAnalyzerConfig();
        Analyzer other = new Analyzer();
        other.setId("2");
        other.setStatus(Analyzer.AnalyzerStatus.ACTIVE);
        existing.setAnalyzer(other);
        existing.setConnectionRole("SERVER");
        existing.setServerListenPort(6001);

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "CLIENT");
        payload.put("clientTargetIp", "10.0.0.5");
        payload.put("clientTargetPort", 9100);

        service.updateConfig("1", payload);

        verify(configDAO, times(2)).insert(any(AstmAnalyzerConfig.class));
    }

    @Test
    public void updateConfig_WithFlagMappings_ReplacesMappings() {
        AstmFlagMapping oldMapping = new AstmFlagMapping();
        oldMapping.setId("old-1");
        when(flagMappingDAO.findByAnalyzerId("1")).thenReturn(List.of(oldMapping));

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("flagMappings", List.of(Map.of("analyzerFlag", "H", "openelisFlag", "HIGH", "isCustom", true),
                Map.of("analyzerFlag", "X", "openelisFlag", "ABNORMAL", "isCustom", false)));

        service.updateConfig("1", payload);

        verify(flagMappingDAO).delete(oldMapping);
        verify(flagMappingDAO, times(2)).insert(any(AstmFlagMapping.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_WithDuplicateAnalyzerFlags_RejectsPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("flagMappings", List.of(Map.of("analyzerFlag", "H", "openelisFlag", "HIGH"),
                Map.of("analyzerFlag", "H", "openelisFlag", "ABNORMAL")));

        service.updateConfig("1", payload);
    }

    @Test
    public void updateConfig_WithExtractionOverrides_ReplacesOverrides() {
        AstmFieldExtractionConfig old = new AstmFieldExtractionConfig();
        old.setId("old-extraction");
        when(extractionDAO.findByAnalyzerId("1")).thenReturn(List.of(old));

        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("extractionOverrides", List.of(Map.of("key", "SPECIMEN_ID_FIELD", "fieldIndex", 4),
                Map.of("key", "TEST_ID_COMPONENT", "fieldIndex", 3, "componentIndex", 5)));

        service.updateConfig("1", payload);

        verify(extractionDAO).delete(old);
        verify(extractionDAO, times(2)).insert(any(AstmFieldExtractionConfig.class));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_WithInvalidExtractionIndex_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("extractionOverrides", List.of(Map.of("key", "SPECIMEN_ID_FIELD", "fieldIndex", 0)));

        service.updateConfig("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void updateConfig_WithUnsupportedExtractionKey_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("extractionOverrides", List.of(Map.of("key", "UNKNOWN_FIELD", "fieldIndex", 1)));

        service.updateConfig("1", payload);
    }

    @Test
    public void getExtractionConfigs_WhenNoOverrides_ReturnsNineDefaults() {
        List<AstmFieldExtractionConfig> defaults = service.getExtractionConfigs("1");

        assertEquals(9, defaults.size());
        assertTrue(defaults.stream().allMatch(c -> Boolean.TRUE.equals(c.getIsDefault())));
        assertTrue(defaults.stream().anyMatch(c -> "SENDER_FIELD".equals(c.getKey())));
    }

    @Test
    public void updateConfig_WithCustomFlagIsCustomFalse_PreservesCustomValue() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("flagMappings", List.of(Map.of("analyzerFlag", "N", "openelisFlag", "NORMAL", "isCustom", false)));

        service.updateConfig("1", payload);

        ArgumentCaptor<AstmFlagMapping> captor = ArgumentCaptor.forClass(AstmFlagMapping.class);
        verify(flagMappingDAO).insert(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getIsCustom());
    }

    @Test
    public void updateConfig_WithDuplicateExtractionKey_DoesNotDeleteExistingOverrides() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("connectionRole", "SERVER");
        payload.put("serverListenPort", 6001);
        payload.put("extractionOverrides", List.of(Map.of("key", "SPECIMEN_ID_FIELD", "fieldIndex", 3),
                Map.of("key", "SPECIMEN_ID_FIELD", "fieldIndex", 4)));

        try {
            service.updateConfig("1", payload);
        } catch (LIMSRuntimeException e) {
            // expected
        }

        verify(extractionDAO, never()).delete(any(AstmFieldExtractionConfig.class));
    }
}
