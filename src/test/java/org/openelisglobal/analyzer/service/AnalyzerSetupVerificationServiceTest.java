package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.context.ApplicationEventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerSetupVerificationServiceTest {

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Mock
    private AnalyzerFieldMappingService analyzerFieldMappingService;

    @Mock
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Mock
    private AnalyzerPendingCodeService analyzerPendingCodeService;

    @Mock
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Mock
    private QCControlLotService qcControlLotService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditTrailService auditTrailService;

    @InjectMocks
    private AnalyzerSetupVerificationServiceImpl service;

    private Analyzer analyzer;
    private Map<String, Object> pluginConfig;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("2013");
        analyzer.setName("GeneXpert Lab 1");
        when(analyzerService.get("2013")).thenReturn(analyzer);

        AnalyzerTestMapping testMapping = new AnalyzerTestMapping();
        testMapping.setAnalyzerId("2013");
        testMapping.setAnalyzerTestName("MTB");
        testMapping.setTestId("501");
        when(analyzerTestMappingService.getAllForAnalyzer("2013")).thenReturn(List.of(testMapping));
        when(analyzerFieldMappingService.getMappingsForAnalyzer("2013", true)).thenReturn(List.of());
        when(analyzerPendingCodeService.findByAnalyzerId("2013")).thenReturn(List.of());

        pluginConfig = new LinkedHashMap<>();
        pluginConfig.put("profile", Map.of("id", "genexpert-astm", "qcApplicable", true));
        when(analyzerPluginConfigService.getConfigAsMap("2013")).thenAnswer(invocation -> pluginConfig);
        when(analyzerPluginConfigService.getPendingResultValues("2013")).thenReturn(List.of());
        when(analyzerPluginConfigService.getResultValueMappings("2013"))
                .thenReturn(List.of(Map.of("analyzerValue", "DETECTED", "testCode", "MTB", "openelisResultOptionId",
                        "9001", "bindingStatus", "BOUND", "active", true)));

        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setId("rule-1");
        rule.setAnalyzerId("2013");
        rule.setRuleType(AnalyzerQcRule.RuleType.FIELD_EQUALS);
        rule.setTargetField("Q.3");
        rule.setOperand("QC");
        rule.setActive(true);
        when(analyzerQcRuleService.getActiveRulesForAnalyzer("2013")).thenReturn(List.of(rule));

        QCControlLot lot = new QCControlLot();
        lot.setId("lot-1");
        lot.setInstrumentId("2013");
        lot.setTestId("501");
        lot.setLotNumber("LOT-2026-01");
        lot.setStatus("ACTIVE");
        when(qcControlLotService.getActiveControlLotsByInstrument("2013")).thenReturn(List.of(lot));
    }

    @Test
    public void getVerificationStatus_WhenCompleteButUnverified_RequiresConfirmation() {
        Map<String, Object> status = service.getVerificationStatus("2013");

        assertTrue((Boolean) status.get("mappingReady"));
        assertTrue((Boolean) status.get("qcReady"));
        assertFalse((Boolean) status.get("currentlyVerified"));
        assertFalse((Boolean) status.get("readyForActivation"));
        assertEquals("UNVERIFIED", status.get("verificationState"));
        assertTrue(list(status.get("blockers")).contains("SETUP_NOT_VERIFIED"));
        assertTrue(list(status.get("mappingIds")).contains("TEST:MTB"));
        assertTrue(list(status.get("qcIds")).contains("RULE:rule-1"));
        assertTrue(list(status.get("qcIds")).contains("LOT:lot-1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void verifySetup_RecordsFingerprintsActorAndDurableConfigAudit() {
        Map<String, Object> before = service.getVerificationStatus("2013");
        Map<String, Object> request = Map.of("mappingIds", before.get("mappingIds"), "qcIds", before.get("qcIds"));
        when(analyzerPluginConfigService.upsert(eq("2013"), any(), eq("77"))).thenAnswer(invocation -> {
            pluginConfig = invocation.getArgument(1);
            return null;
        });

        Map<String, Object> verified = service.verifySetup("2013", request, "77");

        assertTrue((Boolean) verified.get("currentlyVerified"));
        assertTrue((Boolean) verified.get("readyForActivation"));
        assertEquals("CURRENT", verified.get("verificationState"));
        Map<String, Object> saved = (Map<String, Object>) pluginConfig.get("setupVerification");
        assertEquals("77", saved.get("verifiedBy"));
        assertEquals(before.get("mappingFingerprint"), saved.get("mappingFingerprint"));
        assertEquals(before.get("qcFingerprint"), saved.get("qcFingerprint"));

        ArgumentCaptor<AnalyzerPluginConfig> auditedConfig = ArgumentCaptor.forClass(AnalyzerPluginConfig.class);
        verify(auditTrailService).saveHistory(auditedConfig.capture(), any(AnalyzerPluginConfig.class), eq("77"),
                eq(IActionConstants.AUDIT_TRAIL_UPDATE), eq("analyzer_plugin_config"));
        assertTrue(auditedConfig.getValue().getConfig().contains("\"setupVerification\""));
        assertTrue(auditedConfig.getValue().getConfig().contains("\"verifiedBy\":\"77\""));
        verify(eventPublisher).publishEvent(any(AnalyzerSetupVerifiedEvent.class));
    }

    @Test
    public void getVerificationStatus_AfterMappingChange_MarksVerificationStale() {
        Map<String, Object> before = service.getVerificationStatus("2013");
        pluginConfig.put("setupVerification",
                Map.of("mappingFingerprint", before.get("mappingFingerprint"), "qcFingerprint",
                        before.get("qcFingerprint"), "mappingIds", before.get("mappingIds"), "qcIds",
                        before.get("qcIds"), "verifiedBy", "77", "verifiedAt", "2026-07-24T12:00:00Z"));

        AnalyzerTestMapping changed = new AnalyzerTestMapping();
        changed.setAnalyzerId("2013");
        changed.setAnalyzerTestName("MTB");
        changed.setTestId("999");
        when(analyzerTestMappingService.getAllForAnalyzer("2013")).thenReturn(List.of(changed));

        Map<String, Object> status = service.getVerificationStatus("2013");

        assertFalse((Boolean) status.get("currentlyVerified"));
        assertEquals("STALE", status.get("verificationState"));
        assertTrue(list(status.get("blockers")).contains("MAPPINGS_CHANGED"));
    }

    @Test
    public void verifySetup_WithLegacyUnboundResultMapping_IsRejected() {
        when(analyzerPluginConfigService.getResultValueMappings("2013"))
                .thenReturn(List.of(Map.of("analyzerValue", "DETECTED", "testCode", "MTB", "openelisValue",
                        "POSITIVE", "bindingStatus", "LEGACY_UNBOUND", "active", true)));

        Map<String, Object> status = service.getVerificationStatus("2013");

        assertFalse((Boolean) status.get("mappingReady"));
        assertTrue(list(status.get("blockers")).contains("UNBOUND_RESULT_VALUES"));
        assertThrows(IllegalStateException.class,
                () -> service.verifySetup("2013",
                        Map.of("mappingIds", status.get("mappingIds"), "qcIds", status.get("qcIds")), "77"));
    }

    @Test
    public void verifySetup_WithDifferentConfirmedIds_IsRejected() {
        Map<String, Object> status = service.getVerificationStatus("2013");

        assertThrows(IllegalArgumentException.class, () -> service.verifySetup("2013",
                Map.of("mappingIds", List.of("TEST:OTHER"), "qcIds", status.get("qcIds")), "77"));
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Object value) {
        return (List<String>) value;
    }
}
