package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig;
import org.openelisglobal.common.dao.BaseDAO;

public interface AnalyzerPluginConfigDAO extends BaseDAO<AnalyzerPluginConfig, String> {
    Optional<AnalyzerPluginConfig> findByAnalyzerId(String analyzerId);
}
