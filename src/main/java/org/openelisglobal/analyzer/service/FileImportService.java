package org.openelisglobal.analyzer.service;

import java.util.Map;
import org.openelisglobal.common.service.CrossDomainService;

/**
 * Service for FILE analyzer configuration.
 *
 * <p>
 * File PARSING is owned by the analyzer bridge — OE does not parse files. This
 * service writes profile-driven config to the Analyzer entity and registers
 * FILE analyzers with the bridge.
 */
@CrossDomainService(callers = "analyzer file import pipeline — internal infrastructure")
public interface FileImportService {

    /**
     * Auto-create FILE config on the Analyzer entity from a loaded profile. Called
     * during analyzer creation when the profile protocol is FILE.
     */
    void autoCreateFromProfile(String analyzerId, Map<String, Object> configData, String analyzerName,
            String sysUserId);
}
