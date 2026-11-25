package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.service.BaseObjectService;

public interface AnalyzerService extends BaseObjectService<Analyzer, String> {

    List<Analyzer> getAllWithMapping();

    Optional<Analyzer> getWithMapping(String id);

    Analyzer getAnalyzerByName(String name);

    Optional<Analyzer> getByName(String name);

    Optional<Analyzer> findByBridgeConnectionId(String bridgeConnectionId);

    Optional<Analyzer> findByBridgeConnectionIdForUpdate(String bridgeConnectionId);

    Optional<Analyzer> findByIdForUpdate(String id);

    List<AnalyzerTestCapability> getCapabilitiesForTest(String testId);
}
