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
import java.util.List;
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
import org.openelisglobal.referral.service.ReferralService;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.sample.valueholder.OrderProgressStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceBlockedException;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceChecklistService;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceEvaluation;
import org.openelisglobal.sampleacceptance.service.SampleAcceptanceRecordService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

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
    private SampleItemService sampleItemService;
    @Mock
    private ObservationHistoryService observationHistoryService;
    @Mock
    private ReferralService referralService;

    @InjectMocks
    private OrderProgressServiceImpl service;

    @Test
    public void theFirstSaveMarksTheOrderEntered() {
        Sample sample = order("41", null);

        service.recordStepSave(sample, null, null);

        assertEquals("ENTERED", sample.getOrderProgressStatus());
        assertNotNull(sample.getOrderEnteredAt());
        assertNull(sample.getOrderPreparedAt());
        verify(sampleService).update(sample);
    }

    @Test
    public void aSaveThatCompletesPrepareSamplesMarksTheOrderPrepared() {
        Sample sample = order("41", "ENTERED");

        service.recordStepSave(sample, "SAMPLES_PREPARED", null);

        assertEquals("SAMPLES_PREPARED", sample.getOrderProgressStatus());
        assertNotNull(sample.getOrderPreparedAt());
    }

    @Test
    public void aLaterSaveNeverMovesTheStatusBack() {
        Sample sample = order("41", "READY_FOR_TESTING");

        service.recordStepSave(sample, null, null);
        service.recordStepSave(sample, "SAMPLES_PREPARED", null);

        assertEquals("READY_FOR_TESTING", sample.getOrderProgressStatus());
        verify(sampleService, never()).update(any());
    }

    @Test
    public void aCancelledOrderRefusesASave() {
        Sample sample = order("41", "CANCELLED");

        assertThrows(IllegalArgumentException.class, () -> service.recordStepSave(sample, null, null));
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
        when(acceptanceRecordService.evaluateOrder("41")).thenReturn(Arrays.asList(evaluation("PENDING")));

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
        Analysis alreadyCancelled = new Analysis();
        alreadyCancelled.setStatusId("4");
        when(statusService.matches("1", AnalysisStatus.NotStarted)).thenReturn(true);
        when(statusService.matches("4", AnalysisStatus.Canceled)).thenReturn(true);
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(Arrays.asList(open, alreadyCancelled));
        SampleItem item = new SampleItem();
        item.setStatusId("1");
        when(sampleItemService.getSampleItemsBySampleId("41")).thenReturn(Arrays.asList(item));

        Sample cancelled = service.cancel("41", "Registered in the wrong domain", "7");

        assertEquals("CANCELLED", cancelled.getOrderProgressStatus());
        assertEquals("Registered in the wrong domain", cancelled.getOrderCancelReason());
        assertEquals("7", cancelled.getOrderCancelledBy());
        assertNull(cancelled.getStatusId());
        assertEquals("4", open.getStatusId());
        assertEquals("12", item.getStatusId());
        verify(analysisService).update(open);
        verify(analysisService, never()).update(alreadyCancelled);
        verify(sampleItemService).update(item);
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

        service.recordStepSave(shell, "SAMPLES_PREPARED", null);

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

        service.recordStepSave(shell, null, null);

        verify(sampleService, never()).update(any(Sample.class));
        assertEquals("READY_FOR_TESTING", stored.getOrderProgressStatus());
        assertEquals("READY_FOR_TESTING", shell.getOrderProgressStatus());
    }

    @Test
    public void cancelLeavesTheSampleItemsAloneWhenNoCancelledStatusIsConfigured() {
        Sample sample = order("41", "ENTERED");
        sample.setStatusId("1");
        when(sampleService.get("41")).thenReturn(sample);
        when(statusService.getStatusID(SampleStatus.Canceled)).thenReturn("-1");

        Sample cancelled = service.cancel("41", "Duplicate order", "7");

        assertEquals("CANCELLED", cancelled.getOrderProgressStatus());
        assertEquals("1", cancelled.getStatusId());
        verify(sampleItemService, never()).update(any(SampleItem.class));
    }

    // Found in review: a test whose result was already accepted was cancelled
    // underneath the technician. Once any test has moved past Not started the
    // order is no longer cancellable from order entry.
    @Test
    public void cancelRefusesAnOrderWhoseTestingHasStarted() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(sampleService.get("41")).thenReturn(sample);
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");
        Analysis accepted = new Analysis();
        accepted.setStatusId("6");
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(Arrays.asList(accepted));

        assertThrows(IllegalStateException.class, () -> service.cancel("41", "Duplicate order", "7"));
        verify(analysisService, never()).update(any(Analysis.class));
        assertEquals("SAMPLES_PREPARED", sample.getOrderProgressStatus());
    }

    // The storage decision reaches the stored row even when the save carries a
    // detached copy of the order (an order reopened for editing).
    @Test
    public void theStorageDecisionIsStoredWithTheStep() {
        Sample stored = order("41", "ENTERED");
        Sample copy = new Sample();
        copy.setId("41");
        when(sampleService.get("41")).thenReturn(stored);

        service.recordStepSave(copy, null, Boolean.TRUE);

        verify(sampleService).update(stored);
        assertEquals(Boolean.TRUE, stored.getStorageSkipped());
        assertEquals(Boolean.TRUE, copy.getStorageSkipped());
    }

    // Found in review: the environmental and vector lanes have no Prepare
    // Samples step, so their Sample check releases from Entered.
    @Test
    public void anEnvironmentalOrderReleasesWithoutPrepareSamples() {
        Sample sample = order("41", "ENTERED");
        when(sampleService.get("41")).thenReturn(sample);
        when(observationHistoryService.getRawValueForSample(ObservationType.ENV_WORKFLOW_TYPE, "41"))
                .thenReturn("environmental");
        when(acceptanceChecklistService.getEnforcement("environmental")).thenReturn("OFF");

        Sample released = service.release("41", null, "7");

        assertEquals("READY_FOR_TESTING", released.getOrderProgressStatus());
    }

    // Found in review: with every specimen's checklist answered, Optional
    // acceptance asked for a reason anyway.
    @Test
    public void optionalAcceptanceNeedsNoReasonOnceEverySpecimenIsAnswered() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(sampleService.get("41")).thenReturn(sample);
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");
        when(acceptanceRecordService.evaluateOrder("41")).thenReturn(Arrays.asList(evaluation("ACCEPTED")));

        Sample released = service.release("41", null, "7");

        assertEquals("READY_FOR_TESTING", released.getOrderProgressStatus());
        assertNull(released.getOrderReleaseNote());
    }

    private SampleAcceptanceEvaluation evaluation(String overallStatus) {
        SampleAcceptanceEvaluation evaluation = new SampleAcceptanceEvaluation();
        evaluation.setOverallStatus(overallStatus);
        return evaluation;
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
    // ── OGC-1423: an order whose every test is referred out has nothing left for
    // the in-house Sample check ──────────────────────────────────────────────

    private Analysis analysis(String id) {
        Analysis analysis = new Analysis();
        analysis.setId(id);
        analysis.setStatusId("4");
        return analysis;
    }

    private Referral openReferral(String id) {
        Referral referral = new Referral();
        referral.setId(id);
        referral.setStatus(ReferralStatus.DRAFT);
        return referral;
    }

    @Test
    public void anOrderWithEveryTestReferredIsFullyReferredAndCompleteWithoutSampleCheck() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(List.of(analysis("a1"), analysis("a2")));
        when(referralService.getReferralByAnalysisId("a1")).thenReturn(openReferral("r1"));
        when(referralService.getReferralByAnalysisId("a2")).thenReturn(openReferral("r2"));
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");

        assertTrue(service.isFullyReferred("41"));
        assertFalse("the status alone still needs the Sample check",
                service.isComplete(OrderProgressStatus.SAMPLES_PREPARED, "clinical"));
        assertTrue(service.isComplete(sample, OrderProgressStatus.SAMPLES_PREPARED, "clinical"));
        assertFalse("an order still being entered is not complete even when referred",
                service.isComplete(sample, OrderProgressStatus.ENTERED, "clinical"));
    }

    @Test
    public void anOrderWithOneInHouseTestIsNotFullyReferred() {
        Sample sample = order("41", "SAMPLES_PREPARED");
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(List.of(analysis("a1"), analysis("a2")));
        when(referralService.getReferralByAnalysisId("a1")).thenReturn(openReferral("r1"));
        when(referralService.getReferralByAnalysisId("a2")).thenReturn(null);
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");

        assertFalse(service.isFullyReferred("41"));
        assertFalse(service.isComplete(sample, OrderProgressStatus.SAMPLES_PREPARED, "clinical"));
    }

    @Test
    public void aCancelledReferralOrACancelledTestDoesNotCount() {
        Referral cancelled = openReferral("r1");
        cancelled.setStatus(ReferralStatus.CANCELLED);
        when(analysisService.getAnalysesBySampleId("41")).thenReturn(List.of(analysis("a1"), analysis("gone")));
        when(statusService.matches("4", AnalysisStatus.Canceled)).thenReturn(false);
        when(referralService.getReferralByAnalysisId("a1")).thenReturn(cancelled);
        assertFalse("a cancelled referral leaves the test in-house", service.isFullyReferred("41"));

        Analysis gone = analysis("gone");
        gone.setStatusId("9");
        when(analysisService.getAnalysesBySampleId("42")).thenReturn(List.of(analysis("a1"), gone));
        when(statusService.matches("9", AnalysisStatus.Canceled)).thenReturn(true);
        when(referralService.getReferralByAnalysisId("a1")).thenReturn(openReferral("r1"));
        assertTrue("a cancelled test is ignored", service.isFullyReferred("42"));

        when(analysisService.getAnalysesBySampleId("43")).thenReturn(List.of());
        assertFalse("no tests, nothing referred", service.isFullyReferred("43"));
    }

    @Test
    public void oneOpenReferralRuleServesTheDashboardCountsAndTheFullyReferredCheck() {
        assertTrue(service.isOpenReferral(openReferral("r1")));
        Referral requested = openReferral("r2");
        requested.setStatus(ReferralStatus.REQUESTED);
        assertTrue(service.isOpenReferral(requested));

        Referral rejected = openReferral("r3");
        rejected.setStatus(ReferralStatus.REJECTED);
        assertFalse("a referral the reference lab rejected is back in-house", service.isOpenReferral(rejected));
        Referral cancelled = openReferral("r4");
        cancelled.setStatus(ReferralStatus.CANCELLED);
        assertFalse(service.isOpenReferral(cancelled));
        assertFalse(service.isOpenReferral(null));
        assertFalse("an unsaved referral does not count", service.isOpenReferral(new Referral()));

        when(analysisService.getAnalysesBySampleId("44")).thenReturn(List.of(analysis("a1"), analysis("a2")));
        when(referralService.getReferralByAnalysisId("a1")).thenReturn(openReferral("r1"));
        when(referralService.getReferralByAnalysisId("a2")).thenReturn(rejected);
        assertFalse("a rejected referral leaves the order partially referred", service.isFullyReferred("44"));
    }

    @Test
    public void aPrecomputedFullyReferredAnswerCompletesOnlyAPreparedOrder() {
        when(acceptanceChecklistService.getEnforcement("clinical")).thenReturn("OPTIONAL");
        assertTrue(service.isComplete(OrderProgressStatus.SAMPLES_PREPARED, "clinical", true));
        assertFalse(service.isComplete(OrderProgressStatus.ENTERED, "clinical", true));
        assertFalse(service.isComplete(OrderProgressStatus.SAMPLES_PREPARED, "clinical", false));
        assertTrue("a released order is complete whatever the referrals say",
                service.isComplete(OrderProgressStatus.READY_FOR_TESTING, "clinical", false));
    }
}
