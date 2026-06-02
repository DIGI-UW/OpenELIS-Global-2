package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerQcRuleService extends BaseObjectService<AnalyzerQcRule, String> {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerQcRule> getRulesForAnalyzer(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerQcRule> getActiveRulesForAnalyzer(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean hasAtLeastOneActiveRule(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerQcRule createRule(String analyzerId, AnalyzerQcRule rule, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    AnalyzerQcRule updateRule(String analyzerId, String ruleId, AnalyzerQcRule updates, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void deleteRule(String analyzerId, String ruleId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void validateRule(AnalyzerQcRule rule);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QcRuleDto> getActiveRuleDtosForAnalyzer(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean ruleExists(String analyzerId, AnalyzerQcRule.RuleType ruleType, String targetField, String operand);
}
