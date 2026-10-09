package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public class MicroCaseMembershipServiceTest {
    private final MicroCaseDAO cases = mock(MicroCaseDAO.class);
    private final MicroCaseSampleDAO members = mock(MicroCaseSampleDAO.class);
    private final MicroCaseRequestDAO requests = mock(MicroCaseRequestDAO.class);
    private final MicroCaseSplitDAO splits = mock(MicroCaseSplitDAO.class);
    private final MicroCaseActivityDAO activities = mock(MicroCaseActivityDAO.class);
    private final MicroCaseAnalysisDAO links = mock(MicroCaseAnalysisDAO.class);
    private final MicroCaseAnalysisService linkService = mock(MicroCaseAnalysisService.class);
    private final AnalysisService analyses = mock(AnalysisService.class);
    private final ResultService results = mock(ResultService.class);
    private MicroCaseMembershipServiceImpl service;
    private MicroCase microCase;
    private SampleTypeRequest request;
    private SampleItem sample;
    private Analysis analysis;
    private MicroCaseAnalysis link;

    @Before
    public void setup() {
        service = new MicroCaseMembershipServiceImpl(cases, members, requests, splits, activities, links, linkService,
                analyses, results);
        Sample order = new Sample();
        order.setId("10");
        TypeOfSample type = new TypeOfSample();
        type.setId("20");
        microCase = new MicroCase();
        microCase.setId("case-a");
        microCase.setSampleId("10");
        microCase.setSampleTypeId("20");
        microCase.setLabUnitId("30");
        request = new SampleTypeRequest();
        request.setId(40);
        request.setSample(order);
        request.setTypeOfSample(type);
        sample = new SampleItem();
        sample.setId("50");
        sample.setSample(order);
        sample.setTypeOfSample(type);
        analysis = new Analysis();
        analysis.setId("60");
        analysis.setSampleItem(sample);
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("70");
        analysis.setTest(test);
        link = new MicroCaseAnalysis();
        link.setCaseId("case-a");
        link.setAnalysisId("60");
        when(cases.getForUpdate("case-a")).thenReturn(microCase);
        when(requests.lockSampleTypeRequest(40)).thenReturn(request);
        when(members.lockSampleItem("50")).thenReturn(sample);
        when(analyses.get("60")).thenReturn(analysis);
        when(linkService.linkAnalysis(microCase, analysis, null)).thenReturn(link);
        when(requests.getBySampleItemId("50")).thenReturn(Collections.emptyList());
    }

    private MicroCaseRequest ownership() {
        MicroCaseRequest o = new MicroCaseRequest();
        o.setId("request-a");
        o.setCaseId("case-a");
        o.setSampleTypeRequestId(40);
        o.setSampleTypeId("20");
        o.setTestId("70");
        o.setCaseRole(MicroCaseRole.CULTURE);
        o.setCollectedInSets(true);
        when(requests.get("request-a")).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    public void requestOwnsWorkBeforePhysicalSampleOrAnalysisExists() {
        MicroCaseRequest owned = service.ownRequest("case-a", 40, "70", MicroCaseRole.CULTURE, true, "actor");
        assertNull(owned.getSampleItemId());
        assertNull(owned.getAnalysisId());
        assertNull(request.getSampleItem());
        assertNull(microCase.getSampleItemId());
        assertEquals(MicroCaseRole.CULTURE, owned.getCaseRole());
        assertTrue(owned.isCollectedInSets());
        verify(requests).insert(owned);
        verifyZeroInteractions(members, linkService, analyses);
        ArgumentCaptor<MicroCaseActivity> event = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activities).insert(event.capture());
        assertEquals("actor", event.getValue().getPerformedBy());
        assertEquals("REQUEST_OWNED", event.getValue().getActivityType());
    }

    @Test
    public void retryKeepsExistingRoleSnapshotAndDoesNotWrite() {
        MicroCaseRequest owned = ownership();
        when(requests.getActiveByRequestAndTest(40, "70")).thenReturn(owned);
        assertSame(owned, service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor"));
        assertEquals(MicroCaseRole.CULTURE, owned.getCaseRole());
        verify(requests, never()).insert(any());
        verifyZeroInteractions(activities);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestCannotBeOwnedByTwoCases() {
        MicroCaseRequest owned = ownership();
        owned.setCaseId("other");
        when(requests.getActiveByRequestAndTest(40, "70")).thenReturn(owned);
        service.ownRequest("case-a", 40, "70", MicroCaseRole.CULTURE, true, "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestFromAnotherOrderIsRejected() {
        request.getSample().setId("other");
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void requestedSampleTypeMustMatchCase() {
        request.getTypeOfSample().setId("other");
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void directTestCannotBeCollectedInSets() {
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, true, "actor");
    }

    @Test
    public void collectionAttachesToRequestedCaseAndOrdinaryAnalysis() {
        MicroCaseRequest owned = ownership();
        assertSame(owned, service.fulfillRequest("request-a", "50", "60", "collector"));
        assertEquals("case-a", owned.getCaseId());
        assertEquals("50", owned.getSampleItemId());
        assertEquals("60", owned.getAnalysisId());
        assertEquals(MicroCaseRole.CULTURE, link.getCaseRole());
        assertEquals(Boolean.TRUE, link.getCollectedInSets());
        verify(members).insert(any(MicroCaseSample.class));
        verify(linkService).linkAnalysis(microCase, analysis, null);
        verify(requests).update(owned);
        assertNull(sample.getCollectionDate());
    }

    @Test
    public void sameCollectionRetryAddsNoMembershipOrAnalysis() {
        MicroCaseRequest owned = ownership();
        owned.setSampleItemId("50");
        owned.setAnalysisId("60");
        assertSame(owned, service.fulfillRequest("request-a", "50", "60", "collector"));
        verify(members, never()).insert(any());
        verifyZeroInteractions(linkService, activities);
    }

    @Test(expected = IllegalArgumentException.class)
    public void collectionRetryCannotReplaceAnalysis() {
        MicroCaseRequest owned = ownership();
        owned.setSampleItemId("50");
        owned.setAnalysisId("other");
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void oneBottleCannotFulfillTwoRequests() {
        ownership();
        MicroCaseRequest other = new MicroCaseRequest();
        other.setSampleTypeRequestId(99);
        when(requests.getBySampleItemId("50")).thenReturn(Arrays.asList(other));
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongTestAnalysisIsRejected() {
        ownership();
        analysis.getTest().setId("other");
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void analysisAlreadyOnOtherCaseIsRejectedBeforeMembershipWrite() {
        ownership();
        MicroCaseAnalysis other = new MicroCaseAnalysis();
        other.setCaseId("other");
        when(links.getActiveByAnalysisId("60")).thenReturn(other);
        try {
            service.fulfillRequest("request-a", "50", "60", "actor");
        } finally {
            verify(members, never()).insert(any());
        }
    }

    @Test
    public void caseRoleAttachesSampleWithoutCreatingResultAnalysis() {
        MicroCaseRequest owned = ownership();
        owned.setCaseRole(MicroCaseRole.CASE);
        owned.setCollectedInSets(false);
        service.fulfillRequest("request-a", "50", null, "actor");
        assertEquals("50", owned.getSampleItemId());
        assertNull(owned.getAnalysisId());
        verifyZeroInteractions(analyses, linkService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void caseRoleCannotHaveResultAnalysis() {
        ownership().setCaseRole(MicroCaseRole.CASE);
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test
    public void cancellationPreservesOwnershipAndAttribution() {
        MicroCaseRequest owned = ownership();
        service.cancelRequest("request-a", "Patient declined", "actor");
        assertEquals("actor", owned.getCancelledBy());
        assertEquals("Patient declined", owned.getCancellationReason());
        assertNotNull(owned.getCancelledAt());
        assertEquals("case-a", owned.getCaseId());
        verify(requests, never()).delete(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cancelledOwnershipCannotBeFulfilled() {
        ownership().setCancelledAt(new Timestamp(1));
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cancelledSharedRequestCannotBeReactivated() {
        request.setStatus(SampleTypeRequest.Status.CANCELLED);
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test
    public void cancellationPreservesExistingAnalysisAndLinkHistory() {
        MicroCaseRequest owned = ownership();
        owned.setAnalysisId("60");
        when(links.getByCaseAndAnalysis("case-a", "60")).thenReturn(link);
        service.cancelRequest("request-a", "Order changed", "actor");
        assertNotNull(link.getCancelledAt());
        assertEquals("actor", link.getCancelledBy());
        assertEquals("60", owned.getAnalysisId());
        verify(analyses, never()).update(any());
        verify(links).update(link);
    }

    @Test
    public void cultureCollectedInSetsCanOwnDifferentRequestedSampleTypes() {
        microCase.setSampleTypeId("other");
        assertEquals("20",
                service.ownRequest("case-a", 40, "70", MicroCaseRole.CULTURE, true, "actor").getSampleTypeId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cancelledAnalysisCannotBeReusedForReorderedRequest() {
        ownership();
        MicroCaseRequest cancelled = new MicroCaseRequest();
        cancelled.setAnalysisId("60");
        cancelled.setCancelledAt(new Timestamp(1));
        cancelled.setSampleTypeRequestId(40);
        when(requests.getBySampleItemId("50")).thenReturn(Arrays.asList(cancelled));
        service.fulfillRequest("request-a", "50", "60", "actor");
    }

    @Test
    public void attachmentPreservesExistingAnalysisPlacement() {
        ownership();
        link.setPlacement("ADDITIONAL_TESTING");
        link.setCaseRole(MicroCaseRole.CULTURE);
        link.setCollectedInSets(true);
        service.fulfillRequest("request-a", "50", "60", "actor");
        assertEquals("ADDITIONAL_TESTING", link.getPlacement());
    }

    @Test(expected = MicroCaseLockedException.class)
    public void cancelledCaseRejectsOwnership() {
        microCase.setStatus(MicroCaseStatus.CANCELLED);
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test(expected = MicroCaseLockedException.class)
    public void rejectedCaseRejectsOwnership() {
        microCase.setStatus(MicroCaseStatus.REJECTED);
        service.addSample("case-a", "50", "actor");
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingHistoricalLabUnitCannotBeGuessed() {
        microCase.setLabUnitId(null);
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test
    public void cancelledOwnershipRetryDoesNotAppendHistory() {
        MicroCaseRequest owned = ownership();
        owned.setCancelledAt(new Timestamp(1));
        service.cancelRequest("request-a", "reason", "actor");
        verify(requests, never()).update(any());
        verifyZeroInteractions(activities);
    }

    @Test(expected = MicroCaseLockedException.class)
    public void finalReleasedCaseRejectsNewOwnership() {
        microCase.setFinalReleaseState("FINAL_RELEASED");
        service.ownRequest("case-a", 40, "70", MicroCaseRole.DIRECT, false, "actor");
    }

    @Test
    public void sharedCancellationDoesNotPreventRecordingOwnershipCancellation() {
        MicroCaseRequest owned = ownership();
        request.setStatus(SampleTypeRequest.Status.CANCELLED);
        service.cancelRequest("request-a", "Collection declined", "actor");
        assertNotNull(owned.getCancelledAt());
        service.cancelRequest("request-a", "Collection declined", "actor");
        verify(requests).update(owned);
    }

    @Test
    public void uncollectedOwnershipCanBeCancelledWithoutReason() {
        MicroCaseRequest owned = ownership();
        service.cancelRequest("request-a", null, "actor");
        assertNotNull(owned.getCancelledAt());
        assertNull(owned.getCancellationReason());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cancellationWithStoredResultsRequiresReason() {
        ownership().setAnalysisId("60");
        when(results.getResultsByAnalysis(analysis)).thenReturn(Arrays.asList(new Result()));
        service.cancelRequest("request-a", null, "actor");
    }

    @Test
    public void cancellationKeepsStoredResultValues() {
        ownership().setAnalysisId("60");
        Result result = new Result();
        result.setValue("Positive");
        when(results.getResultsByAnalysis(analysis)).thenReturn(Arrays.asList(result));
        when(links.getByCaseAndAnalysis("case-a", "60")).thenReturn(link);
        service.cancelRequest("request-a", "Order corrected", "actor");
        assertEquals("Positive", result.getValue());
        verify(results, never()).update(any());
        verify(results, never()).delete(any());
    }

    @Test
    public void membershipUniquenessIsPerCaseIncludingSameLabUnit() {
        MicroCaseSample existing = new MicroCaseSample();
        existing.setCaseId("case-a");
        existing.setSampleItemId("50");
        when(members.getActiveByCaseAndSample("case-a", "50")).thenReturn(existing);
        assertSame(existing, service.addSample("case-a", "50", "actor"));
        verify(members, never()).insert(any());
        MicroCase other = new MicroCase();
        other.setId("case-b");
        other.setSampleId("10");
        other.setSampleTypeId("20");
        other.setLabUnitId("30");
        when(cases.getForUpdate("case-b")).thenReturn(other);
        assertEquals("case-b", service.addSample("case-b", "50", "actor").getCaseId());
        verify(members).insert(any(MicroCaseSample.class));
    }

    @Test
    public void relatedCasesIncludeSplitLineageWithoutSharedSamples() {
        when(members.getRelatedCaseIds("case-a")).thenReturn(Arrays.asList("case-b"));
        MicroCaseSplit split=new MicroCaseSplit(); split.setSourceCaseId("case-a"); split.setResultCaseId("case-c"); when(splits.getByCaseId("case-a")).thenReturn(Arrays.asList(split));
        assertEquals(Arrays.asList("case-b","case-c"),service.getRelatedCaseIds("case-a"));
        when(members.getRelatedCaseIds("case-c")).thenReturn(Collections.emptyList()); when(splits.getByCaseId("case-c")).thenReturn(Arrays.asList(split));
        assertEquals(Arrays.asList("case-a"),service.getRelatedCaseIds("case-c"));
    }
}
