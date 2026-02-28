package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerMappingPreviewExtendedTest {

    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;

    @Mock
    private AstmConfigService astmConfigService;

    @Mock
    private AstmQcRuleService astmQcRuleService;

    @Mock
    private AstmTestMappingConfigService astmTestMappingConfigService;

    private AnalyzerMappingPreviewServiceImpl service;

    @Before
    public void setUp() {
        service = new AnalyzerMappingPreviewServiceImpl(analyzerFieldMappingDAO, analyzerFieldDAO);
        ReflectionTestUtils.setField(service, "astmConfigService", astmConfigService);
        ReflectionTestUtils.setField(service, "astmQcRuleService", astmQcRuleService);
        ReflectionTestUtils.setField(service, "astmTestMappingConfigService", astmTestMappingConfigService);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("1")).thenReturn(List.of());
    }

    @Test
    public void previewMapping_IncludesTransformsQcExtractionFlagsAndWarnings() {
        String astmMessage = String.join("\n", "H|\\^&|||SENDER-X|", "O|1||QC-123|",
                "R|1||^^^TBIL|>5.2|mg/dL||H||F||||20260227");

        List<AstmFieldExtractionConfig> extraction = defaultExtractionConfigs();
        when(astmConfigService.getExtractionConfigs("1")).thenReturn(extraction);
        when(astmQcRuleService.evaluateAsQc(org.mockito.ArgumentMatchers.eq("1"), anyMap())).thenReturn(true);

        AstmFlagMapping fm = new AstmFlagMapping();
        fm.setAnalyzerFlag("H");
        fm.setOpenelisFlag("HIGH");
        when(astmConfigService.getFlagMappings("1")).thenReturn(List.of(fm));

        AstmTestMappingConfig transform = new AstmTestMappingConfig();
        transform.setAnalyzerTestName("TBIL");
        transform.setTransformType("GREATER_LESS_FLAG");
        transform.setIsActive(true);
        when(astmTestMappingConfigService.findByAnalyzerId("1")).thenReturn(List.of(transform));
        when(astmTestMappingConfigService.applyTransform("TBIL", ">5.2", List.of(transform))).thenReturn("5.2");

        MappingPreviewResult result = service.previewMapping("1", astmMessage, new PreviewOptions());

        assertNotNull(result.getTransformResults());
        assertEquals(1, result.getTransformResults().size());
        assertEquals("5.2", result.getTransformResults().get(0).get("transformedValue"));
        assertEquals("GREATER_LESS_FLAG", result.getTransformResults().get(0).get("transformType"));

        assertNotNull(result.getQcRuleEvaluation());
        assertEquals(true, result.getQcRuleEvaluation().get("isQc"));

        assertNotNull(result.getExtractionApplied());
        assertEquals(9, result.getExtractionApplied().size());
        assertTrue(result.getExtractionApplied().stream().anyMatch(e -> "SENDER_FIELD".equals(e.get("key"))));

        assertNotNull(result.getFlagMappings());
        assertEquals(1, result.getFlagMappings().size());
        assertEquals("HIGH", result.getFlagMappings().get(0).get("openelisFlag"));
    }

    @Test
    public void previewMapping_WhenNoTransformConfigured_AddsUnmappedCodeWarning() {
        String astmMessage = String.join("\n", "H|\\^&|||SENDER-X|", "O|1||PAT-100|",
                "R|1||^^^UNMAPPED|7.1|mg/dL|||F||||20260227");

        when(astmConfigService.getExtractionConfigs("1")).thenReturn(defaultExtractionConfigs());
        when(astmQcRuleService.evaluateAsQc(org.mockito.ArgumentMatchers.eq("1"), anyMap())).thenReturn(false);
        when(astmConfigService.getFlagMappings("1")).thenReturn(List.of());
        when(astmTestMappingConfigService.findByAnalyzerId("1")).thenReturn(List.of());
        when(astmTestMappingConfigService.applyTransform("UNMAPPED", "7.1", List.of())).thenReturn("7.1");

        MappingPreviewResult result = service.previewMapping("1", astmMessage, new PreviewOptions());

        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("Unmapped analyzer test code detected")));
    }

    private List<AstmFieldExtractionConfig> defaultExtractionConfigs() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("1");
        List<AstmFieldExtractionConfig> configs = new ArrayList<>();
        configs.add(extraction(analyzer, "SPECIMEN_ID_FIELD", 3, null));
        configs.add(extraction(analyzer, "TEST_ID_FIELD", 3, null));
        configs.add(extraction(analyzer, "TEST_ID_COMPONENT", 3, 4));
        configs.add(extraction(analyzer, "RESULT_VALUE_FIELD", 4, null));
        configs.add(extraction(analyzer, "RESULT_UNITS_FIELD", 5, null));
        configs.add(extraction(analyzer, "ABNORMAL_FLAG_FIELD", 7, null));
        configs.add(extraction(analyzer, "RESULT_STATUS_FIELD", 9, null));
        configs.add(extraction(analyzer, "RESULT_TIMESTAMP_FIELD", 13, null));
        configs.add(extraction(analyzer, "SENDER_FIELD", 5, null));
        return configs;
    }

    private AstmFieldExtractionConfig extraction(Analyzer analyzer, String key, int fieldIndex,
            Integer componentIndex) {
        AstmFieldExtractionConfig config = new AstmFieldExtractionConfig();
        config.setAnalyzer(analyzer);
        config.setKey(key);
        config.setFieldIndex(fieldIndex);
        config.setComponentIndex(componentIndex);
        return config;
    }
}
