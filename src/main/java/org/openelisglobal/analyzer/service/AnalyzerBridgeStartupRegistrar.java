package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Re-registers analyzers with the bridge on startup.
 *
 * This ensures bridge-owned FILE watcher state is restored after bridge
 * restarts.
 */
@Component
public class AnalyzerBridgeStartupRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerBridgeStartupRegistrar.class);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private BridgeRegistrationService bridgeRegistrationService;

    @EventListener(ContextRefreshedEvent.class)
    public void reRegisterActiveAnalyzers(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        try {
            List<Analyzer> analyzers = analyzerService.getAllWithTypes();
            int registered = 0;

            for (Analyzer analyzer : analyzers) {
                if (analyzer.getStatus() == Analyzer.AnalyzerStatus.DELETED) {
                    continue;
                }

                String analyzerId = analyzer.getId();
                String analyzerName = analyzer.getName();

                if (analyzer.getIpAddress() != null && !analyzer.getIpAddress().isBlank()) {
                    String protocol = analyzer.getProtocolVersion() != null && analyzer.getProtocolVersion().isHl7()
                            ? "HL7"
                            : "ASTM";
                    if (bridgeRegistrationService.registerTcp(analyzerId, analyzerName, analyzer.getIpAddress(),
                            analyzer.getPort(), protocol)) {
                        registered++;
                    }
                }

                try {
                    Integer analyzerIdInt = Integer.valueOf(analyzerId);
                    Optional<FileImportConfiguration> fileConfig = fileImportService.getByAnalyzerId(analyzerIdInt);
                    if (fileConfig.isPresent()) {
                        FileImportConfiguration fc = fileConfig.get();
                        if (bridgeRegistrationService.registerFile(analyzerId, analyzerName, fc.getImportDirectory(),
                                fc.getFilePattern(), fc.getColumnMappings())) {
                            registered++;
                        }
                    }
                } catch (NumberFormatException ignored) {
                    logger.debug("Skipping FILE bridge re-registration for non-integer analyzer id {}", analyzerId);
                }
            }

            logger.info("Bridge startup registration complete. Registered {} transport bindings", registered);
        } catch (Exception e) {
            logger.error("Bridge startup registration failed", e);
        }
    }
}
