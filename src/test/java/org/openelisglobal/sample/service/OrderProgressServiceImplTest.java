package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.qachecklist.service.SampleQaChecklistService;
import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceBlockedException;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceRecordService;

/**
 * OGC-1266 FR-F5, FR-F3, FR-F4, FR-A4: the order's progress status advances
 * with the steps' saves, the Sample check releases it under its enforcement
 * mode, and cancellation marks the order and its tests without deleting
 * anything.
 */
@RunWith(MockitoJUnitRunner.class)
public class OrderProgressServiceImplTest {

    @Mock
    private SampleService sampleService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private IStatusService statusService;
    @Mock
    private SampleAcceptanceChecklistService acceptanceChecklistService;
    @Mock
    private SampleAcceptanceRecordService acceptanceRecordService;
    @Mock
    private SampleQaChecklistService qaChecklistService;
    @Mock
    private ObservationHistoryService observationHistoryService;

    @InjectMocks
    private OrderProgressServiceImpl service;

    @Test
    public void theFirstSaveMarksTheOrderEntered() {
        Sample sample = order("41", null);

        service.recordStepSave(sample, null);

        assertEquals("ENTERED", sample.getOrderProgressStatus());
        assertNotNull(sample.getOrderEnteredAt());
        assertNull(sample.getOrderPreparedAt());
        verify(sampleService).update(sample);
    }

    @Test
    public void aSaveThatCompletesPrepareSamplesMarksTheOrderPrepared() {
        Sample sample = order("41", "ENTERED");

        service.recordStepSave(sample, "SAMPLES_PREPARED");

        assertEquals("SAMPLES_PREPARED", sample.getOrderProgressStatus());
        assertNotNull(sample.getOrderPreparedAt());
    }

    @Test
    public void aLaterSaveNeverMovesTheStatusBack() {
        Sample sample = order("41", "READY_FOR_TESTING");

        service.recordStepSave(sample, null);
        service.recordStepSave(sample, "SAMPLES_PREPARED");

        assertEquals("READY_FOR_TESTING", sample.getOrderProgressStatus());
        verify(sampleService, never()).update(any());
    }

    @Test
    public void aCancelledOrderRefusesASave() {
        Sample sample = order("41", "CANCELLED");

        assertThrows(IllegalArgumentException.class, () -> service.recordStepSave(sample, null));
    }

    @Test
    public void anOrderWithoutAStoredStatusIsReadFromItsLegacyFlags() {
        Sample sample = order("41", null);

        assertEquals(OrderProgressStatus.ENTERED, service.statusOf(sample, false, false));
        assertEquals(OrderProgressStatus.SAMPLES_PREPARED, service.statusOf(sample, true, false));
        assertEquals(OrderProgressStatus.READY_FOR_TESTING, service.statusOf(sample, true, true));
    }

    @Test
    public void completionDependsOnTheSampleCheckSetting() {
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OFF", "OPTIONAL");

        assertTrue(service.isComplete(OrderProgressStatus.SAMPLES_PREPARED, "clinical"));
        assertFalse(service.isComplete(OrderProgressStatus.SAMPLES_PREPARED, "clinical"));
        assertTrue(service.isComplete(OrderProgressStatus.READY_FOR_TESTING, "clinical"));
        assertFalse(service.isComplete(OrderProgressStatus.ENTERED, "clinical"));
    }

    @Test
    public void releaseUnderMandatoryAcceptanceIsHeldByTheGate() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(sampleService.get("41")).thenReturn(sample);
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("MANDATORY");
        Mockito.doThrow(new SampleAcceptanceBlockedException("blocked")).when(acceptanceRecordService)
                .enforceAcceptanceGateForOrder("41");

        assertThrows(SampleAcceptanceBlockedException.class, () -> service.release("41", null, "7"));
        assertEquals("SAMPLES_PREPARED", sample.getOrderProgressStatus());
    }

    @Test
    public void releaseUnderOptionalAcceptanceWithItemsUnansweredNeedsANote() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(sampleService.get("41")).thenReturn(sample);
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");
        when(qaChecklistService.areAllItemsVerified(41)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.release("41", " ", "7"));

        Sample released = service.release("41", "Tube visibly fine, reviewer away", "7");

        assertEquals("READY_FOR_TESTING", released.getOrderProgressStatus());
        assertEquals("Tube visibly fine, reviewer away", released.getOrderReleaseNote());
        assertNotNull(released.getOrderReadyAt());
    }

    @Test
    public void releaseBeforePrepareSamplesIsCompleteIsRefused() {
        Sample sample = order("41", "ENTERED");
        when(sampleService.get("41")).thenReturn(sample);

        assertThrows(IllegalStateException.class, () -> service.release("41", null, "7"));
    }

    @Test
    public void cancelMarksTheOrderAndItsOpenTestsWithoutDeleting() {
        Sample sample = order("41", "ENTERED");
        when(sampleService.get("41")).thenReturn(sample);
        when(statusService.getStatusID(AnalysisStatus.Canceled)).thenReturn("4");
        when(statusService.getStatusID(SampleStatus.Canceled)).thenReturn("12");
        Analysis open = new Analysis();
        open.setStatusId("1");
        Analysis finalized = new Analysis();
        finalized.setStatusId("9");
        when(statusService.matches("9", AnalysisStatus.Finalized)).thenReturn(true);
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(Arrays.asList(open, finalized));

        Sample cancelled = service.cancel("41", "Registered in the wrong domain", "7");

        assertEquals("CANCELLED", cancelled.getOrderProgressStatus());
        assertEquals("Registered in the wrong domain", cancelled.getOrderCancelReason());
        assertEquals("7", cancelled.getOrderCancelledBy());
        assertEquals("12", cancelled.getStatusId());
        assertEquals("4", open.getStatusId());
        assertEquals("9", finalized.getStatusId());
        verify(analysisService).update(open);
        verify(analysisService, never()).update(finalized);
    }

    // The FHIR ServiceRequest create hands the save a shell that carries only
    // the stored order's id: the progress is read from and written to the
    // stored row, never merged from the shell, and the shell learns the result.
    @Test
    public void aDetachedShellOfAStoredOrderAdvancesTheStoredRowAndMirrorsIt() {
        Sample stored = order("41", "ENTERED");
        Sample shell = new Sample();
        shell.setId("41");
        when(sampleService.get("41")).thenReturn(stored);

        service.recordStepSave(shell, "SAMPLES_PREPARED");

        verify(sampleService).update(stored);
        verify(sampleService, never()).update(shell);
        assertEquals("SAMPLES_PREPARED", stored.getOrderProgressStatus());
        assertEquals("SAMPLES_PREPARED", shell.getOrderProgressStatus());
        assertNotNull(shell.getOrderPreparedAt());
    }

    @Test
    public void aDetachedShellNeverResetsAReleasedOrder() {
        Sample stored = order("41", "READY_FOR_TESTING");
        Sample shell = new Sample();
        shell.setId("41");
        when(sampleService.get("41")).thenReturn(stored);

        service.recordStepSave(shell, null);

        verify(sampleService, never()).update(any(Sample.class));
        assertEquals("READY_FOR_TESTING", stored.getOrderProgressStatus());
        assertEquals("READY_FOR_TESTING", shell.getOrderProgressStatus());
    }

    @Test
    public void cancelLeavesTheSampleStatusAloneWhenNoneIsConfigured() {
        Sample sample = order("41", "ENTERED");
        sample.setStatusId("1");
        when(sampleService.get("41")).thenReturn(sample);
        when(statusService.getStatusID(SampleStatus.Canceled)).thenReturn("-1");

        Sample cancelled = service.cancel("41", "Duplicate order", "7");

        assertEquals("CANCELLED", cancelled.getOrderProgressStatus());
        assertEquals("1", cancelled.getStatusId());
    }

    @Test
    public void cancelNeedsAReasonAndRefusesACompleteOrder() {
        Sample sample = order("41", "READY_FOR_TESTING");
        when(sampleService.get("41")).thenReturn(sample);

        assertThrows(IllegalArgumentException.class, () -> service.cancel("41", "", "7"));
        assertThrows(IllegalStateException.class, () -> service.cancel("41", "Duplicate order", "7"));
    }

    private Sample order(String id, String status) {
        Sample sample = new Sample();
        sample.setId(id);
        sample.setOrderProgressStatus(status);
        Mockito.lenient().when(observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE, id))
                .thenReturn(null);
        return sample;
    }
}
