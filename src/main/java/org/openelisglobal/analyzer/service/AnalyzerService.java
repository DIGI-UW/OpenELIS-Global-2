package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<Analyzer> getAllWithTypes();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getWithType(String id);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Analyzer getAnalyzerByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void persistTestMappings(String analyzerId, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getByIpAddress(String ipAddress);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getByIpAddressAndPort(String ipAddress, Integer port);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> findActiveByListenPort(Integer port);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> findByIdentifierPatternMatch(List<String> analyzerIdentifiers);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean hasRecentResults(String analyzerId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Analyzer setStatusManually(String analyzerId, AnalyzerStatus status, String userId);

    /**
     * Auto-create test mappings from a default config's
     * {@code default_test_mappings} array. Each mapping entry with a valid LOINC
     * code is resolved to an OpenELIS test and persisted as an AnalyzerTestMapping.
     *
     * @param analyzerId The analyzer's ID to associate mappings with
     * @param config     Parsed default config JSON containing
     *                   "default_test_mappings"
     * @param sysUserId  The authenticated user's ID for audit attribution
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void autoCreateTestMappings(String analyzerId, Map<String, Object> config, String sysUserId);

    /**
     * Delete an analyzer and all its dependent records (analyzer_plugin_config,
     * analyzer_field, analyzer_field_mapping, analyzer_results, analyzer_error,
     * analyzer_pending_code, etc.).
     *
     * @param analyzer The analyzer entity to delete
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    void deleteWithDependents(Analyzer analyzer);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> findByDiscoveredSourceId(String discoveredSourceId);
}
