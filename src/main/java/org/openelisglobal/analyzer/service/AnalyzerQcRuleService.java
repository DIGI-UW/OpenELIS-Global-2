package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerQcRuleService extends BaseObjectService<AnalyzerQcRule, String> {

    List<AnalyzerQcRule> getRulesForAnalyzer(String analyzerId);

    List<AnalyzerQcRule> getActiveRulesForAnalyzer(String analyzerId);

    boolean hasAtLeastOneActiveRule(String analyzerId);

    AnalyzerQcRule createRule(String analyzerId, AnalyzerQcRule rule, String sysUserId);

    AnalyzerQcRule updateRule(String analyzerId, String ruleId, AnalyzerQcRule updates, String sysUserId);

    void deleteRule(String analyzerId, String ruleId);

    void validateRule(AnalyzerQcRule rule);

    List<QcRuleDto> getActiveRuleDtosForAnalyzer(String analyzerId);

    boolean ruleExists(String analyzerId, AnalyzerQcRule.RuleType ruleType, String targetField, String operand);
}
