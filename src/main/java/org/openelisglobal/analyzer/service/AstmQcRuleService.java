package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;

public interface AstmQcRuleService {

    List<AstmQcRule> findByAnalyzerId(String analyzerId);

    AstmQcRule create(String analyzerId, Map<String, Object> payload);

    AstmQcRule update(String ruleId, Map<String, Object> payload);

    void delete(String ruleId);

    boolean evaluateAsQc(String analyzerId, Map<String, String> extractedFields);
}
