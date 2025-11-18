package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for AnalyzerConfiguration
 */
public interface AnalyzerConfigurationService extends BaseObjectService<AnalyzerConfiguration, String> {

    /**
     * Get AnalyzerConfiguration by analyzer ID
     * 
     * @param analyzerId The analyzer ID
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> getByAnalyzerId(String analyzerId);

    /**
     * Create AnalyzerConfiguration for an Analyzer
     * 
     * @param analyzer    The analyzer
     * @param ipAddress   IP address
     * @param port        Port number
     * @param testUnitIds Test unit IDs array
     * @return Created AnalyzerConfiguration ID
     */
    String createConfiguration(Analyzer analyzer, String ipAddress, Integer port, List<String> testUnitIds);

    /**
     * Get all AnalyzerConfigurations with their analyzers
     * 
     * @return List of AnalyzerConfigurations
     */
    List<AnalyzerConfiguration> getAllWithAnalyzers();
}
