package org.openelisglobal.analyzer.dao;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AstmFieldExtractionConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface AstmFieldExtractionConfigDAO extends BaseDAO<AstmFieldExtractionConfig, String> {
    List<AstmFieldExtractionConfig> findByAnalyzerId(String analyzerId);
}
