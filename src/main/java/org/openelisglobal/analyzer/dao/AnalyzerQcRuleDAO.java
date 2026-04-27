package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerQcRuleDAO extends BaseDAO<AnalyzerQcRule, String> {

    List<AnalyzerQcRule> findByAnalyzerId(String analyzerId);

    List<AnalyzerQcRule> findActiveByAnalyzerId(String analyzerId);

    long countActiveByAnalyzerId(String analyzerId);

    void deleteByAnalyzerIdAndRuleId(String analyzerId, String ruleId);

    boolean existsByAnalyzerIdAndRule(String analyzerId, String ruleType, String targetField, String operand);
}
