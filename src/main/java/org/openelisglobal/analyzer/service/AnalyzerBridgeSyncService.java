package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerBridgeSyncService {

    private static final String CLASS_NAME = "AnalyzerBridgeSyncService";

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

    @Autowired(required = false)
    private BridgeRegistrationService bridgeRegistrationService;

    public void pushAnalyzer(String analyzerId) {
        if (bridgeRegistrationService == null || analyzerId == null || analyzerId.isBlank()) {
            return;
        }
        try {
            Analyzer analyzer = analyzerService.get(analyzerId);
            if (analyzer == null) {
                return;
            }
            if (analyzer.getIpAddress() != null && !analyzer.getIpAddress().isBlank()) {
                String protocol = analyzer.getProtocolVersion() != null && analyzer.getProtocolVersion().isHl7() ? "HL7"
                        : "ASTM";
                bridgeRegistrationService.registerTcp(analyzer.getId(), analyzer.getName(), analyzer.getIpAddress(),
                        analyzer.getPort(), protocol, analyzer.getIdentifierPattern());
            }
            if (analyzer.getImportDirectory() != null && !analyzer.getImportDirectory().isBlank()) {
                List<String> testMappings = analyzerTestMappingService.getAllForAnalyzer(analyzer.getId()).stream()
                        .map(AnalyzerTestMapping::getAnalyzerTestName).distinct().collect(Collectors.toList());
                bridgeRegistrationService.registerFile(analyzer.getId(), analyzer.getName(),
                        analyzer.getImportDirectory(), analyzer.getFilePattern(), analyzer.getColumnMappings(),
                        analyzer.getFileFormat(), analyzer.getDelimiter(), analyzer.getSkipRows(), testMappings);
            }
        } catch (Exception e) {
            LogEvent.logWarn(CLASS_NAME, "pushAnalyzer",
                    "Bridge push failed for analyzer " + analyzerId + ": " + e.getMessage());
        }
    }
}
