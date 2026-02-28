package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmQcRuleDAO extends BaseDAO<AstmQcRule, String> {
    List<AstmQcRule> findByAnalyzerId(String analyzerId);

    List<AstmQcRule> findActiveByAnalyzerId(String analyzerId);
}
