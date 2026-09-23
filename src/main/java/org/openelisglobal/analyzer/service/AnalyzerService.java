package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {

    /**
     * Compile-only compatibility for generic artifacts consumed by shared CI.
     * Bridge owns analyzer identification, so this method never matches an
     * analyzer.
     */
    @Deprecated(forRemoval = true)
    default Optional<Analyzer> findByIdentifierPatternMatch(String identifier) {
        return Optional.empty();
    }

    @Deprecated(forRemoval = true)
    default Optional<Analyzer> findByIdentifierPatternMatch(List<String> identifiers) {
        return Optional.empty();
    }

    List<Analyzer> getAllWithBindings();

    Optional<Analyzer> getWithBinding(String id);

    Analyzer getAnalyzerByName(String name);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId);

    Optional<Analyzer> findByBridgeConnectionIdForUpdate(String bridgeConnectionId);

    List<AnalyzerTestCapability> getCapabilitiesForTest(String testId);
}
