package org.openelisglobal.qc.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface QCService {

    Map<String, Object> evaluateQCResult(Long testTypeId, List<BigDecimal> values, BigDecimal mean, BigDecimal sd);

    Map<String, Object> calculateChartData(List<BigDecimal> values);

    List<Map<String, Object>> getRuleConfigs(Long testTypeId);

    void updateRuleConfig(Long testTypeId, String ruleCode, boolean enabled);
}
