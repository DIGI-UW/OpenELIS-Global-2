package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    List<Analyzer> getAllWithTypes();

    Optional<Analyzer> getWithType(String id);

    Analyzer getAnalyzerByName(String name);

    Optional<Analyzer> getByIpAddress(String ipAddress);

    Optional<Analyzer> getByIpAddressAndPort(String ipAddress, Integer port);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findActiveByListenPort(Integer port);

    Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier);

    Optional<Analyzer> findByIdentifierPatternMatch(List<String> analyzerIdentifiers);

    boolean hasRecentResults(String analyzerId);

    boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus);

    boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus);

    Analyzer setStatusManually(String analyzerId, AnalyzerStatus status, String userId);

    /**
     * Delete an analyzer and all its dependent records (analyzer_plugin_config,
     * analyzer_field, analyzer_field_mapping, analyzer_results, analyzer_error,
     * analyzer_pending_code, etc.).
     *
     * @param analyzer The analyzer entity to delete
     */
    void deleteWithDependents(Analyzer analyzer);

    Optional<Analyzer> findByDiscoveredSourceId(String discoveredSourceId);
}
