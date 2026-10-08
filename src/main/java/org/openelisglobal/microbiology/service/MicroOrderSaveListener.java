package org.openelisglobal.microbiology.service;

import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Runs synchronously inside the shared order-save transaction. */
@Component
public class MicroOrderSaveListener {
    private final MicroOrderRoutingService routing;

    public MicroOrderSaveListener(MicroOrderRoutingService routing) {
        this.routing = routing;
    }

    @EventListener
    public void routeSavedOrder(SamplePatientUpdateDataCreatedEvent event) {
        routing.routeOrder(event.getUpdateData().getSample(), event.getUpdateData().getCurrentUserId());
    }

    // Collection resolves requested-specimen identity later in the same save.
    // Waiting until before commit preserves that ownership across transfers,
    // while a routing failure still rolls back the whole clinical transaction.
    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT)
    public void routeCreatedAnalysis(org.openelisglobal.analysis.valueholder.AnalysisCreatedEvent event) {
        routing.routeAnalysis(event.analysis(), event.actor());
    }
}
