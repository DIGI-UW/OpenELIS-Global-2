package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.dao.ExportJobDAO;
import org.openelisglobal.reports.dataexport.form.ExportFilter;
import org.openelisglobal.reports.dataexport.form.ExportSnapshot;
import org.openelisglobal.reports.dataexport.form.ExportSubmission;
import org.openelisglobal.reports.dataexport.form.ReportSourceConfig;
import org.openelisglobal.reports.dataexport.form.ReportingVariable;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.reports.dataexport.service.ReportingJobService;
import org.openelisglobal.reports.dataexport.service.ReportingSettings;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.openelisglobal.reports.dataexport.valueholder.ExportJobState;

public class ReportingJobServiceTest {
    private ExportJobDAO jobs;
    private ReportingCatalogService catalog;
    private ReportingAccess access;
    private ReportingSettings settings;
    private ReportingJobService service;
    private ExportSnapshot snapshot;
    private ExportSubmission submission;

    @Before
    public void setUp() {
        jobs = mock(ExportJobDAO.class);
        catalog = mock(ReportingCatalogService.class);
        access = mock(ReportingAccess.class);
        settings = mock(ReportingSettings.class);
        service = new ReportingJobService(jobs, catalog, access, settings);
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), List.of("accessionNumber"), List.of(),
                List.of("labSectionIds"), Map.of("SPREADSHEET", List.of("accessionNumber")));
        ExportFilter filter = new ExportFilter("2026-08-01", "2026-08-31", List.of("7"), List.of(),
                List.of("FINALIZED"));
        snapshot = new ExportSnapshot(definition, "SPREADSHEET", List.of(new ReportingVariable("accessionNumber",
                "Accession Number", "text", "sample", false, List.of("SPREADSHEET"))), filter, "UTC", List.of("30"));
        submission = new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", "request-1", List.of("accessionNumber"),
                filter);
        when(catalog.freeze(submission, "42")).thenReturn(snapshot);
        when(settings.maxActive()).thenReturn(5);
    }

    @Test
    public void acceptedSubmissionIsOwnedFrozenAndPersistedForAnOrdinaryReportUser() {
        AtomicReference<ExportJob> persisted = capturePersisted();

        var accepted = service.submit("42", submission);

        assertEquals("job-1", accepted.id());
        assertEquals(snapshot, accepted.request());
        assertEquals("42", persisted.get().getOwnerId());
        verify(access).requireReports("42");
        verify(jobs).lockOwner("42");
        verify(jobs).flushJobs();
    }

    @Test
    public void sameRequestIdentityReturnsTheOriginalJobAndChangedContentConflicts() {
        AtomicReference<ExportJob> persisted = capturePersisted();
        var original = service.submit("42", submission);
        when(jobs.submission("42", "request-1")).thenAnswer(call -> persisted.get());

        var repeated = service.submit("42", submission);
        assertEquals(original.request(), repeated.request());
        assertEquals(original.id(), repeated.id());

        ExportSubmission changed = new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", "request-1",
                List.of("accessionNumber"),
                new ExportFilter("2026-08-02", "2026-08-31", List.of("7"), List.of(), List.of("FINALIZED")));
        ReportingException conflict = assertThrows(ReportingException.class, () -> service.submit("42", changed));
        assertEquals(409, conflict.status());
        verify(jobs).persistJob(persisted.get());
    }

    @Test
    public void activeLimitRejectsBeforePersistenceWithoutChangingTheDraftRequest() {
        when(jobs.activeCount("42")).thenReturn(5L);

        ReportingException limit = assertThrows(ReportingException.class, () -> service.submit("42", submission));

        assertEquals(429, limit.status());
        assertEquals("reporting.jobs.limit", limit.getMessage());
        verify(jobs, never()).persistJob(any());
    }

    @Test
    public void downloadIsOwnerScopedAndRechecksCurrentLabScope() throws Exception {
        ExportJob ready = storedReadyJob();
        when(jobs.owned("42", "job-1", false)).thenReturn(ready);

        assertEquals("job-1", service.authorizeDownload("42", "job-1").id());
        verify(access).requireScope("42", List.of("7"));

        ReportingException hidden = assertThrows(ReportingException.class,
                () -> service.authorizeDownload("99", "job-1"));
        assertEquals(404, hidden.status());
    }

    @Test
    public void revokedCurrentScopePreventsAReadyFileDownload() throws Exception {
        ExportJob ready = storedReadyJob();
        when(jobs.owned("42", "job-1", false)).thenReturn(ready);
        org.mockito.Mockito.doThrow(new ReportingException(403, "reporting.access.scopeChanged")).when(access)
                .requireScope("42", List.of("7"));

        assertEquals(403,
                assertThrows(ReportingException.class, () -> service.authorizeDownload("42", "job-1")).status());
    }

    private AtomicReference<ExportJob> capturePersisted() {
        AtomicReference<ExportJob> persisted = new AtomicReference<>();
        doAnswer(call -> {
            ExportJob job = call.getArgument(0);
            job.setId("job-1");
            persisted.set(job);
            return null;
        }).when(jobs).persistJob(any());
        return persisted;
    }

    private ExportJob storedReadyJob() throws Exception {
        String json = new ObjectMapper().writeValueAsString(snapshot);
        ExportJob job = new ExportJob("42", "request-1", "SAMPLE_TESTING", "SPREADSHEET", json, "digest", Instant.now(),
                null);
        job.setId("job-1");
        job.transitionTo(ExportJobState.GENERATING);
        job.transitionTo(ExportJobState.READY);
        job.setExpiresAt(Instant.now().plusSeconds(600));
        job.setFileSize(12L);
        return job;
    }
}
