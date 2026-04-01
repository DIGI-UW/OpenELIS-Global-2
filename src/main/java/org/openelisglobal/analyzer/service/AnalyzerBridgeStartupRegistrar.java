package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Pushes analyzer registrations to the bridge on OE startup with retry.
 *
 * <p>
 * Event-driven registration — no polling. Sync triggers:
 * <ul>
 * <li>OE startup → pushes all analyzers (retries until bridge is
 * reachable)</li>
 * <li>Bridge startup → bridge pulls from OE (covers bridge-started-later
 * case)</li>
 * <li>Analyzer CRUD → controller pushes immediately</li>
 * </ul>
 *
 * <p>
 * Both OE and bridge retry independently on their startup events. Whichever
 * starts first keeps trying until the other is up.
 */
@Component
public class AnalyzerBridgeStartupRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerBridgeStartupRegistrar.class);
    private static final int MAX_RETRIES = 10;
    private static final long INITIAL_BACKOFF_MS = 5_000;
    private static final long MAX_BACKOFF_MS = 60_000;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private FileImportService fileImportService;

    @Autowired
    private BridgeRegistrationService bridgeRegistrationService;

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        // Run async so OE startup isn't blocked waiting for bridge
        CompletableFuture.runAsync(this::pushAllWithRetry);
    }

    private void pushAllWithRetry() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            int registered = pushAllToBridge();
            if (registered >= 0) {
                // Success (0 = no analyzers configured, which is fine)
                logger.info("Bridge registration complete: {} bindings pushed (attempt {})", registered, attempt);
                return;
            }
            // registered == -1 means bridge unreachable
            long backoff = Math.min(INITIAL_BACKOFF_MS * (1L << (attempt - 1)), MAX_BACKOFF_MS);
            logger.info("Bridge not reachable, retrying in {}ms (attempt {}/{})", backoff, attempt, MAX_RETRIES);
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        logger.warn("Bridge registration failed after {} attempts — bridge will pull from OE on its startup",
                MAX_RETRIES);
    }

    /**
     * Push all active analyzers to bridge.
     *
     * @return number of successful registrations, or -1 if bridge is unreachable
     */
    private int pushAllToBridge() {
        try {
            List<Analyzer> analyzers = analyzerService.getAllWithTypes();
            if (analyzers.isEmpty()) {
                return 0;
            }

            int registered = 0;
            boolean bridgeReachable = false;

            for (Analyzer analyzer : analyzers) {
                if (analyzer.getStatus() == Analyzer.AnalyzerStatus.DELETED) {
                    continue;
                }

                String analyzerId = analyzer.getId();
                String analyzerName = analyzer.getName();

                // TCP analyzers (ASTM/HL7)
                if (analyzer.getIpAddress() != null && !analyzer.getIpAddress().isBlank()) {
                    String protocol = analyzer.getProtocolVersion() != null && analyzer.getProtocolVersion().isHl7()
                            ? "HL7"
                            : "ASTM";
                    if (bridgeRegistrationService.registerTcp(analyzerId, analyzerName, analyzer.getIpAddress(),
                            analyzer.getPort(), protocol)) {
                        registered++;
                        bridgeReachable = true;
                    }
                }

                // FILE analyzers
                try {
                    Integer analyzerIdInt = Integer.valueOf(analyzerId);
                    Optional<FileImportConfiguration> fileConfig = fileImportService.getByAnalyzerId(analyzerIdInt);
                    if (fileConfig.isPresent()) {
                        FileImportConfiguration fc = fileConfig.get();
                        if (bridgeRegistrationService.registerFile(analyzerId, analyzerName, fc.getImportDirectory(),
                                fc.getFilePattern(), fc.getColumnMappings())) {
                            registered++;
                            bridgeReachable = true;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // If we tried to register but nothing succeeded, bridge is likely down
            return bridgeReachable ? registered : -1;
        } catch (Exception e) {
            logger.debug("Bridge registration attempt failed: {}", e.getMessage());
            return -1;
        }
    }
}
