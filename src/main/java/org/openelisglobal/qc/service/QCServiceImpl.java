package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.qc.dao.QCRuleConfigDAO;
import org.openelisglobal.qc.valueholder.QCRuleConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QCServiceImpl implements QCService {

    @Autowired
    private WestgardRuleEngine westgardRuleEngine;

    @Autowired
    private QCRuleConfigDAO ruleConfigDAO;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> evaluateQCResult(Long testTypeId, List<BigDecimal> values, BigDecimal mean,
            BigDecimal sd) {
        List<QCRuleConfig> enabledConfigs = ruleConfigDAO.findEnabledByTestTypeId(testTypeId);
        List<String> enabledRules = enabledConfigs.stream().map(QCRuleConfig::getRuleCode).collect(Collectors.toList());

        List<String> violations = westgardRuleEngine.evaluate(values, mean, sd, enabledRules);

        Map<String, Object> result = new HashMap<>();
        result.put("testTypeId", testTypeId);
        result.put("violations", violations);
        result.put("hasViolations", !violations.isEmpty());
        result.put("enabledRules", enabledRules);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> calculateChartData(List<BigDecimal> values) {
        Map<String, Object> chartData = new HashMap<>();

        if (values == null || values.isEmpty()) {
            chartData.put("mean", BigDecimal.ZERO);
            chartData.put("sd", BigDecimal.ZERO);
            chartData.put("dataPoints", List.of());
            return chartData;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        BigDecimal mean = sum.divide(new BigDecimal(values.size()), 5, RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            BigDecimal diff = v.subtract(mean);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }

        BigDecimal sd = BigDecimal.ZERO;
        if (values.size() > 1) {
            BigDecimal variance = varianceSum.divide(new BigDecimal(values.size() - 1), 10, RoundingMode.HALF_UP);
            sd = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(5, RoundingMode.HALF_UP);
        }

        chartData.put("mean", mean);
        chartData.put("sd", sd);
        chartData.put("plus2SD", mean.add(sd.multiply(new BigDecimal("2"))));
        chartData.put("minus2SD", mean.subtract(sd.multiply(new BigDecimal("2"))));
        chartData.put("plus3SD", mean.add(sd.multiply(new BigDecimal("3"))));
        chartData.put("minus3SD", mean.subtract(sd.multiply(new BigDecimal("3"))));

        List<Map<String, Object>> dataPoints = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("index", i);
            point.put("value", values.get(i));
            if (sd.compareTo(BigDecimal.ZERO) != 0) {
                point.put("zScore", values.get(i).subtract(mean).divide(sd, 5, RoundingMode.HALF_UP));
            }
            dataPoints.add(point);
        }
        chartData.put("dataPoints", dataPoints);

        return chartData;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRuleConfigs(Long testTypeId) {
        List<QCRuleConfig> configs = ruleConfigDAO.findByTestTypeId(testTypeId);
        Map<String, String> descriptions = westgardRuleEngine.getRuleDescriptions();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String ruleCode : WestgardRuleEngine.ALL_RULE_CODES) {
            Map<String, Object> ruleInfo = new HashMap<>();
            ruleInfo.put("ruleCode", ruleCode);
            ruleInfo.put("description", descriptions.get(ruleCode));

            boolean enabled = configs.stream().filter(c -> c.getRuleCode().equals(ruleCode)).findFirst()
                    .map(QCRuleConfig::getEnabled).orElse(true);
            ruleInfo.put("enabled", enabled);
            result.add(ruleInfo);
        }
        return result;
    }

    @Override
    public void updateRuleConfig(Long testTypeId, String ruleCode, boolean enabled) {
        List<QCRuleConfig> configs = ruleConfigDAO.findByTestTypeId(testTypeId);
        QCRuleConfig config = configs.stream().filter(c -> c.getRuleCode().equals(ruleCode)).findFirst().orElse(null);

        if (config == null) {
            config = new QCRuleConfig();
            config.setTestTypeId(testTypeId);
            config.setRuleCode(ruleCode);
            config.setEnabled(enabled);
            config.setLastupdated(new Timestamp(System.currentTimeMillis()));
            ruleConfigDAO.insert(config);
        } else {
            config.setEnabled(enabled);
            config.setLastupdated(new Timestamp(System.currentTimeMillis()));
            ruleConfigDAO.update(config);
        }
    }
}
