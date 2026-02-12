package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    Analyzer getAnalyzerByName(String name);

    void persistData(Analyzer analyzer, List<AnalyzerTestMapping> testMappings,
            List<AnalyzerTestMapping> existingMappings);

    Optional<Analyzer> getByIpAddress(String ipAddress);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier);

    boolean hasRecentResults(String analyzerId);

    boolean canTransitionTo(String analyzerId, AnalyzerStatus newStatus);

    boolean validateStatusTransition(AnalyzerStatus currentStatus, AnalyzerStatus newStatus);

    Analyzer setStatusManually(String analyzerId, AnalyzerStatus status, String userId);
}
