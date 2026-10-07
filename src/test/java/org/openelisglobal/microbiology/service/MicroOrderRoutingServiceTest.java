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
import org.openelisglobal.testmethod.service.TestMethodService;

@RunWith(MockitoJUnitRunner.class)
public class MicroOrderRoutingServiceTest {

    @Mock
    private MicroCaseService caseService;

    @Mock
    private MicrobiologyReferenceService referenceService;

    @Mock
    private MicroCaseAnalysisService caseAnalysisService;

    @Mock
    private TestMethodService testMethodService;

    @Test
    public void routeAnalysesIgnoresNonMicrobiologyTests() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);

        List<MicroCase> routed = service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(analysis(null, "1")),
                "1");

        assertTrue(routed.isEmpty());
        verify(caseService, never()).createOrGetCase(any(String.class), any(MicroWorkflowType.class), any(String.class),
                any(String.class));
    }

    @Test
    public void routeAnalysesCreatesOneCasePerWorkflowWithConfiguredCultureSetup() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
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
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1");
    }

    @Test
    public void routeAnalysesUsesTheTestDefault() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-1");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1")).thenReturn(routedCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1");

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");
    }

    @Test
    public void routeAnalysesUsesTheTestMethodDefaultBeforeTheLegacyMethod() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
        when(testMethodService.getDefaultMethodId("test-BACTERIOLOGY-1")).thenReturn("2");
        when(referenceService.getActiveCultureSetupForMethod("2", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("2", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-1");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1")).thenReturn(routedCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1");

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1");
    }

    @Test
    public void routeAnalysesCreatesCaseWhenNoDefaultCultureMethodResolves() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
        Analysis analysis = analysis(MicroWorkflowType.BACTERIOLOGY.name(), null);
        analysis.setId("analysis-1");
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-without-protocol");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, null, "1")).thenReturn(routedCase);

        List<MicroCase> routed = service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(analysis), "1");

        assertEquals(1, routed.size());
        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, null, "1");
        verify(referenceService, never()).getActiveCultureSetupForMethod(any(String.class), any());
        verify(caseAnalysisService).linkAnalysis(routedCase, analysis, null);
    }

    @Test
    public void routeAnalysesKeepsSiblingWorkflowOnItsOwnDefaultMethod() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));
        when(referenceService.getActiveCultureSetupForMethod("9", MicroWorkflowType.MYCOBACTERIOLOGY_TB))
                .thenReturn(cultureSetup("9", MicroWorkflowType.MYCOBACTERIOLOGY_TB));
        MicroCase bacteriologyCase = new MicroCase();
        bacteriologyCase.setId("case-bacteriology");
        MicroCase tbCase = new MicroCase();
        tbCase.setId("case-tb");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1"))
                .thenReturn(bacteriologyCase);
        when(caseService.createOrGetCase("1001", MicroWorkflowType.MYCOBACTERIOLOGY_TB, "9", "1")).thenReturn(tbCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1"),
                        analysis(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), "9")),
                "1");

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");
        verify(caseService).createOrGetCase("1001", MicroWorkflowType.MYCOBACTERIOLOGY_TB, "9", "1");
    }

    @Test
    public void routeAnalysesLinksPersistedAnalysesToTheCaseAndReportMapping() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                caseAnalysisService, testMethodService);
        MicroCultureSetup setup = cultureSetup("1", MicroWorkflowType.BACTERIOLOGY);
        setup.setReportableTestAnalyteId("17");
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        Analysis analysis = analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1");
        analysis.setId("42");
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY)).thenReturn(setup);
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1")).thenReturn(microCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(analysis), "1");

        verify(caseAnalysisService).linkAnalysis(microCase, analysis, setup);
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
        if (methodId != null) {
            Method method = new Method();
            method.setId(methodId);
            test.setMethod(method);
        }
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
