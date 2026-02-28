package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerFieldDAO;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.dao.AstmAnalyzerConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFieldExtractionConfigDAO;
import org.openelisglobal.analyzer.dao.AstmFlagMappingDAO;
import org.openelisglobal.analyzer.dao.AstmQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.analyzer.valueholder.AstmFlagMapping;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class AstmConfigIntegrationTest {

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
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;
    @Mock
    private AnalyzerFieldDAO analyzerFieldDAO;
    @Mock
    private AstmTestMappingConfigService astmTestMappingConfigService;

    private Analyzer analyzer;
    private AstmConfigServiceImpl astmConfigService;
    private AnalyzerStatusTransitionServiceImpl transitionService;
    private AnalyzerMappingPreviewServiceImpl previewService;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("1");
        analyzer.setStatus(Analyzer.AnalyzerStatus.VALIDATION);
        analyzer.setActive(true);

        when(analyzerService.get("1")).thenReturn(analyzer);
        when(configDAO.findByAnalyzerId("1")).thenReturn(Optional.empty());
        when(configDAO.getAll()).thenReturn(new ArrayList<>());
        when(extractionDAO.findByAnalyzerId("1")).thenReturn(new ArrayList<>());
        when(flagMappingDAO.findByAnalyzerId("1")).thenReturn(new ArrayList<>());
        when(analyzerService.update(any(Analyzer.class))).thenReturn(analyzer);
        when(analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId("1")).thenReturn(List.of());

        astmConfigService = new AstmConfigServiceImpl();
        ReflectionTestUtils.setField(astmConfigService, "configDAO", configDAO);
        ReflectionTestUtils.setField(astmConfigService, "extractionDAO", extractionDAO);
        ReflectionTestUtils.setField(astmConfigService, "flagMappingDAO", flagMappingDAO);
        ReflectionTestUtils.setField(astmConfigService, "qcRuleDAO", qcRuleDAO);
        ReflectionTestUtils.setField(astmConfigService, "analyzerService", analyzerService);

        transitionService = new AnalyzerStatusTransitionServiceImpl();
        ReflectionTestUtils.setField(transitionService, "analyzerService", analyzerService);
        ReflectionTestUtils.setField(transitionService, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(transitionService, "applicationContext", applicationContext);
        when(applicationContext.getBean(AstmConfigService.class)).thenReturn(astmConfigService);

        previewService = new AnalyzerMappingPreviewServiceImpl(analyzerFieldMappingDAO, analyzerFieldDAO);
        ReflectionTestUtils.setField(previewService, "astmConfigService", astmConfigService);
        ReflectionTestUtils.setField(previewService, "astmQcRuleService", new AstmQcRuleService() {
            @Override
            public List<AstmQcRule> findByAnalyzerId(String analyzerId) {
                return List.of();
            }

            @Override
            public AstmQcRule create(String analyzerId, Map<String, Object> payload) {
                return null;
            }

            @Override
            public AstmQcRule update(String ruleId, Map<String, Object> payload) {
                return null;
            }

            @Override
            public void delete(String ruleId) {
            }

            @Override
            public boolean evaluateAsQc(String analyzerId, Map<String, String> extractedFields) {
                return extractedFields.getOrDefault("SPECIMEN_ID_FIELD", "").startsWith("QC-");
            }
        });
        ReflectionTestUtils.setField(previewService, "astmTestMappingConfigService", astmTestMappingConfigService);
    }

    @Test
    public void qcActivationAndPreviewFlow_WorksEndToEnd() {
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of());
        try {
            transitionService.transitionToActive("1");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isEmpty());
        }

        AstmQcRule activeRule = new AstmQcRule();
        activeRule.setId("rule-1");
        activeRule.setRuleType("SPECIMEN_ID_PREFIX");
        activeRule.setTargetField("SPECIMEN_ID_FIELD");
        activeRule.setOperand("QC-");
        activeRule.setIsActive(true);
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(activeRule));

        Map<String, Object> configUpdate = Map.of("connectionRole", "SERVER", "serverListenPort", 6001, "aggregationMode",
                "PER_MESSAGE", "extractionOverrides", List.of(Map.of("key", "SPECIMEN_ID_FIELD", "fieldIndex", 3),
                        Map.of("key", "TEST_ID_FIELD", "fieldIndex", 3), Map.of("key", "TEST_ID_COMPONENT", "fieldIndex", 3,
                                "componentIndex", 4),
                        Map.of("key", "RESULT_VALUE_FIELD", "fieldIndex", 4),
                        Map.of("key", "RESULT_UNITS_FIELD", "fieldIndex", 5),
                        Map.of("key", "ABNORMAL_FLAG_FIELD", "fieldIndex", 7),
                        Map.of("key", "RESULT_STATUS_FIELD", "fieldIndex", 9),
                        Map.of("key", "RESULT_TIMESTAMP_FIELD", "fieldIndex", 13),
                        Map.of("key", "SENDER_FIELD", "fieldIndex", 5)),
                "flagMappings", List.of(Map.of("analyzerFlag", "H", "openelisFlag", "HIGH", "isCustom", true)));
        astmConfigService.updateConfig("1", configUpdate);

        List<AstmFieldExtractionConfig> extractions = new ArrayList<>();
        extractions.add(extraction("SPECIMEN_ID_FIELD", 3, null));
        extractions.add(extraction("TEST_ID_FIELD", 3, null));
        extractions.add(extraction("TEST_ID_COMPONENT", 3, 4));
        extractions.add(extraction("RESULT_VALUE_FIELD", 4, null));
        extractions.add(extraction("RESULT_UNITS_FIELD", 5, null));
        extractions.add(extraction("ABNORMAL_FLAG_FIELD", 7, null));
        extractions.add(extraction("RESULT_STATUS_FIELD", 9, null));
        extractions.add(extraction("RESULT_TIMESTAMP_FIELD", 13, null));
        extractions.add(extraction("SENDER_FIELD", 5, null));
        when(extractionDAO.findByAnalyzerId("1")).thenReturn(extractions);

        AstmFlagMapping flagMapping = new AstmFlagMapping();
        flagMapping.setAnalyzerFlag("H");
        flagMapping.setOpenelisFlag("HIGH");
        when(flagMappingDAO.findByAnalyzerId("1")).thenReturn(List.of(flagMapping));

        AstmTestMappingConfig transform = new AstmTestMappingConfig();
        transform.setAnalyzerTestName("TBIL");
        transform.setTransformType("GREATER_LESS_FLAG");
        transform.setIsActive(true);
        List<AstmTestMappingConfig> transforms = List.of(transform);
        when(astmTestMappingConfigService.findByAnalyzerId("1")).thenReturn(transforms);
        when(astmTestMappingConfigService.applyTransform("TBIL", ">5.2", transforms)).thenReturn("5.2");

        Analyzer activated = transitionService.transitionToActive("1");
        assertEquals(Analyzer.AnalyzerStatus.ACTIVE, activated.getStatus());

        String astmMessage = String.join("\n", "H|\\^&|||SENDER-X|", "O|1||QC-123|", "R|1||^^^TBIL|>5.2|mg/dL||H||F||||20260227",
                "R|2||^^^UNKNOWN|7.0|mg/dL|||F||||20260227");
        MappingPreviewResult preview = previewService.previewMapping("1", astmMessage, new PreviewOptions());

        assertFalse(preview.getTransformResults().isEmpty());
        assertTrue(preview.getTransformResults().stream().anyMatch(r -> "TBIL".equals(r.get("analyzerTestName"))
                && "5.2".equals(r.get("transformedValue"))));
        assertEquals(true, preview.getQcRuleEvaluation().get("isQc"));
        assertFalse(preview.getFlagMappings().isEmpty());
    }

    private AstmFieldExtractionConfig extraction(String key, int fieldIndex, Integer componentIndex) {
        AstmFieldExtractionConfig cfg = new AstmFieldExtractionConfig();
        cfg.setAnalyzer(analyzer);
        cfg.setKey(key);
        cfg.setFieldIndex(fieldIndex);
        cfg.setComponentIndex(componentIndex);
        cfg.setIsDefault(false);
        return cfg;
    }
}
