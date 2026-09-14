package org.openelisglobal.reports.dataexport.service;

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.openelisglobal.reports.dataexport.form.ExportJobView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * One job per application worker; generation never holds an HTTP request open.
 */
@Component
public class ReportingWorker {
    private final String worker = UUID.randomUUID().toString();
    @Autowired
    private ReportingJobService jobs;
    @Autowired
    private ReportingCatalogService catalog;
    @Autowired
    private ReportingAccess access;
    @Value("${reporting.export.enabled:true}")
    private boolean enabled;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "reporting-export");
        thread.setDaemon(true);
        return thread;
    });
    private Future<?> running;
    private ExportJobView active;

    @Scheduled(fixedDelayString = "${reporting.export.poll-ms:2000}", initialDelayString = "${reporting.export.initial-delay-ms:10000}")
    public synchronized void tick() {
        if (!enabled)
            return;
        // Keep the scheduler free to renew leases while CSV generation is busy.
        // Each application still has exactly one generation thread.
        if (running != null && !running.isDone())
            jobs.renewLease(active.id(), worker);
        jobs.recover();
        jobs.cleanupOutput();
        if (running != null && !running.isDone())
            return;
        var job = jobs.claim(worker);
        if (job == null)
            return;
        active = job;
        running = executor.submit(() -> generate(job));
    }

    private void generate(ExportJobView job) {
        String owner = null;
        Path partial = null;
        try {
            owner = jobs.ownerForWorker(job.id(), worker);
            access.requireScope(owner, job.request().filterSpec().labSectionIds());
            catalog.validateCurrent(job.request());
            partial = jobs.stage(owner, job.id(), worker);
            long rows;
            try (var output = Files.newBufferedWriter(partial, StandardCharsets.UTF_8)) {
                rows = catalog.source(job.request().definition().source()).write(output, job.request());
            }
            jobs.publish(owner, job.id(), worker, rows);
        } catch (Exception error) {
            String code = error instanceof ReportingException ? error.getMessage() : "reporting.job.generationFailed";
            if (owner != null)
                jobs.failed(owner, job.id(), worker, code);
        } finally {
            try {
                if (partial != null)
                    Files.deleteIfExists(partial);
            } catch (java.io.IOException cleanupError) {
                org.openelisglobal.common.log.LogEvent.logWarn(getClass().getSimpleName(), "tick",
                        "Reporting output cleanup is pending for job " + job.id());
            }
        }
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }
}
