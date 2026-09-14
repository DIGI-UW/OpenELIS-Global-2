package org.openelisglobal.reports.dataexport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import org.openelisglobal.reports.dataexport.dao.ExportJobDAO;
import org.openelisglobal.reports.dataexport.form.ExportJobView;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.openelisglobal.reports.dataexport.valueholder.ExportJobState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReportingJobService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExportJobDAO jobs;
    private final ReportingCatalogService catalog;
    private final ReportingAccess access;
    private final ReportingSettings settings;
    private java.time.Clock clock = java.time.Clock.systemUTC();
    @Autowired
    private ReportingFiles files;

    private Instant now() {
        return clock.instant();
    }

    @Autowired
    public ReportingJobService(ExportJobDAO jobs, ReportingCatalogService catalog, ReportingAccess access,
            ReportingSettings settings) {
        this.jobs = jobs;
        this.catalog = catalog;
        this.access = access;
        this.settings = settings;
    }

    public ExportJobView submit(String owner, ExportSubmission request) {
        access.requireReports(owner);
        if (request == null || request.clientRequestId() == null
                || !request.clientRequestId().matches("[A-Za-z0-9_-]{1,80}")) {
            throw new ReportingException(422, "reporting.request.invalid");
        }
        // Lock the existing owner row so parallel requests cannot both pass admission.
        jobs.lockOwner(owner);
        String requestHash = digest(json(request));
        ExportJob previous = jobs.submission(owner, request.clientRequestId());
        if (previous != null) {
            if (!previous.getRequestHash().equals(requestHash))
                throw new ReportingException(409, "reporting.request.conflict");
            return view(previous);
        }
        ExportSnapshot frozen = catalog.freeze(request, owner);
        if (jobs.activeCount(owner) >= settings.maxActive())
            throw new ReportingException(429, "reporting.jobs.limit");
        ExportJob job = new ExportJob(owner, request.clientRequestId(), frozen.definition().id(), frozen.layout(),
                json(frozen), requestHash, now(), null);
        jobs.persistJob(job);
        jobs.flushJobs();
        ReportingAudit.job(ReportingAudit.Action.SUBMITTED, owner, job);
        return view(job);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String owner, int page, int size) {
        access.requireReports(owner);
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.multiplyExact(Math.max(0, page), limit);
        var found = jobs.page(owner, offset, limit + 1);
        return Map.of("jobs", found.stream().limit(limit).map(this::view).toList(), "hasMore", found.size() > limit,
                "page", Math.max(0, page), "activeCount", jobs.activeCount(owner));
    }

    @Transactional(readOnly = true)
    public ExportJobView detail(String owner, String id) {
        access.requireReports(owner);
        return view(owned(owner, id, false));
    }

    @Transactional(readOnly = true)
    public ExportJobView authorizeDownload(String owner, String id) {
        ExportJob job = owned(owner, id, false);
        return authorizeDownload(owner, job);
    }

    private ExportJobView authorizeDownload(String owner, ExportJob job) {
        access.requireScope(owner, snapshot(job).filterSpec().labSectionIds());
        if (job.getState() == ExportJobState.EXPIRED
                || job.getExpiresAt() != null && !job.getExpiresAt().isAfter(now())) {
            throw new ReportingException(410, "reporting.job.expired");
        }
        if (job.getState() != ExportJobState.READY)
            throw new ReportingException(409, "reporting.job.notReady");
        return view(job);
    }

    public record Download(ExportJobView job, java.io.InputStream input) {
    }

    public Download download(String owner, String id) throws java.io.IOException {
        // Open while holding the same row lock as expiry/cleanup. An already open
        // descriptor can finish after unlink; subsequent requests fail at expiry.
        var stored = owned(owner, id, true);
        var job = authorizeDownload(owner, stored);
        var input = files.open(id);
        ReportingAudit.job(ReportingAudit.Action.DOWNLOAD_OPENED, owner, stored);
        return new Download(job, input);
    }

    public ExportJobView cancel(String owner, String id) {
        ExportJob job = owned(owner, id, true);
        access.requireReports(owner);
        if (job.getState() == ExportJobState.CANCELLED)
            return view(job);
        if (job.getState() != ExportJobState.QUEUED)
            throw new ReportingException(409, "reporting.job.cannotCancel");
        job.transitionTo(ExportJobState.CANCELLED);
        job.setCompletedAt(now());
        ReportingAudit.job(ReportingAudit.Action.CANCELLED, owner, job);
        return view(job);
    }

    public ExportJobView retry(String owner, String id, String clientRequestId) {
        access.requireReports(owner);
        if (clientRequestId == null || !clientRequestId.matches("[A-Za-z0-9_-]{1,80}"))
            throw new ReportingException(422, "reporting.request.invalid");
        jobs.lockOwner(owner);
        ExportJob parent = owned(owner, id, true);
        if (parent.getState() != ExportJobState.FAILED)
            throw new ReportingException(409, "reporting.job.cannotRetry");
        ExportJob previous = jobs.submission(owner, clientRequestId);
        if (previous != null) {
            if (!id.equals(previous.getParentId()))
                throw new ReportingException(409, "reporting.request.conflict");
            return view(previous);
        }
        var frozen = snapshot(parent);
        access.requireScope(owner, frozen.filterSpec().labSectionIds());
        catalog.validateCurrent(frozen);
        if (jobs.activeCount(owner) >= settings.maxActive())
            throw new ReportingException(429, "reporting.jobs.limit");
        ExportJob child = new ExportJob(owner, clientRequestId, parent.getSourceId(), parent.getLayout(),
                parent.getRequestJson(), digest(json(Map.of("retryOf", id))), now(), id);
        jobs.persistJob(child);
        jobs.flushJobs();
        ReportingAudit.job(ReportingAudit.Action.RETRIED, owner, child);
        return view(child);
    }

    public ExportJobView claim(String worker) {
        ExportJob job = jobs.nextQueued();
        if (job == null)
            return null;
        if (job.getState() != ExportJobState.QUEUED)
            return null;
        job.transitionTo(ExportJobState.GENERATING);
        job.setStartedAt(now());
        job.setWorkerId(worker);
        job.setLeaseUntil(now().plus(5, ChronoUnit.MINUTES));
        jobs.flushJobs();
        ReportingAudit.job(ReportingAudit.Action.STARTED, "worker:" + worker, job);
        return view(job);
    }

    public String ownerForWorker(String id, String worker) {
        var job = jobs.get(id).orElseThrow();
        if (!hasLease(job, worker)) {
            throw new ReportingException(409, "reporting.job.claimLost");
        }
        return job.getOwnerId();
    }

    public void ready(String owner, String id, String worker, long rows, long size) {
        ExportJob job = owned(owner, id, true);
        if (!hasLease(job, worker)) {
            throw new ReportingException(409, "reporting.job.claimLost");
        }
        job.transitionTo(ExportJobState.READY);
        job.setRowCount(rows);
        job.setFileSize(size);
        job.setCompletedAt(now());
        job.setExpiresAt(job.getCompletedAt().plus(settings.retentionDays(), ChronoUnit.DAYS));
        job.setLeaseUntil(null);
        ReportingAudit.job(ReportingAudit.Action.READY, "worker:" + worker, job);
    }

    private boolean hasLease(ExportJob job, String worker) {
        return job != null && job.getState() == ExportJobState.GENERATING && worker.equals(job.getWorkerId())
                && job.getLeaseUntil() != null && job.getLeaseUntil().isAfter(now());
    }

    public boolean renewLease(String id, String worker) {
        var job = jobs.locked(id);
        if (!hasLease(job, worker))
            return false;
        job.setLeaseUntil(now().plus(5, ChronoUnit.MINUTES));
        return true;
    }

    public java.nio.file.Path stage(String owner, String id, String worker) throws java.io.IOException {
        if (!hasLease(owned(owner, id, true), worker))
            throw new ReportingException(409, "reporting.job.claimLost");
        return files.stage(id, worker);
    }

    public void publish(String owner, String id, String worker, long rows) throws java.io.IOException {
        if (!hasLease(owned(owner, id, true), worker))
            throw new ReportingException(409, "reporting.job.claimLost");
        ready(owner, id, worker, rows, files.publish(id, worker));
    }

    public void recover() {
        for (var job : jobs.dueForRecovery(now())) {
            if (job.getState() == ExportJobState.GENERATING
                    && (job.getLeaseUntil() == null || !job.getLeaseUntil().isAfter(now()))) {
                job.transitionTo(ExportJobState.FAILED);
                job.setFailureCode("reporting.job.interrupted");
                job.setCompletedAt(now());
                job.setLeaseUntil(null);
                ReportingAudit.job(ReportingAudit.Action.INTERRUPTED, "system", job);
            } else if (job.getState() == ExportJobState.READY && job.getExpiresAt() != null
                    && !job.getExpiresAt().isAfter(now())) {
                job.transitionTo(ExportJobState.EXPIRED);
                ReportingAudit.job(ReportingAudit.Action.EXPIRED, "system", job);
            }
        }
    }

    public void cleanupOutput() {
        for (var job : jobs.pendingCleanup()) {
            try {
                files.removeOutput(job.getId(), job.getWorkerId());
                job.setOutputCleanedAt(now());
            } catch (java.io.IOException error) {
                org.openelisglobal.common.log.LogEvent.logWarn(getClass().getSimpleName(), "cleanupOutput",
                        "Reporting output cleanup will be retried for job " + job.getId());
            }
        }
    }

    public void failed(String owner, String id, String worker, String code) {
        ExportJob job = owned(owner, id, true);
        if (job.getState() == ExportJobState.GENERATING && worker.equals(job.getWorkerId())) {
            job.transitionTo(ExportJobState.FAILED);
            job.setFailureCode(code);
            job.setCompletedAt(now());
            job.setLeaseUntil(null);
            ReportingAudit.job(ReportingAudit.Action.FAILED, "worker:" + worker, job);
        }
    }

    private ExportJob owned(String owner, String id, boolean lock) {
        ExportJob job = jobs.owned(owner, id, lock);
        if (job == null) {
            ReportingAudit.denied(owner, id);
            throw new ReportingException(404, "reporting.job.notFound");
        }
        return job;
    }

    private ExportSnapshot snapshot(ExportJob job) {
        try {
            return mapper.readValue(job.getRequestJson(), ExportSnapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("reporting.request.invalid", e);
        }
    }

    private ExportJobView view(ExportJob job) {
        String state = job.getState().name();
        if (job.getState() == ExportJobState.READY && job.getExpiresAt() != null && !job.getExpiresAt().isAfter(now()))
            state = "EXPIRED";
        return new ExportJobView(job.getId(), state, text(job.getSubmittedAt()), text(job.getStartedAt()),
                text(job.getCompletedAt()), text(job.getExpiresAt()), job.getRowCount(), job.getFileSize(),
                job.getFailureCode(), job.getParentId(), snapshot(job));
    }

    private static String text(Instant time) {
        return time == null ? null : time.toString();
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("reporting.request.invalid", e);
        }
    }

    private static String digest(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
