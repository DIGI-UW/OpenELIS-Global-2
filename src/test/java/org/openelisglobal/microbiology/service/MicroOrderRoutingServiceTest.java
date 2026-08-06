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
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
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
    private MicroCaseOrderDetailService orderDetailService;

    @Mock
    private MicroCaseAnalysisService caseAnalysisService;

    @Mock
    private TestMethodService testMethodService;

    @Test
    public void routeAnalysesIgnoresNonMicrobiologyTests() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");

        List<MicroCase> routed = service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(analysis(null, "1")),
                "1");

        assertTrue(routed.isEmpty());
        verify(caseService, never()).createOrGetCase(any(String.class), any(MicroWorkflowType.class), any(String.class),
                any(String.class));
    }

    @Test
    public void manualMicrobiologyProgramCreatesUnassignedCaseWhenNoDefaultIsConfigured() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        Analysis untypedAnalysis = analysis(null, "1");
        untypedAnalysis.setId("analysis-1");
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-unassigned");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.UNASSIGNED, "1", "1")).thenReturn(routedCase);

        List<MicroCase> routed = service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(untypedAnalysis), "1",
                new MicroCaseOrderDetailRequestForm(), true);

        assertEquals(1, routed.size());
        verify(caseService).createOrGetCase("1001", MicroWorkflowType.UNASSIGNED, "1", "1");
        verify(caseAnalysisService).linkAnalysis(routedCase, untypedAnalysis, null);
        verify(referenceService, never()).getActiveCultureSetupForMethod(any(String.class), any());
    }

    @Test
    public void manualMicrobiologyProgramUsesExplicitDeploymentDefault() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "BACTERIOLOGY");
        Analysis untypedAnalysis = analysis(null, "1");
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-bacteriology");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1")).thenReturn(routedCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"), List.of(untypedAnalysis), "1",
                new MicroCaseOrderDetailRequestForm(), true);

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1");
    }

    @Test
    public void routeAnalysesCreatesOneCasePerWorkflowWithConfiguredCultureSetup() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
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
                orderDetailService, caseAnalysisService, testMethodService, "");

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1");
    }

    @Test
    public void routeAnalysesWithOrderDetailPersistsDetailOnEveryRoutedCase() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-1");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1")).thenReturn(routedCase);
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.patientOrigin = "Emergency department";

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", orderDetail);

        verify(orderDetailService).saveOrderDetail("case-1", orderDetail, "1");
    }

    @Test
    public void routeAnalysesUsesTheSelectedLinkedCultureMethod() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        when(testMethodService.testMethodLinkExists("test-BACTERIOLOGY-1", "2")).thenReturn(true);
        when(referenceService.getActiveCultureSetupForMethod("2", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("2", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-1");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1")).thenReturn(routedCase);
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.cultureMethodId = "2";

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", orderDetail);

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1");
    }

    @Test
    public void routeAnalysesUsesTheTestMethodDefaultBeforeTheLegacyMethod() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
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

    @Test(expected = IllegalArgumentException.class)
    public void routeAnalysesRejectsMoreThanTenCultureSets() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.numberOfSets = 11;

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", orderDetail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void routeAnalysesRejectsClinicalHistoryOverOneThousandCharacters() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.clinicalHistory = "x".repeat(1001);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", orderDetail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void routeAnalysesRejectsSelectedMethodNotLinkedToTheTest() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.cultureMethodId = "2";

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", orderDetail);
    }

    @Test
    public void routeAnalysesKeepsSiblingWorkflowOnItsOwnDefaultMethod() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        when(testMethodService.testMethodLinkExists("test-BACTERIOLOGY-1", "2")).thenReturn(true);
        when(referenceService.getActiveCultureSetupForMethod("2", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("2", MicroWorkflowType.BACTERIOLOGY));
        when(referenceService.getActiveCultureSetupForMethod("9", MicroWorkflowType.MYCOBACTERIOLOGY_TB))
                .thenReturn(cultureSetup("9", MicroWorkflowType.MYCOBACTERIOLOGY_TB));
        MicroCase bacteriologyCase = new MicroCase();
        bacteriologyCase.setId("case-bacteriology");
        MicroCase tbCase = new MicroCase();
        tbCase.setId("case-tb");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1"))
                .thenReturn(bacteriologyCase);
        when(caseService.createOrGetCase("1001", MicroWorkflowType.MYCOBACTERIOLOGY_TB, "9", "1")).thenReturn(tbCase);
        MicroCaseOrderDetailRequestForm orderDetail = new MicroCaseOrderDetailRequestForm();
        orderDetail.cultureMethodId = "2";

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1"),
                        analysis(MicroWorkflowType.MYCOBACTERIOLOGY_TB.name(), "9")),
                "1", orderDetail);

        verify(caseService).createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "2", "1");
        verify(caseService).createOrGetCase("1001", MicroWorkflowType.MYCOBACTERIOLOGY_TB, "9", "1");
    }

    @Test
    public void routeAnalysesSkipsOrderDetailPersistenceWhenNoDetailProvided() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
        when(referenceService.getActiveCultureSetupForMethod("1", MicroWorkflowType.BACTERIOLOGY))
                .thenReturn(cultureSetup("1", MicroWorkflowType.BACTERIOLOGY));
        MicroCase routedCase = new MicroCase();
        routedCase.setId("case-1");
        when(caseService.createOrGetCase("1001", MicroWorkflowType.BACTERIOLOGY, "1", "1")).thenReturn(routedCase);

        service.routeAnalysesForSampleItem(sampleItem("1001"),
                List.of(analysis(MicroWorkflowType.BACTERIOLOGY.name(), "1")), "1", null);

        verify(orderDetailService, never()).saveOrderDetail(any(String.class),
                any(MicroCaseOrderDetailRequestForm.class), any(String.class));
    }

    @Test
    public void routeAnalysesLinksPersistedAnalysesToTheCaseAndReportMapping() {
        MicroOrderRoutingService service = new MicroOrderRoutingServiceImpl(caseService, referenceService,
                orderDetailService, caseAnalysisService, testMethodService, "");
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
