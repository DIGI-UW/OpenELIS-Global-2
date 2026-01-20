package org.openelisglobal.test.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.event.TestCreatedEvent;
import org.openelisglobal.test.service.middleware.TestMiddlewareSyncService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TestMiddlewareSyncEventListener {

    @Autowired
    private TestMiddlewareSyncService testMiddlewareSyncService;

    @Async
    @EventListener
    @Order(3)
    public void handleTestCreatedEvent(TestCreatedEvent event) {
        try {
            Test test = event.getTest();
            boolean isUpdate = event.isUpdate();

            String operation = isUpdate ? "UPDATE" : "CREATE";
            LogEvent.logInfo(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Syncing test to middleware: " + test.getDescription() + " (operation=" + operation + ", guid="
                            + test.getGuid() + ")");

            testMiddlewareSyncService.syncTestToMiddleware(test, isUpdate);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Error processing TestCreatedEvent for middleware sync: " + e.getMessage());
            LogEvent.logError(this.getClass().getSimpleName(), "handleTestCreatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
