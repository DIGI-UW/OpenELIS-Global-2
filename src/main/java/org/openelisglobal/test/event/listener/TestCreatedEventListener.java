package org.openelisglobal.test.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.service.OdooTestProductService;
import org.openelisglobal.test.event.TestCreatedEvent;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class TestCreatedEventListener {

    @Autowired
    private OdooTestProductService odooTestProductService;

    @Async
    @EventListener
    public void handleTestCreatedEvent(TestCreatedEvent event) {
        try {
            Test test = event.getTest();
            odooTestProductService.syncTestToOdoo(test);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Error processing TestCreatedEvent: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
