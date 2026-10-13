package org.openelisglobal.microbiology;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.test.service.TestService;

public class MicroIncomingResultServiceTest {
    private final MicroIncomingItemDAO queue = mock(MicroIncomingItemDAO.class);
    private final MicroCaseDAO cases = mock(MicroCaseDAO.class);
    private final MicroCaseAnalysisDAO links = mock(MicroCaseAnalysisDAO.class);
    private final MicroCaseActivityDAO activities = mock(MicroCaseActivityDAO.class);
    private final MicrobiologyCaseAccessService access = mock(MicrobiologyCaseAccessService.class);
    private final MicroCaseResultService results = mock(MicroCaseResultService.class);
    private final TestService tests = mock(TestService.class);
    private MicroIncomingResultService service;
    private MicroCase c;
    private MicroCaseResultRequestForm payload;

    @Before
    public void setup() {
        service = new MicroIncomingResultServiceImpl(queue, cases, links, activities, access, results, tests,
                new ObjectMapper());
        c = new MicroCase();
        c.setId("case");
        c.setLabUnitId("unit");
        when(cases.getForUpdate("case")).thenReturn(c);
        var test = new org.openelisglobal.test.valueholder.Test();
        test.setId("12");
        test.setName("Culture");
        when(tests.get("12")).thenReturn(test);
        payload = new MicroCaseResultRequestForm();
        var component = new MicroCaseResultRequestForm.ResultComponent();
        component.componentId = "primary";
        component.value = "3";
        payload.components = List.of(component);
    }

    @Test
    public void newUnorderedResultWaitsWithoutCreatingAnalysis() {
        var f = service.receive("case", "tube", "12", "ANALYZER", "source-run", "3", payload, true, "1");
        assertEquals("WAITING", f.state);
        assertEquals("3", f.result.components.get(0).value);
        verifyZeroInteractions(results);
        verify(queue).insert(any(MicroIncomingItem.class));
    }

    @Test
    public void retryReturnsOriginalEvenAfterMove() {
        var prior = new MicroIncomingItem();
        prior.setMicroCase(c);
        prior.setOriginalCaseId("case");
        prior.setTestId("12");
        prior.setState("PLACED");
        prior.setStructuredResult("{\"components\":[]}");
        when(queue.findDelivery("case", "12", "source-run")).thenReturn(prior);
        var f = service.receive("case", "tube", "12", "ANALYZER", "source-run", "3", payload, true, "1");
        assertEquals(prior.getId(), f.id);
        assertEquals("PLACED", f.state);
        verify(queue, never()).insert(any());
        verifyZeroInteractions(results);
    }

    @Test
    public void finalCaseKeepsArrivalForAmendment() {
        c.setFinalReleaseState("FINAL_RELEASED");
        var f = service.receive("case", "tube", "12", "ANALYZER", "source-run", "3", payload, true, "1");
        assertEquals("NEEDS_AMENDMENT", f.state);
        verifyZeroInteractions(results);
    }

    @Test
    public void exactOrderedResultAttachesAndStillUsesNormalResultValidation() {
        var a = new org.openelisglobal.analysis.valueholder.Analysis();
        a.setId("analysis");
        a.setTest(tests.get("12"));
        var tube = new org.openelisglobal.sampleitem.valueholder.SampleItem();
        tube.setId("tube");
        a.setSampleItem(tube);
        when(links.getAnalyses("case")).thenReturn(List.of(a));
        var f = service.receive("case", "tube", "12", "ANALYZER", "source-run", "3", payload, true, "1");
        assertEquals("PLACED", f.state);
        assertEquals("analysis", f.placementTarget);
        verify(results).receiveResult("case", "analysis", payload, "1");
    }

    @Test
    public void placementRequiresResultsRightsBeforeAddingAnyTests() {
        var request = new MicroIncomingPlacementForm();
        request.sampleItemId = "tube";
        try {
            service.place("case", "incoming", request, "outsider", null);
            fail("Denied user must not place a result");
        } catch (org.springframework.security.access.AccessDeniedException expected) {
            verifyZeroInteractions(results);
        }
    }
}
