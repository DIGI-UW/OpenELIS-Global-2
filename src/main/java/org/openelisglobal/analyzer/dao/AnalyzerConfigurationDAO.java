package org.openelisglobal.analyzer.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for AnalyzerConfiguration
 */
public interface AnalyzerConfigurationDAO extends BaseDAO<AnalyzerConfiguration, String> {

    /**
     * Find AnalyzerConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByAnalyzerId(String analyzerId);

    /**
     * Find AnalyzerConfiguration by IP address
     * 
     * @param ipAddress The IP address
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByIpAddress(String ipAddress);

    /**
     * Find AnalyzerConfiguration by analyzer name
     * 
     * @param name The analyzer name
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByAnalyzerName(String name);

    /**
     * Find all analyzer configurations enabled for generic plugin matching.
     *
     * @return list of AnalyzerConfigurations with is_generic_plugin=true and a
     *         non-null identifier_pattern
     */
    List<AnalyzerConfiguration> findGenericPluginConfigsWithPatterns();
}
