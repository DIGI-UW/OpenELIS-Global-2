package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {
    List<Analyzer> getAllWithBindings();

    Optional<Analyzer> getWithBinding(String id);

    Analyzer getAnalyzerByName(String name);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId);

    List<AnalyzerTestCapability> getCapabilitiesForTest(String testId);
}
