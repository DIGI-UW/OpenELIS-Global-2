package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

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

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<Analyzer> getAllWithBindings();

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getWithBinding(String id);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Analyzer getAnalyzerByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> getByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<AnalyzerTestCapability> getCapabilitiesForTest(String testId);
}
