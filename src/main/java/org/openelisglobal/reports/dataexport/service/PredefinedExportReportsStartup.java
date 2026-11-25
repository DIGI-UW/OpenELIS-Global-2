package org.openelisglobal.reports.dataexport.service;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Writes the predefined Custom Data Export reports once the application has
 * started, as the system user (OGC-1652). A failure is logged and leaves the
 * saved reports as they were; it never stops the start.
 */
@Component
public class PredefinedExportReportsStartup implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private PredefinedExportReports reports;
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
                reports.ensure();
            });
        } catch (RuntimeException e) {
            LogEvent.logError(getClass().getSimpleName(), "onApplicationEvent",
                    "Could not write the predefined export reports: " + e.getMessage());
        }
    }
}
