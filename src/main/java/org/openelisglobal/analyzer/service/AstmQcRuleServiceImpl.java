package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.openelisglobal.analyzer.dao.AstmQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AstmQcRuleServiceImpl implements AstmQcRuleService {

    @Autowired
    private AstmQcRuleDAO qcRuleDAO;

    @Autowired
    private AnalyzerService analyzerService;

    @Override
    @Transactional(readOnly = true)
    public List<AstmQcRule> findByAnalyzerId(String analyzerId) {
        return qcRuleDAO.findByAnalyzerId(analyzerId);
    }

    @Override
    @Transactional
    public AstmQcRule create(String analyzerId, Map<String, Object> payload) {
        Analyzer analyzer = analyzerService.get(analyzerId);
        if (analyzer == null) {
            throw new LIMSRuntimeException("Analyzer not found: " + analyzerId);
        }
        AstmQcRule rule = new AstmQcRule();
        rule.setAnalyzer(analyzer);
        rule.setRuleType((String) payload.get("ruleType"));
        rule.setTargetField((String) payload.get("targetField"));
        rule.setOperand((String) payload.get("operand"));
        rule.setIsActive(payload.get("isActive") != null ? (Boolean) payload.get("isActive") : true);
        rule.setSortOrder(payload.get("sortOrder") != null ? ((Number) payload.get("sortOrder")).intValue() : 0);
        qcRuleDAO.insert(rule);
        return qcRuleDAO.get(rule.getId()).orElse(rule);
    }

    @Override
    @Transactional
    public AstmQcRule update(String ruleId, Map<String, Object> payload) {
        AstmQcRule rule = qcRuleDAO.get(ruleId).orElseThrow(() -> new LIMSRuntimeException("QC rule not found"));
        if (payload.containsKey("ruleType")) {
            rule.setRuleType((String) payload.get("ruleType"));
        }
        if (payload.containsKey("targetField")) {
            rule.setTargetField((String) payload.get("targetField"));
        }
        if (payload.containsKey("operand")) {
            rule.setOperand((String) payload.get("operand"));
        }
        if (payload.containsKey("isActive")) {
            rule.setIsActive((Boolean) payload.get("isActive"));
        }
        if (payload.containsKey("sortOrder")) {
            rule.setSortOrder(((Number) payload.get("sortOrder")).intValue());
        }
        return qcRuleDAO.update(rule);
    }

    @Override
    @Transactional
    public void delete(String ruleId) {
        AstmQcRule rule = qcRuleDAO.get(ruleId).orElseThrow(() -> new LIMSRuntimeException("QC rule not found"));
        qcRuleDAO.delete(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean evaluateAsQc(String analyzerId, Map<String, String> extractedFields) {
        List<AstmQcRule> rules = qcRuleDAO.findActiveByAnalyzerId(analyzerId);
        for (AstmQcRule rule : rules) {
            if (matches(rule, extractedFields)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(AstmQcRule rule, Map<String, String> fields) {
        String value = fields.get(rule.getTargetField());
        if (value == null) {
            value = "";
        }
        String operand = rule.getOperand() != null ? rule.getOperand() : "";

        switch (rule.getRuleType()) {
        case "FIELD_EQUALS":
            return value.equals(operand);
        case "SPECIMEN_ID_PREFIX":
            return value.startsWith(operand);
        case "SPECIMEN_ID_PATTERN":
            try {
                return Pattern.compile(operand).matcher(value).find();
            } catch (PatternSyntaxException e) {
                return false;
            }
        case "FIELD_CONTAINS":
            return value.contains(operand);
        default:
            return false;
        }
    }
}
