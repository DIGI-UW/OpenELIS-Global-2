package org.openelisglobal.qaevent.service;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Links existing NCE categories and types to their shipped display keys once
 * the application has started, as the system user (OGC-1507). A failure is
 * logged and leaves the labels as they were; it never stops the start.
 */
@Component
public class NceDisplayKeyLinkerStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private NceDisplayKeyLinker linker;
    @Autowired
    private DaemonContextExecutor daemonContextExecutor;

    private boolean done;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (done) {
            return;
        }
        done = true;
        try {
            daemonContextExecutor.executeAsDaemon(() -> {
                linker.link();
            });
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "onApplicationEvent",
                    "Could not link NCE categories and types to their display keys: " + e.getMessage());
        }
    }
}
