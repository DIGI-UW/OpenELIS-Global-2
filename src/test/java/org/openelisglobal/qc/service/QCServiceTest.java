package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCRuleConfigDAO;
import org.openelisglobal.qc.valueholder.QCRuleConfig;

@RunWith(MockitoJUnitRunner.class)
public class QCServiceTest {

    @Mock
    private QCRuleConfigDAO ruleConfigDAO;

    @Spy
    private WestgardRuleEngine westgardRuleEngine = new WestgardRuleEngine();

    @InjectMocks
    private QCServiceImpl qcService;

    @Before
    public void setUp() {
        QCRuleConfig rule12s = new QCRuleConfig();
        rule12s.setId(1L);
        rule12s.setRuleCode("12s");
        rule12s.setTestTypeId(1L);
        rule12s.setEnabled(true);

        QCRuleConfig rule13s = new QCRuleConfig();
        rule13s.setId(2L);
        rule13s.setRuleCode("13s");
        rule13s.setTestTypeId(1L);
        rule13s.setEnabled(true);

        when(ruleConfigDAO.findEnabledByTestTypeId(1L)).thenReturn(List.of(rule12s, rule13s));
        when(ruleConfigDAO.findByTestTypeId(1L)).thenReturn(List.of(rule12s, rule13s));
    }

    @Test
    public void testEvaluateQCResult_WithViolation() {
        List<BigDecimal> values = List.of(new BigDecimal("116"));
        BigDecimal mean = new BigDecimal("100");
        BigDecimal sd = new BigDecimal("5");

        Map<String, Object> result = qcService.evaluateQCResult(1L, values, mean, sd);

        assertTrue((Boolean) result.get("hasViolations"));
        @SuppressWarnings("unchecked")
        List<String> violations = (List<String>) result.get("violations");
        assertTrue(violations.contains("12s"));
        assertTrue(violations.contains("13s"));
    }

    @Test
    public void testEvaluateQCResult_NoViolation() {
        List<BigDecimal> values = List.of(new BigDecimal("102"));
        BigDecimal mean = new BigDecimal("100");
        BigDecimal sd = new BigDecimal("5");

        Map<String, Object> result = qcService.evaluateQCResult(1L, values, mean, sd);

        assertFalse((Boolean) result.get("hasViolations"));
    }

    @Test
    public void testCalculateChartData() {
        List<BigDecimal> values = List.of(new BigDecimal("98"), new BigDecimal("100"), new BigDecimal("102"),
                new BigDecimal("99"), new BigDecimal("101"));

        Map<String, Object> chartData = qcService.calculateChartData(values);

        assertNotNull(chartData.get("mean"));
        assertNotNull(chartData.get("sd"));
        assertNotNull(chartData.get("plus2SD"));
        assertNotNull(chartData.get("minus2SD"));
        assertNotNull(chartData.get("plus3SD"));
        assertNotNull(chartData.get("minus3SD"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dataPoints = (List<Map<String, Object>>) chartData.get("dataPoints");
        assertEquals(5, dataPoints.size());
    }

    @Test
    public void testCalculateChartData_EmptyValues() {
        Map<String, Object> chartData = qcService.calculateChartData(List.of());
        assertEquals(BigDecimal.ZERO, chartData.get("mean"));
    }

    @Test
    public void testGetRuleConfigs() {
        List<Map<String, Object>> configs = qcService.getRuleConfigs(1L);
        assertEquals(6, configs.size()); // All 6 Westgard rules
    }

    @Test
    public void testUpdateRuleConfig_NewConfig() {
        when(ruleConfigDAO.findByTestTypeId(2L)).thenReturn(List.of());

        qcService.updateRuleConfig(2L, "12s", false);

        verify(ruleConfigDAO).insert(any(QCRuleConfig.class));
    }

    @Test
    public void testUpdateRuleConfig_ExistingConfig() {
        QCRuleConfig existing = new QCRuleConfig();
        existing.setId(1L);
        existing.setRuleCode("12s");
        existing.setTestTypeId(3L);
        existing.setEnabled(true);
        when(ruleConfigDAO.findByTestTypeId(3L)).thenReturn(List.of(existing));

        qcService.updateRuleConfig(3L, "12s", false);

        verify(ruleConfigDAO).update(any(QCRuleConfig.class));
    }
}
