package org.openelisglobal.analyzer.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;

/**
 * DAO interface for AnalyzerConfiguration
 */
public interface AnalyzerConfigurationDAO extends BaseDAO<AnalyzerConfiguration, String> {
    
    /**
     * Find AnalyzerConfiguration by analyzer ID
     * @param analyzerId The analyzer ID
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByAnalyzerId(String analyzerId);
}

