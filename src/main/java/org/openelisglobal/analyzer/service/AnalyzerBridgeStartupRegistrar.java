package org.openelisglobal.analyzer.service;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps Bridge runtime registration aligned with OpenELIS desired state. */
@Component
public class AnalyzerBridgeStartupRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerBridgeStartupRegistrar.class);

    private final BridgeRegistrationService bridgeRegistrationService;
    private final Executor executor;

    public AnalyzerBridgeStartupRegistrar(BridgeRegistrationService bridgeRegistrationService,
            @Qualifier("bridgeRegistrationExecutor") Executor executor) {
        this.bridgeRegistrationService = bridgeRegistrationService;
        this.executor = executor;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            executor.execute(this::reconcile);
        }
    }

    /**
     * Periodic idempotent reconciliation lets Bridge recover after a restart. It
     * does not inspect analyzer files or own analyzer runtime behavior.
     */
    @Scheduled(fixedDelayString = "${analyzer.bridge.registration-sync-delay-ms:30000}", initialDelayString = "${analyzer.bridge.registration-sync-initial-delay-ms:30000}")
    public void reconcile() {
        try {
            BridgeRegistrationResult result = bridgeRegistrationService.synchronize();
            if (!result.complete()) {
                logger.warn("Analyzer Bridge registration is not synchronized: {}", result.failure());
            }
        } catch (BridgeRegistrationException exception) {
            logger.error("Analyzer Bridge desired state is invalid: {}", exception.getMessage());
        }
    }
}
