package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    List<Analyzer> getAllWithTypes();

    Optional<Analyzer> getWithType(String id);

    Analyzer getAnalyzerByName(String name);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByIdentifierPatternMatch(String analyzerIdentifier);

    Optional<Analyzer> findByIdentifierPatternMatch(List<String> analyzerIdentifiers);

    Optional<Analyzer> findByDiscoveredSourceId(String discoveredSourceId);
}
