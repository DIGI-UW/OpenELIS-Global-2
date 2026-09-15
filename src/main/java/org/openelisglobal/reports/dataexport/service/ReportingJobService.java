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
                json(frozen), requestHash, Instant.now(), null);
        jobs.persistJob(job);
        jobs.flushJobs();
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
        access.requireScope(owner, snapshot(job).filterSpec().labSectionIds());
        if (job.getState() == ExportJobState.EXPIRED
                || job.getExpiresAt() != null && !job.getExpiresAt().isAfter(Instant.now())) {
            throw new ReportingException(410, "reporting.job.expired");
        }
        if (job.getState() != ExportJobState.READY)
            throw new ReportingException(409, "reporting.job.notReady");
        return view(job);
    }

    public ExportJobView claim(String worker) {
        ExportJob job = jobs.nextQueued();
        if (job == null)
            return null;
        if (job.getState() != ExportJobState.QUEUED)
            return null;
        job.transitionTo(ExportJobState.GENERATING);
        job.setStartedAt(Instant.now());
        job.setWorkerId(worker);
        job.setLeaseUntil(Instant.now().plus(5, ChronoUnit.MINUTES));
        jobs.flushJobs();
        return view(job);
    }

    public String ownerForWorker(String id, String worker) {
        var job = jobs.get(id).orElseThrow();
        if (job.getState() != ExportJobState.GENERATING || !worker.equals(job.getWorkerId())) {
            throw new ReportingException(409, "reporting.job.claimLost");
        }
        return job.getOwnerId();
    }

    public void ready(String owner, String id, String worker, long rows, long size) {
        ExportJob job = owned(owner, id, true);
        if (job.getState() != ExportJobState.GENERATING || !worker.equals(job.getWorkerId())) {
            throw new ReportingException(409, "reporting.job.claimLost");
        }
        job.transitionTo(ExportJobState.READY);
        job.setRowCount(rows);
        job.setFileSize(size);
        job.setCompletedAt(Instant.now());
        job.setExpiresAt(job.getCompletedAt().plus(settings.retentionDays(), ChronoUnit.DAYS));
        job.setLeaseUntil(null);
    }

    public void failed(String owner, String id, String worker, String code) {
        ExportJob job = owned(owner, id, true);
        if (job.getState() == ExportJobState.GENERATING && worker.equals(job.getWorkerId())) {
            job.transitionTo(ExportJobState.FAILED);
            job.setFailureCode(code);
            job.setCompletedAt(Instant.now());
            job.setLeaseUntil(null);
        }
    }

    private ExportJob owned(String owner, String id, boolean lock) {
        ExportJob job = jobs.owned(owner, id, lock);
        if (job == null)
            throw new ReportingException(404, "reporting.job.notFound");
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
        if (job.getState() == ExportJobState.READY && job.getExpiresAt() != null
                && !job.getExpiresAt().isAfter(Instant.now()))
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
