package org.openelisglobal.analyzer.service;

import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Uses the existing daemon scheduler after startup. Manual retry calls the same
 * service.
 */
@Component
@Profile("!test")
public class AnalyzerUpgradeStartup {
    private final AnalyzerUpgradeService migration;
    private final String actor;

    public AnalyzerUpgradeStartup(AnalyzerUpgradeService migration, @Qualifier("daemonSysUserId") String actor) {
        this.migration = migration;
        this.actor = actor;
    }

    @Scheduled(initialDelay = 30000, fixedDelay = Long.MAX_VALUE)
    public void run() {
        try {
            migration.migrate(Map.of(), actor);
        } catch (RuntimeException exception) {
            LogEvent.logError("Analyzer upgrade remains pending; use the analyzer upgrade retry action", exception);
        }
    }
}
