package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmTestMappingConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmTestMappingConfigDAO extends BaseDAO<AstmTestMappingConfig, String> {
    List<AstmTestMappingConfig> findByAnalyzerId(String analyzerId);
}
