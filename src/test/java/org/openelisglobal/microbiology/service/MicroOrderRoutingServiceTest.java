package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.class)
public class MicroOrderRoutingServiceTest {

    @Mock
    private MicroCaseService caseService;

    @Mock
    private MicrobiologyReferenceService referenceService;

    @Test
    public void routeAnalysesIgnoresNonMicrobiologyTests() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService);

        List<MicroCase> routed = service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(analysis(null, "1")),
                "1");

        assertTrue(routed.isEmpty());
        verify(caseService, never()).createOrGetCase(any(String.class), any(MicroWorkflowType.class), any(String.class),
                any(String.class));
    }

    @Test
    public void routeAnalysesCreatesOneCasePerWorkflowWithConfiguredCultureSetup() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService);
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                Arrays.asList(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1"),
                        analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")),
                "1");

        ArgumentCaptor<MicroWorkflowType> workflowCaptor = ArgumentCaptor.forClass(MicroWorkflowType.class);
        verify(caseService).createOrGetCase(eq("1001"), workflowCaptor.capture(), any(String.class), any(String.class));
        assertEquals(MicroWorkflowType.BACTERIOLOGY, workflowCaptor.getValue());
    }

    @Test(expected = IllegalStateException.class)
    public void routeAnalysesRejectsWorkflowWithoutConfiguredCultureSetup() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1");
    }

    private SampleItem sampleItem(String id) {
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId(id);
        return sampleItem;
    }

    private Analysis analysis(String workflowType, String methodId) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("test-" + workflowType + "-" + methodId);
        test.setCultureWorkflowType(workflowType);
        Method method = new Method();
        method.setId(methodId);
        test.setMethod(method);
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        return analysis;
    }

    private MicroCultureSetup cultureSetup(String methodId, MicroWorkflowType workflowType) {
        MicroCultureSetup setup = new MicroCultureSetup();
        setup.setMethodId(methodId);
        setup.setWorkflowType(workflowType.name());
        return setup;
    }
}
