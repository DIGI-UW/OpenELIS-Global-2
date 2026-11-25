package org.openelisglobal.eqa.service;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Runs {@link EQAFollowupTargetBackfill} once the application has started, as
 * the system user (OGC-1665). A failure is logged and leaves the register as it
 * was; it never stops the start.
 */
@Component
public class EQAFollowupTargetBackfillStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private EQAFollowupTargetBackfill backfill;
    @Autowired
    private DaemonContextExecutor daemonContextExecutor;
    @Autowired
    @Qualifier("daemonSysUserId")
    private String daemonSysUserId;

    private boolean done;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (done) {
            return;
        }
        done = true;
        try {
            daemonContextExecutor.executeAsDaemon(() -> {
                backfill.backfill(daemonSysUserId);
            });
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "onApplicationEvent",
                    "Could not fill the targets of earlier EQA follow-ups: " + e.getMessage());
        }
    }
}
