package org.openelisglobal.eqa.scheduler;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.eqa.service.EQAFhirExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the provider↔participant FHIR exchange on the delivery-reconcile
 * cadence: a provider takes in participant reports from its own store, a
 * participant takes in score reports from its provider's store. Both passes are
 * no-ops when the store they read is not configured, so a single-instance
 * deployment sees nothing new.
 */
@Component
public class EQAFhirExchangeScheduler {

    @Autowired
    private EQAFhirExchangeService exchangeService;

    @Scheduled(initialDelay = 90 * 1000, fixedRateString = "${org.openelisglobal.remote.poll.frequency:120000}")
    public void takeInParticipantReports() {
        try {
            int applied = exchangeService.pollParticipantReports();
            if (applied > 0) {
                LogEvent.logInfo(getClass().getSimpleName(), "takeInParticipantReports",
                        applied + " participant report(s) taken in from the store");
            }
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "takeInParticipantReports", e.getMessage());
        }
    }

    @Scheduled(initialDelay = 105 * 1000, fixedRateString = "${org.openelisglobal.remote.poll.frequency:120000}")
    public void takeInScoreReports() {
        try {
            int applied = exchangeService.pollScoreReports();
            if (applied > 0) {
                LogEvent.logInfo(getClass().getSimpleName(), "takeInScoreReports",
                        applied + " score report(s) taken in from the provider's store");
            }
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "takeInScoreReports", e.getMessage());
        }
    }
}
