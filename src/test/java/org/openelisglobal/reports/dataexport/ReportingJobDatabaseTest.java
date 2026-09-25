package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
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
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Transactional
public class ReportingJobDatabaseTest extends BaseWebContextSensitiveTest {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ExportJobDAO jobs;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private ExportJob job(String owner, String requestId, String json) {
        return new ExportJob(owner, requestId, "SAMPLE_TESTING", "SPREADSHEET", json, "digest",
                Instant.parse("2026-09-13T12:00:00Z"), null);
    }

    @Test
    public void immutableRequestSurvivesMergeWhileLifecycleChangesPersist() {
        ExportJob original = job("42", "immutable-1", "original-request");
        entityManager.persist(original);
        entityManager.flush();
        assertNotNull(original.getId());
        String id = original.getId();
        ExportJob changed = job("99", "changed-submission", "changed-request");
        changed.setId(id);
        changed.setLastupdated(original.getLastupdated());
        changed.transitionTo(ExportJobState.GENERATING);
        entityManager.clear();
        entityManager.merge(changed);
        entityManager.flush();
        entityManager.clear();
        ExportJob stored = entityManager.find(ExportJob.class, id);
        assertEquals("42", stored.getOwnerId());
        assertEquals("immutable-1", stored.getClientRequestId());
        assertEquals("original-request", stored.getRequestJson());
        assertEquals(ExportJobState.GENERATING, stored.getState());
    }

    @Test
    public void submissionIdentityIsUniqueWithinAnOwner() {
        entityManager.persist(job("42", "duplicate-1", "original"));
        entityManager.flush();
        entityManager.persist(job("42", "duplicate-1", "second"));
        assertThrows(PersistenceException.class, entityManager::flush);
    }

    @Test
    public void differentOwnersCanUseTheSameClientRequestIdentifier() {
        ExportJob first = job("42", "shared-client-id", "first");
        ExportJob second = job("43", "shared-client-id", "second");
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();
        entityManager.clear();
        assertEquals("42", entityManager.find(ExportJob.class, first.getId()).getOwnerId());
        assertEquals("43", entityManager.find(ExportJob.class, second.getId()).getOwnerId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ownerLockSerializesConcurrentAdmissionAtTheConfiguredLimit() throws Exception {
        assertNotNull(entityManager.find(SystemUser.class, "1"));
        ReportingCatalogService catalog = mock(ReportingCatalogService.class);
        ReportingAccess access = mock(ReportingAccess.class);
        ReportingSettings settings = mock(ReportingSettings.class);
        ReportingJobService service = new ReportingJobService(jobs, catalog, access, settings);
        ReportSourceConfig definition = new ReportSourceConfig("SAMPLE_TESTING", 1, "Sample & Testing",
                "SAMPLE_TESTING", "collectionDate", List.of("SPREADSHEET"), List.of("accessionNumber"), List.of(),
                List.of(), Map.of("SPREADSHEET", List.of("accessionNumber")));
        ExportFilter filter = new ExportFilter("2026-08-01", "2026-08-01", List.of("1"), List.of(),
                List.of("FINALIZED"));
        ExportSnapshot snapshot = new ExportSnapshot(definition, "SPREADSHEET",
                List.of(new ReportingVariable("accessionNumber", "Accession Number", "text", "sample", false,
                        List.of("SPREADSHEET"))),
                filter, "UTC", List.of("1"));
        when(catalog.freeze(any(ExportSubmission.class), eq("1"))).thenReturn(snapshot);
        when(settings.maxActive()).thenReturn(1);
        String prefix = "concurrent-" + UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<Future<Integer>> attempts = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                String requestId = prefix + "-" + i;
                attempts.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    try {
                        return transaction.execute(status -> {
                            service.submit("1", new ExportSubmission(1, "SAMPLE_TESTING", "SPREADSHEET", requestId,
                                    List.of("accessionNumber"), filter));
                            return 202;
                        });
                    } catch (ReportingException e) {
                        return e.status();
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<Integer> outcomes = attempts.stream().map(attempt -> {
                try {
                    return attempt.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).sorted().toList();
            assertEquals(List.of(202, 429), outcomes);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            transaction.executeWithoutResult(status -> jobs.page("1", 0, 100).stream()
                    .filter(stored -> stored.getClientRequestId().startsWith(prefix)).forEach(jobs::delete));
        }
        assertEquals(Long.valueOf(0), transaction.execute(status -> jobs.activeCount("1")));
    }
}
