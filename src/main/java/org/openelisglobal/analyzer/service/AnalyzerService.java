package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    List<Analyzer> getAllWithTypes();

    Optional<Analyzer> getWithType(String id);

    Analyzer getAnalyzerByName(String name);

    void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);

    void persistTestMappings(String analyzerTypeId, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);

    Optional<Analyzer> getByIpAddress(String ipAddress);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier);

    boolean hasRecentResults(String analyzerId);

    boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus);

    boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus);

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
    void autoCreateTestMappings(String analyzerId, Map<String, Object> config, String sysUserId);
}
