package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;

/** Transactional owner of the OpenELIS portion of an analyzer instance. */
public interface AnalyzerInstanceLocalStateService {

    AnalyzerInstanceState create(AnalyzerInstanceRequest request, String actor);

    List<AnalyzerInstanceState> list();

    AnalyzerInstanceState get(String analyzerId);

    AnalyzerInstanceState update(String analyzerId, AnalyzerInstanceRequest request, String actor);

    AnalyzerInstanceState attachBridgeConnection(String analyzerId, String bridgeConnectionId, String actor);
}
