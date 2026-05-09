package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AnalyzerQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerQcRuleServiceImpl extends BaseObjectServiceImpl<AnalyzerQcRule, String>
        implements AnalyzerQcRuleService {

    private static final int MAX_RULES_PER_ANALYZER = 50;

    @Autowired
    private AnalyzerQcRuleDAO analyzerQcRuleDAO;

    @Autowired
    private AnalyzerService analyzerService;

    public AnalyzerQcRuleServiceImpl() {
        super(AnalyzerQcRule.class);
    }

    @Override
    protected AnalyzerQcRuleDAO getBaseObjectDAO() {
        return analyzerQcRuleDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerQcRule> getRulesForAnalyzer(String analyzerId) {
        return analyzerQcRuleDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerQcRule> getActiveRulesForAnalyzer(String analyzerId) {
        return analyzerQcRuleDAO.findActiveByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAtLeastOneActiveRule(String analyzerId) {
        return analyzerQcRuleDAO.countActiveByAnalyzerId(analyzerId) > 0;
    }

    @Override
    @Transactional
    public AnalyzerQcRule createRule(String analyzerId, AnalyzerQcRule rule, String sysUserId) {
        if (analyzerService.get(analyzerId) == null) {
            throw new IllegalArgumentException("Analyzer not found: " + analyzerId);
        }

        List<AnalyzerQcRule> existing = analyzerQcRuleDAO.findByAnalyzerId(analyzerId);
        if (existing.size() >= MAX_RULES_PER_ANALYZER) {
            throw new IllegalStateException("Maximum of " + MAX_RULES_PER_ANALYZER + " QC rules per analyzer exceeded");
        }

        validateRule(rule);

        rule.setAnalyzerId(analyzerId);
        rule.setSysUserId(sysUserId);
        if (rule.getDisplayOrder() == 0) {
            rule.setDisplayOrder(existing.size() + 1);
        }

        String id = insert(rule);
        rule.setId(id);
        return rule;
    }

    @Override
    @Transactional
    public AnalyzerQcRule updateRule(String analyzerId, String ruleId, AnalyzerQcRule updates, String sysUserId) {
        AnalyzerQcRule existing;
        try {
            existing = get(ruleId);
        } catch (Exception e) {
            existing = null;
        }
        if (existing == null || !analyzerId.equals(existing.getAnalyzerId())) {
            throw new IllegalArgumentException("QC rule " + ruleId + " not found for analyzer " + analyzerId);
        }

        if (updates.getRuleType() != null) {
            existing.setRuleType(updates.getRuleType());
        }
        if (updates.getTargetField() != null) {
            existing.setTargetField(updates.getTargetField());
        }
        if (updates.getOperand() != null) {
            existing.setOperand(updates.getOperand());
        }
        existing.setActive(updates.isActive());
        if (updates.getDisplayOrder() > 0) {
            existing.setDisplayOrder(updates.getDisplayOrder());
        }
        if (updates.getDescription() != null) {
            existing.setDescription(updates.getDescription());
        }

        validateRule(existing);

        existing.setSysUserId(sysUserId);
        return update(existing);
    }

    @Override
    @Transactional
    public void deleteRule(String analyzerId, String ruleId) {
        analyzerQcRuleDAO.deleteByAnalyzerIdAndRuleId(analyzerId, ruleId);
    }

    @Override
    public void validateRule(AnalyzerQcRule rule) {
        if (rule.getRuleType() == null) {
            throw new IllegalArgumentException("rule_type is required");
        }
        if (rule.getOperand() == null || rule.getOperand().isBlank()) {
            throw new IllegalArgumentException("operand is required");
        }

        switch (rule.getRuleType()) {
        case FIELD_EQUALS, FIELD_CONTAINS -> {
            if (rule.getTargetField() == null || rule.getTargetField().isBlank()) {
                throw new IllegalArgumentException("target_field is required for " + rule.getRuleType());
            }
        }
        case SPECIMEN_ID_PREFIX -> {
            // operand is the prefix string; no special validation beyond non-blank
        }
        case SPECIMEN_ID_PATTERN -> {
            try {
                Pattern.compile(rule.getOperand());
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid regex in operand: " + e.getMessage());
            }
        }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ruleExists(String analyzerId, AnalyzerQcRule.RuleType ruleType, String targetField, String operand) {
        return analyzerQcRuleDAO.findByAnalyzerId(analyzerId).stream()
                .anyMatch(r -> r.getRuleType() == ruleType && java.util.Objects.equals(r.getTargetField(), targetField)
                        && java.util.Objects.equals(r.getOperand(), operand));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QcRuleDto> getActiveRuleDtosForAnalyzer(String analyzerId) {
        return analyzerQcRuleDAO.findActiveByAnalyzerId(analyzerId).stream()
                .map(r -> new QcRuleDto(r.getRuleType().name(), r.getTargetField(), r.getOperand())).toList();
    }
}
