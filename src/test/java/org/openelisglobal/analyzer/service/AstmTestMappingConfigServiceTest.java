package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.analyzer.dao.AstmTestMappingConfigDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.common.exception.LIMSRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class AstmTestMappingConfigServiceTest {

    @Mock
    private AstmTestMappingConfigDAO configDAO;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AstmTestMappingConfigServiceImpl service;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);
    }

    @Test
    public void createUpdateDeleteConfig_PerformsCrud() {
        AstmTestMappingConfig existing = new AstmTestMappingConfig();
        existing.setId("cfg-1");
        existing.setAnalyzer(analyzer);
        existing.setAnalyzerTestName("GLU");
        existing.setTransformType("PASS_THROUGH");
        when(configDAO.get("cfg-1")).thenReturn(Optional.of(existing));

        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("analyzerTestName", "GLU");
        createPayload.put("transformType", "PASS_THROUGH");
        service.create("1", createPayload);
        verify(configDAO).insert(any(AstmTestMappingConfig.class));

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("transformType", "VALUE_MAP");
        updatePayload.put("transformConfig", Map.of("mappings", Map.of("POS", "POSITIVE")));
        service.update("cfg-1", updatePayload);
        verify(configDAO).update(existing);

        service.delete("cfg-1");
        verify(configDAO).delete(existing);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void create_WithInvalidTransformType_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("analyzerTestName", "GLU");
        payload.put("transformType", "NOT_VALID");
        service.create("1", payload);
    }

    @Test(expected = LIMSRuntimeException.class)
    public void create_GreaterLessWithoutOperators_ThrowsValidationError() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("analyzerTestName", "TBIL");
        payload.put("transformType", "GREATER_LESS_FLAG");
        payload.put("transformConfig", Map.of());
        service.create("1", payload);
    }

    @Test
    public void create_WithTypeSpecificConfig_PersistsJson() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("analyzerTestName", "TBIL");
        payload.put("transformType", "GREATER_LESS_FLAG");
        payload.put("transformConfig", Map.of("operators", List.of(">", "<", ">=", "<=")));

        service.create("1", payload);

        ArgumentCaptor<AstmTestMappingConfig> captor = ArgumentCaptor.forClass(AstmTestMappingConfig.class);
        verify(configDAO).insert(captor.capture());
        assertTrue(captor.getValue().getTransformConfigJson().contains("operators"));
    }

    @Test
    public void applyTransform_PassThrough_ReturnsRawValue() {
        AstmTestMappingConfig config = cfg("GLU", "PASS_THROUGH", null);
        assertEquals("123.4", service.applyTransform("GLU", "123.4", List.of(config)));
    }

    @Test
    public void applyTransform_GreaterLessFlag_StripsLeadingOperator() {
        AstmTestMappingConfig config = cfg("TBIL", "GREATER_LESS_FLAG", "{\"operators\":[\">\",\"<\",\">=\",\"<=\"]}");
        assertEquals("5.2", service.applyTransform("TBIL", ">5.2", List.of(config)));
    }

    @Test
    public void applyTransform_ValueMap_MapsKnownCodes() {
        AstmTestMappingConfig config = cfg("HIV", "VALUE_MAP",
                "{\"mappings\":{\"POS\":\"POSITIVE\",\"NEG\":\"NEGATIVE\"}}");
        assertEquals("POSITIVE", service.applyTransform("HIV", "POS", List.of(config)));
    }

    @Test
    public void applyTransform_ThresholdClassify_ReturnsMatchingLabel() {
        AstmTestMappingConfig config = cfg("CD4", "THRESHOLD_CLASSIFY",
                "{\"thresholds\":[{\"op\":\"<\",\"value\":200,\"label\":\"CRITICAL_LOW\"},{\"op\":\">=\",\"value\":200,\"label\":\"NORMAL\"}]}");
        assertEquals("CRITICAL_LOW", service.applyTransform("CD4", "150", List.of(config)));
    }

    @Test
    public void applyTransform_CodedLookup_ResolvesLookupValue() {
        AstmTestMappingConfig config = cfg("COVID", "CODED_LOOKUP",
                "{\"lookup\":{\"D\":\"DETECTED\",\"ND\":\"NOT_DETECTED\"}}");
        assertEquals("DETECTED", service.applyTransform("COVID", "D", List.of(config)));
    }

    private AstmTestMappingConfig cfg(String analyzerTestName, String type, String json) {
        AstmTestMappingConfig config = new AstmTestMappingConfig();
        config.setAnalyzerTestName(analyzerTestName);
        config.setTransformType(type);
        config.setTransformConfigJson(json);
        config.setIsActive(true);
        return config;
    }
}
