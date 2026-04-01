package org.openelisglobal.odoo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.odoo.exception.OdooUnavailableException;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.openelisglobal.sample.event.listener.SamplePatientUpdateDataCreatedEventListener;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OdooEventListenerTest {

    @Mock
    private OdooIntegrationService odooIntegrationService;

    @Mock
    private OdooSyncQueueService odooSyncQueueService;

    @InjectMocks
    private SamplePatientUpdateDataCreatedEventListener listener;

    private static final String ACCESSION_NUMBER = "LARC12345";

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(listener, "odooEnabled", true);
        ReflectionTestUtils.setField(listener, "queueEnabled", true);
        when(odooSyncQueueService.enqueue(anyString())).thenReturn(new OdooSyncQueue());
    }

    // ─── odooEnabled = false ───────────────────────────────────────────────────

    @Test
    public void handleEvent_whenOdooDisabled_doesNothing() {
        ReflectionTestUtils.setField(listener, "odooEnabled", false);

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        verify(odooIntegrationService, never()).createInvoice(any());
        verify(odooSyncQueueService, never()).enqueue(anyString());
    }

    // ─── happy path ───────────────────────────────────────────────────────────

    @Test
    public void handleEvent_whenInvoiceSucceeds_doesNotEnqueue() {
        doNothing().when(odooIntegrationService).createInvoice(any());

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        verify(odooIntegrationService, times(1)).createInvoice(any());
        verify(odooSyncQueueService, never()).enqueue(anyString());
    }

    // ─── OdooUnavailableException → enqueue ───────────────────────────────────

    @Test
    public void handleEvent_whenOdooUnavailable_enqueuesToRetryQueue() {
        doThrow(new OdooUnavailableException("Odoo is down")).when(odooIntegrationService).createInvoice(any());

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        verify(odooSyncQueueService, times(1)).enqueue(ACCESSION_NUMBER);
    }

    @Test
    public void handleEvent_whenOdooUnavailable_enqueuedWithCorrectAccessionNumber() {
        doThrow(new OdooUnavailableException("connection refused")).when(odooIntegrationService).createInvoice(any());

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent("LARC99999"));

        verify(odooSyncQueueService, times(1)).enqueue("LARC99999");
    }

    // ─── generic exception → enqueue (queueEnabled=true) ─────────────────────

    @Test
    public void handleEvent_whenGenericExceptionAndQueueEnabled_enqueuesToRetryQueue() {
        doThrow(new RuntimeException("unexpected failure")).when(odooIntegrationService).createInvoice(any());

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        verify(odooSyncQueueService, times(1)).enqueue(ACCESSION_NUMBER);
    }

    // ─── generic exception → no enqueue (queueEnabled=false) ─────────────────

    @Test
    public void handleEvent_whenGenericExceptionAndQueueDisabled_doesNotEnqueue() {
        ReflectionTestUtils.setField(listener, "queueEnabled", false);
        doThrow(new RuntimeException("unexpected failure")).when(odooIntegrationService).createInvoice(any());

        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        verify(odooSyncQueueService, never()).enqueue(anyString());
    }

    // ─── OdooUnavailableException + queueEnabled=false → no enqueue ──────────

    @Test
    public void handleEvent_whenOdooUnavailableAndQueueDisabled_doesNotEnqueue() {
        ReflectionTestUtils.setField(listener, "queueEnabled", false);
        doThrow(new OdooUnavailableException("Odoo down")).when(odooIntegrationService).createInvoice(any());

        // OdooUnavailableException always enqueues regardless of queueEnabled
        // because it is caught in its own catch block before the queueEnabled check
        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));

        // OdooUnavailableException path calls enqueueForRetry directly —
        // but enqueueForRetry checks queueEnabled, so no enqueue
        verify(odooSyncQueueService, never()).enqueue(anyString());
    }

    // ─── enqueue failure is swallowed ─────────────────────────────────────────

    @Test
    public void handleEvent_whenEnqueueThrows_doesNotPropagateException() {
        doThrow(new OdooUnavailableException("Odoo down")).when(odooIntegrationService).createInvoice(any());
        doThrow(new RuntimeException("DB error")).when(odooSyncQueueService).enqueue(anyString());

        // Should not throw
        listener.handleSamplePatientUpdateDataCreatedEvent(buildEvent(ACCESSION_NUMBER));
    }

    // ─── null updateData edge case ────────────────────────────────────────────

    @Test
    public void handleEvent_whenUpdateDataIsNull_handlesGracefully() {

        SamplePatientUpdateDataCreatedEvent event = new SamplePatientUpdateDataCreatedEvent(this, null, null, null);

        // Should not throw; listener guards null updateData and returns early without
        // enqueueing
        listener.handleSamplePatientUpdateDataCreatedEvent(event);

        verify(odooIntegrationService, never()).createInvoice(any());
        verify(odooSyncQueueService, never()).enqueue(anyString());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private SamplePatientUpdateDataCreatedEvent buildEvent(String accessionNumber) {
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        when(updateData.getAccessionNumber()).thenReturn(accessionNumber);
        return new SamplePatientUpdateDataCreatedEvent(this, updateData, null, null);
    }
}
