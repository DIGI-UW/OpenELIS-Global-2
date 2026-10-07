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
}
