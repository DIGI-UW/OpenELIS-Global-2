package org.openelisglobal.reports.dataexport.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
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
    @Autowired
    private ReportingFiles files;
    @Value("${reporting.export.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${reporting.export.poll-ms:2000}", initialDelayString = "${reporting.export.initial-delay-ms:10000}")
    public void tick() {
        if (!enabled)
            return;
        var job = jobs.claim(worker);
        if (job == null)
            return;
        String owner = jobs.ownerForWorker(job.id(), worker);
        Path partial = null;
        try {
            access.requireScope(owner, job.request().filterSpec().labSectionIds());
            catalog.validateCurrent(job.request());
            partial = files.stage(job.id(), worker);
            long rows;
            try (var output = Files.newBufferedWriter(partial, StandardCharsets.UTF_8)) {
                rows = catalog.source(job.request().definition().source()).write(output, job.request());
            }
            Files.move(partial, files.path(job.id()), StandardCopyOption.ATOMIC_MOVE);
            jobs.ready(owner, job.id(), worker, rows, Files.size(files.path(job.id())));
        } catch (Exception error) {
            String code = error instanceof ReportingException ? error.getMessage() : "reporting.job.generationFailed";
            jobs.failed(owner, job.id(), worker, code);
            try {
                if (partial != null)
                    Files.deleteIfExists(partial);
                Files.deleteIfExists(files.path(job.id()));
            } catch (java.io.IOException cleanupError) {
                org.openelisglobal.common.log.LogEvent.logWarn(getClass().getSimpleName(), "tick",
                        "Reporting output cleanup is pending for job " + job.id());
            }
        }
    }
}
