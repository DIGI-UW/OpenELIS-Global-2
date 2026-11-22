package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.reportdefinition.service.ReportDefinitionService;
import org.openelisglobal.reports.dataexport.form.SavedReportDefinition;
import org.openelisglobal.reports.dataexport.form.SavedReportFilters;
import org.openelisglobal.reports.dataexport.form.SavedReportMutation;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingCatalogService;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.reports.dataexport.service.ReportingSavedConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class ReportingSavedConfigConcurrencyTest extends BaseWebContextSensitiveTest {
    @Autowired
    private ReportDefinitionService definitions;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private record Attempt(int status, String name, String detail) {
    }

    @Test
    public void simultaneousEditsPreserveOneWinnerAndReturnAnActionableConflict() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ReportingCatalogService catalog = mock(ReportingCatalogService.class);
        ReportingSavedConfigService service = new ReportingSavedConfigService(definitions, catalog,
                mock(ReportingAccess.class));
        SavedReportDefinition definition = new SavedReportDefinition(1, "SAMPLE_TESTING", "SPREADSHEET",
                List.of("accessionNumber"), new SavedReportFilters(List.of(), List.of(), List.of("FINALIZED")));
        when(catalog.validateSaved(anyString(), any())).thenReturn(definition);
        var original = transaction
                .execute(status -> service.create("1", new SavedReportMutation("Concurrent report", null, definition)));
        CountDownLatch bothReadOriginal = new CountDownLatch(2);
        // Validation follows the service's version comparison. Hold both real
        // transactions here so they have accepted the same original version.
        when(catalog.validateSaved(anyString(), any())).thenAnswer(call -> {
            bothReadOriginal.countDown();
            assertTrue(bothReadOriginal.await(10, TimeUnit.SECONDS));
            return definition;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Attempt>> attempts = new ArrayList<>();
        try {
            for (String name : List.of("First editor", "Second editor")) {
                attempts.add(executor.submit(() -> {
                    try {
                        transaction.execute(status -> service.update("1", original.id(),
                                new SavedReportMutation(name, original.version(), definition)));
                        return new Attempt(200, name, "saved");
                    } catch (ReportingException error) {
                        return new Attempt(error.status(), name, error.getMessage());
                    } catch (RuntimeException error) {
                        return new Attempt(500, name, error.getClass().getName());
                    }
                }));
            }
            List<Attempt> completed = new ArrayList<>();
            for (Future<Attempt> attempt : attempts)
                completed.add(attempt.get(20, TimeUnit.SECONDS));
            assertEquals(completed.toString(), List.of(200, 409),
                    completed.stream().map(Attempt::status).sorted().toList());
            Attempt winner = completed.stream().filter(attempt -> attempt.status() == 200).findFirst().orElseThrow();
            Attempt conflict = completed.stream().filter(attempt -> attempt.status() == 409).findFirst().orElseThrow();
            assertEquals("reporting.saved.changed", conflict.detail());
            var stored = transaction.execute(status -> service.detail("1", original.id()));
            assertEquals(winner.name(), stored.name());
            assertEquals(definition, stored.definition());
            assertNotEquals(original.version(), stored.version());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
            transaction.executeWithoutResult(status -> {
                var stored = definitions.get(original.id());
                stored.setSysUserId("1");
                definitions.delete(stored);
            });
        }
    }
}
