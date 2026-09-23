package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.openelisglobal.reportdefinition.valueholder.ReportDefinition;
import org.openelisglobal.reports.dataexport.valueholder.ExportJob;
import org.openelisglobal.reports.dataexport.valueholder.ExportJobState;

public class ReportingPersistenceTest {
    @Test
    public void allReportingEntitiesBuildWithoutADatabase() {
        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        configuration.addAnnotatedClass(ExportJob.class);
        configuration.addAnnotatedClass(ReportDefinition.class);
        try (SessionFactory factory = configuration.buildSessionFactory()) {
            assertNotNull(factory.getMetamodel().entity(ExportJob.class).getVersion(java.sql.Timestamp.class));
            assertNotNull(factory.getMetamodel().entity(ReportDefinition.class).getVersion(java.sql.Timestamp.class));
        }
    }

    @Test
    public void completedRequestStaysImmutableWhileItsFileExpires() {
        ExportJob job = new ExportJob("42", "request-1", "SAMPLE_TESTING", "SPREADSHEET", "{\"fields\":[\"hb\"]}",
                "digest-1", Instant.parse("2026-09-13T12:00:00Z"), null);
        job.transitionTo(ExportJobState.GENERATING);
        job.transitionTo(ExportJobState.READY);
        job.transitionTo(ExportJobState.EXPIRED);
        assertEquals("{\"fields\":[\"hb\"]}", job.getRequestJson());
        assertEquals("42", job.getOwnerId());
        assertEquals("request-1", job.getClientRequestId());
        assertThrows(IllegalStateException.class, () -> job.transitionTo(ExportJobState.GENERATING));
    }

    @Test
    public void onlyQueuedJobsCanBeCancelledAndFailureCannotBecomeReady() {
        ExportJob queued = new ExportJob("42", "request-1", "SAMPLE_TESTING", "SPREADSHEET", "{}", "hash",
                Instant.now(), null);
        queued.transitionTo(ExportJobState.CANCELLED);
        assertThrows(IllegalStateException.class, () -> queued.transitionTo(ExportJobState.GENERATING));
        ExportJob running = new ExportJob("42", "request-2", "SAMPLE_TESTING", "RESULT_LIST", "{}", "hash",
                Instant.now(), "parent-job");
        running.transitionTo(ExportJobState.GENERATING);
        assertThrows(IllegalStateException.class, () -> running.transitionTo(ExportJobState.CANCELLED));
        running.transitionTo(ExportJobState.FAILED);
        assertThrows(IllegalStateException.class, () -> running.transitionTo(ExportJobState.READY));
        assertEquals("parent-job", running.getParentId());
    }
}
