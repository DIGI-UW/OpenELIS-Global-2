package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Composes real persisted analyzer mapping/QC state into the bridge contract.
 * Only the outbound HTTP client is mocked.
 */
public class AnalyzerQcBridgeContractIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long ANALYZER_ID = 95402L;
    private static final long TEST_ID = 995402L;
    private static final String LOT_ID = "95402000-0000-0000-0000-000000000001";

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Autowired
    private QCControlLotService qcControlLotService;

    @Autowired
    private BridgeRegistrationService bridgeRegistrationService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;
    private BridgeRegistrationService bridgeTarget;
    private BridgeHttpClient originalBridgeHttpClient;
    private String originalBridgeBaseUrl;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        ensureSystemUser();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, loinc, is_active, guid, lastupdated)"
                        + " VALUES (?, 'QC Bridge Test', 'QC Bridge Test', '94500-6', 'Y', ?, NOW())",
                TEST_ID, "99540200-0000-0000-0000-000000000001");
        jdbc.update("INSERT INTO clinlims.analyzer"
                + " (id, name, analyzer_type, is_active, status, ip_address, port, last_updated)"
                + " VALUES (?, 'TEST-QC-Bridge-Contract', 'MOLECULAR', true, 'SETUP', '198.51.100.42', 5380,"
                + " NOW())", ANALYZER_ID);

        bridgeTarget = (BridgeRegistrationService) AopTestUtils.getUltimateTargetObject(bridgeRegistrationService);
        originalBridgeHttpClient = (BridgeHttpClient) ReflectionTestUtils.getField(bridgeTarget, "bridgeHttpClient");
        originalBridgeBaseUrl = (String) ReflectionTestUtils.getField(bridgeTarget, "bridgeBaseUrl");
    }

    @After
    public void tearDown() {
        if (bridgeTarget != null) {
            ReflectionTestUtils.setField(bridgeTarget, "bridgeHttpClient", originalBridgeHttpClient);
            ReflectionTestUtils.setField(bridgeTarget, "bridgeBaseUrl", originalBridgeBaseUrl);
        }
        cleanup();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void persistedMappingRuleAndControlLotProduceCompleteRegistrationPayload() throws Exception {
        AnalyzerTestMapping mapping = new AnalyzerTestMapping();
        mapping.setAnalyzerId(String.valueOf(ANALYZER_ID));
        mapping.setAnalyzerTestName("MTB");
        mapping.setTestId(String.valueOf(TEST_ID));
        mapping.setSysUserId(TEST_SYS_USER_ID);
        analyzerTestMappingService.insert(mapping);

        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(AnalyzerQcRule.RuleType.FIELD_EQUALS);
        rule.setTargetField("Q.3");
        rule.setOperand("QC");
        rule.setActive(true);
        analyzerQcRuleService.createRule(String.valueOf(ANALYZER_ID), rule, TEST_SYS_USER_ID);

        QCControlLot lot = new QCControlLot();
        lot.setId(LOT_ID);
        lot.setProductName("QC Bridge Control");
        lot.setLotNumber("QC-BRIDGE-LOT");
        lot.setManufacturer("OpenELIS Test");
        lot.setControlLevel("L1");
        lot.setTestId(String.valueOf(TEST_ID));
        lot.setInstrumentId(String.valueOf(ANALYZER_ID));
        lot.setCalculationMethod("MANUFACTURER_FIXED");
        lot.setManufacturerMean(10.0);
        lot.setManufacturerStdDev(1.0);
        lot.setSystemUserId(1);
        lot.setSysUserId(TEST_SYS_USER_ID);
        QCControlLot persistedLot = qcControlLotService.createControlLot(lot);
        assertEquals("ACTIVE", persistedLot.getStatus());

        BridgeHttpClient bridgeHttpClient = mock(BridgeHttpClient.class);
        when(bridgeHttpClient.post(anyString(), anyString(), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "{}"));
        ReflectionTestUtils.setField(bridgeTarget, "bridgeHttpClient", bridgeHttpClient);
        ReflectionTestUtils.setField(bridgeTarget, "bridgeBaseUrl", "https://bridge.test");

        assertTrue(bridgeRegistrationService.registerTcp(String.valueOf(ANALYZER_ID), "QC Bridge Analyzer",
                "198.51.100.42", 5380, "ASTM", "QC-BRIDGE"));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(anyString(), body.capture(), any(Duration.class));
        Map<String, Object> payload = new ObjectMapper().readValue(body.getValue(),
                new TypeReference<Map<String, Object>>() {
                });

        assertEquals(Map.of("MTB", "94500-6"), payload.get("testCodeLoinc"));
        List<Map<String, Object>> qcRules = (List<Map<String, Object>>) payload.get("qcRules");
        assertEquals(1, qcRules.size());
        assertEquals(Map.of("ruleType", "FIELD_EQUALS", "targetField", "Q.3", "operand", "QC"), qcRules.get(0));
        List<Map<String, Object>> controlLots = (List<Map<String, Object>>) payload.get("controlLots");
        assertEquals(1, controlLots.size());
        assertEquals("QC-BRIDGE-LOT", controlLots.get(0).get("lotNumber"));
        assertEquals("L1", controlLots.get(0).get("controlLevel"));
        assertEquals(String.valueOf(TEST_ID), controlLots.get(0).get("testId"));
    }

    private void ensureSystemUser() {
        jdbc.update("INSERT INTO clinlims.system_user"
                + " (id, login_name, last_name, first_name, is_active, is_employee, external_id, lastupdated)"
                + " VALUES (1, 'admin', 'admin', 'admin', 'Y', 'Y', 'admin', NOW())" + " ON CONFLICT (id) DO NOTHING");
    }

    private void cleanup() {
        if (jdbc == null) {
            return;
        }
        jdbc.update("DELETE FROM clinlims.westgard_rule_config WHERE test_id = ? AND instrument_id = ?", TEST_ID,
                ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.qc_statistics WHERE control_lot_id = ?", LOT_ID);
        jdbc.update("DELETE FROM clinlims.qc_control_lot WHERE id = ?", LOT_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_qc_rule WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer_test_map WHERE analyzer_id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.analyzer WHERE id = ?", ANALYZER_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }
}
