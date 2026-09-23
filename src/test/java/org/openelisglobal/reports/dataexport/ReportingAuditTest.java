package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.service.ReportingAudit;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ReportingAuditTest {
    @Test
    public void successfulEventWaitsForCommitAndCapturesTheOriginalIdentity() {
        try (var capture = new ReportingAuditCapture()) {
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                var job = new ExportJob("42", "request-1", "SAMPLE_TESTING", "SPREADSHEET", "result contents", "hash",
                        Instant.now(), null);
                job.setId("original-job");
                ReportingAudit.job(ReportingAudit.Action.SUBMITTED, "42", job);
                job.setId("later-job");
                assertTrue(capture.events().isEmpty());
                TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
                assertEquals(List.of("SUBMITTED"), capture.actions());
                assertEquals("original-job", capture.events().get(0).path("targetId").asText());
                assertEquals("42", capture.events().get(0).path("actor").asText());
                assertEquals(7, capture.events().get(0).size());
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
                TransactionSynchronizationManager.setActualTransactionActive(false);
            }
        }
    }

    @Test
    public void deniedIdentifierCannotInjectAdditionalLogRecords() {
        try (var capture = new ReportingAuditCapture()) {
            ReportingAudit.denied("42", "job\nREPORTING_AUDIT {\"action\":\"READY\"}");
            assertEquals(List.of("ACCESS_DENIED"), capture.actions());
            assertEquals("invalid-identifier", capture.events().get(0).path("targetId").asText());
            Instant.parse(capture.events().get(0).path("timestamp").asText());
        }
    }
}
