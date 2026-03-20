package org.openelisglobal.odoo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.odoo.service.OdooRetryJob;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OdooRetryJobTest {

    @Mock
    private OdooSyncQueueService odooSyncQueueService;

    @Mock
    private OdooIntegrationService odooIntegrationService;

    @Mock
    private SampleService sampleService;

    @InjectMocks
    private OdooRetryJob odooRetryJob;

    private static final String ACCESSION_NUMBER = "LARC12345";

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(odooRetryJob, "odooEnabled", true);
        ReflectionTestUtils.setField(odooRetryJob, "retryEnabled", true);
    }

    // ─── processRetryQueue — disabled ─────────────────────────────────────────

    @Test
    public void processRetryQueue_whenOdooDisabled_doesNothing() {
        ReflectionTestUtils.setField(odooRetryJob, "odooEnabled", false);

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, never()).getItemsReadyForRetry();
        verify(odooIntegrationService, never()).createInvoiceForSample(any());
    }

    @Test
    public void processRetryQueue_whenRetryDisabled_doesNothing() {
        ReflectionTestUtils.setField(odooRetryJob, "retryEnabled", false);

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, never()).getItemsReadyForRetry();
        verify(odooIntegrationService, never()).createInvoiceForSample(any());
    }

    // ─── processRetryQueue — empty queue ──────────────────────────────────────

    @Test
    public void processRetryQueue_whenQueueEmpty_doesNotCallCreateInvoice() {
        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            List.of()
        );

        odooRetryJob.processRetryQueue();

        verify(odooIntegrationService, never()).createInvoice(any());
    }

    // ─── processRetryQueue — sample not found ─────────────────────────────────

    @Test
    public void processRetryQueue_whenSampleNotFound_marksItemFailed() {
        OdooSyncQueue item = buildItem(ACCESSION_NUMBER, 0);
        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            List.of(item)
        );
        when(
            sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)
        ).thenReturn(null);

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, times(1)).markInProgress(item);
        verify(odooSyncQueueService, times(1)).markFailed(
            eq(item),
            contains(ACCESSION_NUMBER)
        );
        verify(odooIntegrationService, never()).createInvoice(any());
    }

    // ─── processRetryQueue — successful retry ─────────────────────────────────

    @Test
    public void processRetryQueue_whenSampleFoundAndInvoiceSucceeds_marksItemCompleted() {
        OdooSyncQueue item = buildItem(ACCESSION_NUMBER, 1);
        Sample sample = new Sample();
        sample.setAccessionNumber(ACCESSION_NUMBER);

        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            List.of(item)
        );
        when(
            sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)
        ).thenReturn(sample);
        doNothing()
            .when(odooIntegrationService)
            .createInvoiceForSample(any(Sample.class));

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, times(1)).markInProgress(item);
        verify(odooIntegrationService, times(1)).createInvoiceForSample(
            any(Sample.class)
        );
        verify(odooSyncQueueService, times(1)).markCompleted(item);
        verify(odooSyncQueueService, never()).markFailed(any(), anyString());
    }

    // ─── processRetryQueue — invoice creation fails ───────────────────────────

    @Test
    public void processRetryQueue_whenInvoiceCreationThrows_marksItemFailed() {
        OdooSyncQueue item = buildItem(ACCESSION_NUMBER, 1);
        Sample sample = new Sample();
        sample.setAccessionNumber(ACCESSION_NUMBER);

        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            List.of(item)
        );
        when(
            sampleService.getSampleByAccessionNumber(ACCESSION_NUMBER)
        ).thenReturn(sample);
        doThrow(new RuntimeException("Odoo connection refused"))
            .when(odooIntegrationService)
            .createInvoiceForSample(any(Sample.class));

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, times(1)).markInProgress(item);
        verify(odooSyncQueueService, times(1)).markFailed(
            eq(item),
            contains("Odoo connection refused")
        );
        verify(odooSyncQueueService, never()).markCompleted(any());
    }

    // ─── processRetryQueue — multiple items ───────────────────────────────────

    @Test
    public void processRetryQueue_withMultipleItems_processesAll() {
        OdooSyncQueue item1 = buildItem("LARC11111", 0);
        OdooSyncQueue item2 = buildItem("LARC22222", 1);
        OdooSyncQueue item3 = buildItem("LARC33333", 2);

        Sample sample1 = buildSample("LARC11111");
        Sample sample2 = buildSample("LARC22222");

        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            Arrays.asList(item1, item2, item3)
        );
        when(sampleService.getSampleByAccessionNumber("LARC11111")).thenReturn(
            sample1
        );
        when(sampleService.getSampleByAccessionNumber("LARC22222")).thenReturn(
            sample2
        );
        when(sampleService.getSampleByAccessionNumber("LARC33333")).thenReturn(
            null
        );
        doNothing()
            .when(odooIntegrationService)
            .createInvoiceForSample(any(Sample.class));

        odooRetryJob.processRetryQueue();

        // All three should be marked in-progress
        verify(odooSyncQueueService, times(3)).markInProgress(any());
        // item1 and item2 succeed
        verify(odooSyncQueueService, times(2)).markCompleted(any());
        // item3 fails because sample not found
        verify(odooSyncQueueService, times(1)).markFailed(
            eq(item3),
            anyString()
        );
    }

    @Test
    public void processRetryQueue_whenOneItemFails_continuesProcessingOthers() {
        OdooSyncQueue item1 = buildItem("LARC11111", 0);
        OdooSyncQueue item2 = buildItem("LARC22222", 0);

        Sample sample1 = buildSample("LARC11111");
        Sample sample2 = buildSample("LARC22222");

        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            Arrays.asList(item1, item2)
        );
        when(sampleService.getSampleByAccessionNumber("LARC11111")).thenReturn(
            sample1
        );
        when(sampleService.getSampleByAccessionNumber("LARC22222")).thenReturn(
            sample2
        );

        // item1 invoice creation fails, item2 succeeds
        doThrow(new RuntimeException("Odoo down"))
            .when(odooIntegrationService)
            .createInvoiceForSample(same(sample1));
        doNothing()
            .when(odooIntegrationService)
            .createInvoiceForSample(same(sample2));

        odooRetryJob.processRetryQueue();

        verify(odooSyncQueueService, times(1)).markFailed(
            eq(item1),
            anyString()
        );
        verify(odooSyncQueueService, times(1)).markCompleted(item2);
    }

    // ─── processRetryQueue — markInProgress exception ─────────────────────────

    @Test
    public void processRetryQueue_whenMarkInProgressThrows_continuesWithNextItem() {
        OdooSyncQueue item1 = buildItem("LARC11111", 0);
        OdooSyncQueue item2 = buildItem("LARC22222", 0);
        Sample sample2 = buildSample("LARC22222");

        when(odooSyncQueueService.getItemsReadyForRetry()).thenReturn(
            Arrays.asList(item1, item2)
        );
        doThrow(new RuntimeException("DB error"))
            .when(odooSyncQueueService)
            .markInProgress(item1);
        doNothing().when(odooSyncQueueService).markInProgress(item2);
        when(sampleService.getSampleByAccessionNumber("LARC22222")).thenReturn(
            sample2
        );
        doNothing()
            .when(odooIntegrationService)
            .createInvoiceForSample(any(Sample.class));

        odooRetryJob.processRetryQueue();

        // item2 should still be processed
        verify(odooSyncQueueService, times(1)).markCompleted(item2);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private OdooSyncQueue buildItem(String accessionNumber, int retryCount) {
        OdooSyncQueue item = new OdooSyncQueue();
        item.setAccessionNumber(accessionNumber);
        item.setStatus(SyncStatus.PENDING);
        item.setRetryCount(retryCount);
        item.setMaxRetries(3);
        item.setCreatedAt(Timestamp.from(Instant.now()));
        item.setNextRetryTime(Timestamp.from(Instant.now().minusSeconds(60)));
        return item;
    }

    private Sample buildSample(String accessionNumber) {
        Sample sample = new Sample();
        sample.setAccessionNumber(accessionNumber);
        return sample;
    }
}
