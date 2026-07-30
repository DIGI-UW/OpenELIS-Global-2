package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
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

    @Autowired
    private AnalyzerTestMappingService analyzerTestMappingService;

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
                            analyzer.getPort(), protocol, analyzer.getIdentifierPattern())) {
                        registered++;
                        bridgeReachable = true;
                    }
                }

                // FILE analyzers — read from unified Analyzer entity
                if (analyzer.getImportDirectory() != null && !analyzer.getImportDirectory().isBlank()) {
                    List<String> testMappings = analyzerTestMappingService.getAllForAnalyzer(analyzerId).stream()
                            .map(AnalyzerTestMapping::getAnalyzerTestName).distinct().collect(Collectors.toList());
                    if (bridgeRegistrationService.registerFile(analyzerId, analyzerName, analyzer.getImportDirectory(),
                            analyzer.getFilePattern(), analyzer.getColumnMappings(), analyzer.getFileFormat(),
                            analyzer.getDelimiter(), analyzer.getSkipRows(), testMappings)) {
                        registered++;
                        bridgeReachable = true;
                    }
                }
            }

            // If we tried to register but nothing succeeded, bridge is likely down
            if (!bridgeReachable) {
                return -1;
            }

            // v5 §4.3: after the per-analyzer push loop, call the bridge's
            // PUT /api/analyzers/sync endpoint with the full list for
            // full-state reconciliation. This handles cases where the bridge
            // has stale entries from deleted analyzers, or where a partial
            // failure during the loop above left the bridge in an inconsistent
            // state. The webapp is the config authority; bridge's local
            // state must mirror it.
            try {
                List<java.util.Map<String, Object>> syncPayloads = new java.util.ArrayList<>();
                for (Analyzer a : analyzers) {
                    if (a.getStatus() == Analyzer.AnalyzerStatus.DELETED) {
                        continue;
                    }
                    if (a.getIpAddress() != null && !a.getIpAddress().isBlank()) {
                        java.util.Map<String, Object> p = new java.util.LinkedHashMap<>();
                        p.put("oeAnalyzerId", a.getId());
                        p.put("sourceId", a.getIpAddress());
                        p.put("name", a.getName());
                        p.put("protocol",
                                a.getProtocolVersion() != null && a.getProtocolVersion().isHl7() ? "HL7" : "ASTM");
                        // Without these the bridge's /sync endpoint resets the entry's
                        // qcRules + controlLots to empty lists on every webapp restart.
                        bridgeRegistrationService.attachQcRules(p, a.getId());
                        bridgeRegistrationService.attachControlLots(p, a.getId());
                        syncPayloads.add(p);
                    }
                    if (a.getImportDirectory() != null && !a.getImportDirectory().isBlank()) {
                        List<String> tm = analyzerTestMappingService.getAllForAnalyzer(a.getId()).stream()
                                .map(AnalyzerTestMapping::getAnalyzerTestName).distinct().collect(Collectors.toList());
                        java.util.Map<String, Object> p = new java.util.LinkedHashMap<>();
                        p.put("oeAnalyzerId", a.getId());
                        p.put("sourceId", a.getImportDirectory());
                        p.put("name", a.getName());
                        p.put("protocol", "FILE");
                        p.put("filePattern", a.getFilePattern() != null ? a.getFilePattern() : "");
                        if (a.getColumnMappings() != null && !a.getColumnMappings().isEmpty()) {
                            p.put("columnMappings", a.getColumnMappings());
                        }
                        if (a.getFileFormat() != null && !a.getFileFormat().isBlank()) {
                            p.put("fileFormat", a.getFileFormat());
                        }
                        if (a.getDelimiter() != null && !a.getDelimiter().isEmpty()) {
                            p.put("delimiter", a.getDelimiter());
                        }
                        if (a.getSkipRows() != null && a.getSkipRows() > 0) {
                            p.put("skipRows", a.getSkipRows());
                        }
                        if (tm != null && !tm.isEmpty()) {
                            p.put("testMappings", tm);
                        }
                        bridgeRegistrationService.attachQcRules(p, a.getId());
                        bridgeRegistrationService.attachControlLots(p, a.getId());
                        syncPayloads.add(p);
                    }
                }
                boolean syncOk = bridgeRegistrationService.syncAll(syncPayloads);
                if (syncOk) {
                    logger.info("Bridge full-state sync reconciled {} analyzers", syncPayloads.size());
                } else {
                    logger.warn(
                            "Bridge full-state sync failed — per-analyzer pushes above succeeded but reconciliation did not");
                }
            } catch (Exception e) {
                logger.warn("Bridge full-state sync threw: {}", e.getMessage());
                // Non-fatal: per-analyzer pushes already succeeded
            }

            return registered;
        } catch (Exception e) {
            logger.debug("Bridge registration attempt failed: {}", e.getMessage());
            return -1;
        }
    }
}
