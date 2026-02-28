package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AstmAnalyzerConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmAnalyzerConfigDAO extends BaseDAO<AstmAnalyzerConfig, String> {
    Optional<AstmAnalyzerConfig> findByAnalyzerId(String analyzerId);
}
