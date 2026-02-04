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
     * Get AnalyzerConfiguration by IP address
     * 
     * @param ipAddress The IP address
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> getByIpAddress(String ipAddress);

    /**
     * Get AnalyzerConfiguration by analyzer name
     * 
     * @param name The analyzer name
     * @return Optional AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> getByAnalyzerName(String name);

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

    /**
     * Find a generic-plugin AnalyzerConfiguration whose identifier_pattern matches
     * the given analyzer identifier.
     *
     * @param analyzerIdentifier identifier extracted from inbound analyzer message
     *                           (e.g. ASTM H-segment)
     * @return Optional matching AnalyzerConfiguration
     */
    Optional<AnalyzerConfiguration> findByIdentifierPatternMatch(String analyzerIdentifier);

    /**
     * Check if analyzer has recent results within the soft delete window
     * 
     * @param analyzerId The analyzer ID to check
     * @return true if analyzer has results within SOFT_DELETE_WINDOW_DAYS, false
     *         otherwise
     */
    boolean hasRecentResults(String analyzerId);

    // === Status Transition Validation Methods (T151c) ===

    /**
     * Check if analyzer can transition to a new status
     * 
     * @param analyzerId The analyzer ID
     * @param newStatus  The target status
     * @return true if transition is valid, false otherwise
     */
    boolean canTransitionTo(String analyzerId, AnalyzerConfiguration.AnalyzerStatus newStatus);

    /**
     * Validate status transition between two statuses
     * 
     * @param currentStatus Current analyzer status
     * @param newStatus     Target status
     * @return true if transition is valid, false otherwise
     */
    boolean validateStatusTransition(AnalyzerConfiguration.AnalyzerStatus currentStatus,
            AnalyzerConfiguration.AnalyzerStatus newStatus);

    /**
     * Manually set analyzer status (for manual overrides like INACTIVE)
     * 
     * @param analyzerId The analyzer ID
     * @param status     The new status (only INACTIVE, SETUP, VALIDATION allowed
     *                   manually)
     * @param userId     The user performing the action (for audit trail)
     * @return Updated AnalyzerConfiguration
     */
    AnalyzerConfiguration setStatusManually(String analyzerId, AnalyzerConfiguration.AnalyzerStatus status,
            String userId);
}
