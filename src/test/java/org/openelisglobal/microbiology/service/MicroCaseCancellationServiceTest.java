package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sampletyperequest.dao.SampleTypeRequestDAO;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.service.TestSectionService;

public class MicroCaseCancellationServiceTest {
    @Test
    public void recordedResultsRequireReasonEvenWhenCaseWasConfirmed() {
        var cases = mock(MicroCaseDAO.class);
        var ownership = mock(MicroCaseRequestedTestDAO.class);
        var links = mock(MicroCaseAnalysisDAO.class);
        var activities = mock(MicroCaseActivityDAO.class);
        var isolates = mock(MicroIsolateDAO.class);
        var requests = mock(SampleTypeRequestDAO.class);
        var panels = mock(PanelItemService.class);
        var analyses = mock(AnalysisService.class);
        var results = mock(ResultService.class);
        var statuses = mock(IStatusService.class);
        var units = mock(TestSectionService.class);
        var owner = new MicroCase();
        owner.setId("case");
        owner.setSampleId("order");
        owner.setTestSectionId("unit");
        var link = new MicroCaseRequestedTest();
        link.setRequestId(1);
        link.setTestId("test");
        link.setCaseId("case");
        var request = new SampleTypeRequest();
        request.setId(1);
        request.setStatus(SampleTypeRequest.Status.CANCELLED);
        when(cases.getByOrder("order")).thenReturn(List.of(owner));
        when(ownership.getByCaseId("case")).thenReturn(List.of(link));
        when(requests.getRequestsBySampleId("order")).thenReturn(List.of(request));
        var observation = new MicroCaseActivity();
        observation.setResultSourceSampleItemId("specimen");
        when(activities.getByCaseId("case")).thenReturn(List.of(observation));
        var service = new MicroCaseCancellationService(cases, ownership, links, activities, isolates, requests, panels,
                analyses, results, statuses, units);
        var rejected = assertThrows(MicroCaseCancellationRequiredException.class,
                () -> service.reconcile("order", List.of("case"), "  ", "actor"));
        assertTrue(rejected.getCases().get(0).hasResults());
        verify(cases, never()).update(any());
        verify(ownership, never()).update(any());
        service.reconcile("order", List.of("case"), "Duplicate corrected", "actor");
        assertEquals("CANCELLED", owner.getStage());
        assertEquals("Duplicate corrected", link.getCancellationReason());
        verify(cases).update(owner);
        verify(ownership).update(link);
        verifyZeroInteractions(results);
    }

    @Test
    public void collectedLastTestRequiresConsentAndRetainsAnalysisLink() {
        var f = new CollectedFixture();
        assertThrows(MicroCaseCancellationRequiredException.class, () -> f.reconcile(List.of()));
        verify(f.cases, never()).update(any());
        f.reconcile(List.of("case"));
        assertEquals("CANCELLED", f.owner.getStage());
        assertNotNull(f.requestLink.getCancelledAt());
        verify(f.links, never()).delete(any(MicroCaseAnalysis.class));
    }

    @Test
    public void anotherLiveAnalysisKeepsCollectedCaseOpen() {
        var f = new CollectedFixture();
        var other = new org.openelisglobal.analysis.valueholder.Analysis();
        other.setId("other");
        other.setStatusId("live");
        var link = new MicroCaseAnalysis();
        link.setAnalysisId("other");
        when(f.analyses.get("other")).thenReturn(other);
        when(f.links.getByCaseId("case")).thenReturn(List.of(f.analysisLink, link));
        f.reconcile(List.of());
        assertNotNull(f.requestLink.getCancelledAt());
        assertEquals("RECEIVED", f.owner.getStage());
        verify(f.cases, never()).update(any());
    }

    @Test
    public void directlyCollectedCaseWithoutRequestAlsoRequiresConsent() {
        var f = new CollectedFixture();
        when(f.ownership.getByCaseId("case")).thenReturn(List.of());
        assertThrows(MicroCaseCancellationRequiredException.class, () -> f.reconcile(List.of()));
        f.reconcile(List.of("case"));
        assertEquals("CANCELLED", f.owner.getStage());
    }

    @Test
    public void unrelatedSaveDoesNotCancelHistoricalEmptyCase() {
        var f = new CollectedFixture();
        f.service.reconcile("order", List.of(), null, "actor");
        verify(f.cases, never()).update(any());
        verify(f.ownership, never()).update(any());
    }

    @Test
    public void collectedEditCannotCancelFinalReleasedCase() {
        var f = new CollectedFixture();
        f.owner.setFinalReleaseState("FINAL_RELEASED");
        assertThrows(MicroCaseLockedException.class, () -> f.reconcile(List.of("case")));
        verify(f.cases, never()).update(any());
        verify(f.ownership, never()).update(any());
    }

    private static class CollectedFixture {
        final MicroCaseDAO cases = mock(MicroCaseDAO.class);
        final MicroCaseRequestedTestDAO ownership = mock(MicroCaseRequestedTestDAO.class);
        final MicroCaseAnalysisDAO links = mock(MicroCaseAnalysisDAO.class);
        final AnalysisService analyses = mock(AnalysisService.class);
        final MicroCase owner = new MicroCase();
        final MicroCaseRequestedTest requestLink = new MicroCaseRequestedTest();
        final MicroCaseAnalysis analysisLink = new MicroCaseAnalysis();
        final MicroCaseCancellationService service;

        CollectedFixture() {
            var requests = mock(SampleTypeRequestDAO.class);
            var statuses = mock(IStatusService.class);
            when(statuses.getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Canceled))
                    .thenReturn("cancelled");
            owner.setId("case");
            owner.setTestSectionId("unit");
            when(cases.getByOrder("order")).thenReturn(List.of(owner));
            var specimen = new org.openelisglobal.sampleitem.valueholder.SampleItem();
            specimen.setId("specimen");
            var test = new org.openelisglobal.test.valueholder.Test();
            test.setId("test");
            var analysis = new org.openelisglobal.analysis.valueholder.Analysis();
            analysis.setId("analysis");
            analysis.setStatusId("cancelled");
            analysis.setSampleItem(specimen);
            analysis.setTest(test);
            when(analyses.get("analysis")).thenReturn(analysis);
            analysisLink.setAnalysisId("analysis");
            when(links.getByCaseId("case")).thenReturn(List.of(analysisLink));
            var request = new SampleTypeRequest();
            request.setId(1);
            request.setStatus(SampleTypeRequest.Status.COLLECTED);
            request.setSampleItem(specimen);
            when(requests.getRequestsBySampleId("order")).thenReturn(List.of(request));
            requestLink.setRequestId(1);
            requestLink.setTestId("test");
            when(ownership.getByCaseId("case")).thenReturn(List.of(requestLink));
            service = new MicroCaseCancellationService(cases, ownership, links, mock(MicroCaseActivityDAO.class),
                    mock(MicroIsolateDAO.class), requests, mock(PanelItemService.class), analyses,
                    mock(ResultService.class), statuses, mock(TestSectionService.class));
        }

        void reconcile(List<String> confirmed) {
            service.reconcile("order", confirmed, "Corrected order", "actor", List.of("analysis"));
        }
    }
}
