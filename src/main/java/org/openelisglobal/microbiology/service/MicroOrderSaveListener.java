package org.openelisglobal.microbiology.service;

import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Runs synchronously inside the shared order-save transaction. */
@Component
public class MicroOrderSaveListener {
    private final MicroOrderRoutingService routing;
    public MicroOrderSaveListener(MicroOrderRoutingService routing) { this.routing = routing; }
    @EventListener
    public void routeSavedOrder(SamplePatientUpdateDataCreatedEvent event) {
        routing.routeOrder(event.getUpdateData().getSample(), event.getUpdateData().getCurrentUserId());
    }
}
