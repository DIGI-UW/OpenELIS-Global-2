package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseNonconformanceRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroCultureAnalyzerEventRestController;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.form.MicroCaseActivityRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseWorkflowChangeRequestForm;
import org.openelisglobal.microbiology.form.MicroCultureAnalyzerEventRequestForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.form.MicroLotSelectionRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseNonconformanceResult;
import org.openelisglobal.microbiology.service.MicroCaseNonconformanceService;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroCaseWorkflowService;
import org.openelisglobal.microbiology.service.MicroCultureAnalyzerEventCommand;
import org.openelisglobal.microbiology.service.MicroCultureAnalyzerEventService;
import org.openelisglobal.microbiology.service.MicroIdentificationHistoryService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseRestControllerTest {

    @Test
    public void cultureAnalyzerEventUsesTheAuthenticatedBridgeActor() {
        MicroCultureAnalyzerEventService service = org.mockito.Mockito.mock(MicroCultureAnalyzerEventService.class);
        MicroCultureAnalyzerEventRequestForm request = new MicroCultureAnalyzerEventRequestForm();
        request.externalEventId = "evt-1";
        request.eventType = "POSITIVE_SIGNAL";
        request.analyzerId = "analyzer-7";
        request.sourceId = "bottle-42";
        request.note = "Bottle positive";
        AnalyzerEvent event = new AnalyzerEvent();
        event.setStatus("APPLIED");
        when(service.receive(new MicroCultureAnalyzerEventCommand("evt-1", "POSITIVE_SIGNAL", "analyzer-7", "bottle-42",
                null, "Bottle positive"), "42")).thenReturn(event);

        ResponseEntity<?> response = new MicroCultureAnalyzerEventRestController(service).receive(request,
                requestFor("42"));

        assertEquals(202, response.getStatusCode().value());
        verify(service).receive(new MicroCultureAnalyzerEventCommand("evt-1", "POSITIVE_SIGNAL", "analyzer-7",
                "bottle-42", null, "Bottle positive"), "42");
    }

    @Test
    public void getCaseDetailReturnsCompiledServiceDto() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = requestFor("7");
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.stage = MicroCaseStage.RECEIVED.name();
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("case-1", "7", false)).thenReturn(true);
        when(service.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = controller(service, accessService, userModuleService,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class)).getCaseDetail("case-1", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("case-1", response.getBody().id);
        assertEquals(MicroCaseStage.RECEIVED.name(), response.getBody().stage);
    }

    @Test
    public void getCaseDetailReturns404WhenMissing() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = requestFor("7");
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("missing", "7", false)).thenReturn(true);

        ResponseEntity<MicroCaseDetailForm> response = controller(service, accessService, userModuleService,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class)).getCaseDetail("missing", request);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    public void getCaseDetailRejectsUserWithoutLabUnitAccessBeforeLoadingPatientData() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = requestFor("7");
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("case-1", "7", false)).thenReturn(false);

        ResponseEntity<MicroCaseDetailForm> response = controller(service, accessService, userModuleService,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class)).getCaseDetail("case-1", request);

        assertEquals(403, response.getStatusCode().value());
        verify(service, never()).getCaseDetail("case-1");
    }

    @Test
    public void recordActivityAdvancesCaseStageAndReturnsUpdatedCaseDetail() {
        MicroCaseService caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseStateService stateService = org.mockito.Mockito.mock(MicroCaseStateService.class);
        MicroCase updated = new MicroCase();
        updated.setId("case-1");
        updated.setStage(MicroCaseStage.SETUP_RECORDED.name());
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.stage = MicroCaseStage.SETUP_RECORDED.name();
        when(stateService.advanceStage(eq("case-1"), eq(MicroCaseStage.SETUP_RECORDED), eq("42"), eq("setup complete")))
                .thenReturn(updated);
        when(caseService.getCaseDetail("case-1")).thenReturn(detail);
        MicroCaseActivityRequestForm request = new MicroCaseActivityRequestForm();
        request.nextStage = MicroCaseStage.SETUP_RECORDED.name();
        request.note = "setup complete";

        ResponseEntity<MicroCaseDetailForm> response = controller(caseService,
                org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                org.mockito.Mockito.mock(UserModuleService.class), stateService,
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class))
                .recordActivity("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MicroCaseStage.SETUP_RECORDED.name(), response.getBody().stage);
    }

    @Test
    public void recordActivityRejectsMissingStageBeforeCallingTheService() {
        MicroCaseStateService stateService = org.mockito.Mockito.mock(MicroCaseStateService.class);
        MicroCaseActivityRequestForm request = new MicroCaseActivityRequestForm();

        try {
            controller(org.mockito.Mockito.mock(MicroCaseService.class),
                    org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                    org.mockito.Mockito.mock(UserModuleService.class), stateService,
                    org.mockito.Mockito.mock(MicroCaseOrderDetailService.class))
                    .recordActivity("case-1", request, requestFor("42"));
            fail("Expected a missing stage to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("nextStage is required", exception.getMessage());
        }

        verify(stateService, never()).advanceStage(eq("case-1"), org.mockito.ArgumentMatchers.any(), eq("42"),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void saveOrderDetailReturnsUpdatedCaseDetail() {
        MicroCaseService caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseStateService stateService = org.mockito.Mockito.mock(MicroCaseStateService.class);
        MicroCaseOrderDetailService orderDetailService = org.mockito.Mockito.mock(MicroCaseOrderDetailService.class);
        MicroCaseOrderDetailRequestForm request = new MicroCaseOrderDetailRequestForm();
        request.patientOrigin = "Emergency department";
        when(orderDetailService.saveOrderDetail(eq("case-1"), eq(request), eq("42")))
                .thenReturn(new MicroCaseOrderDetail());
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        when(caseService.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = controller(caseService,
                org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                org.mockito.Mockito.mock(UserModuleService.class), stateService, orderDetailService,
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class))
                .saveOrderDetail("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("case-1", response.getBody().id);
    }

    @Test
    public void createIsolateReturnsIsolateDto() {
        MicroIsolateService isolateService = org.mockito.Mockito.mock(MicroIsolateService.class);
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        isolate.setIsolateLabel("ISO-1");
        isolate.setGramStain("Gram negative rods");
        isolate.setColonyMorphology("Lactose fermenting colonies");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        when(isolateService.createIsolate(eq("case-1"), eq("ISO-1"), eq("Gram negative rods"),
                eq("Lactose fermenting colonies"), eq(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT), eq("42")))
                .thenReturn(isolate);
        MicroIsolateRequestForm request = new MicroIsolateRequestForm();
        request.caseId = "case-1";
        request.isolateLabel = "ISO-1";
        request.gramStain = "Gram negative rods";
        request.colonyMorphology = "Lactose fermenting colonies";
        request.significance = MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name();

        ResponseEntity<MicroIsolateForm> response = new MicroIsolateRestController(isolateService,
                org.mockito.Mockito.mock(MicroIdentificationHistoryService.class))
                .createIsolate(request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("iso-1", response.getBody().id);
        assertEquals("ISO-1", response.getBody().isolateLabel);
    }

    @Test
    public void requestActorCannotOverrideTheAuthenticatedActor() {
        MicroCaseService caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseStateService stateService = org.mockito.Mockito.mock(MicroCaseStateService.class);
        MicroCase updated = new MicroCase();
        updated.setId("case-1");
        updated.setStage(MicroCaseStage.SETUP_RECORDED.name());
        MicroCaseActivityRequestForm request = new MicroCaseActivityRequestForm();
        request.nextStage = MicroCaseStage.SETUP_RECORDED.name();
        request.note = "setup complete";
        when(stateService.advanceStage(eq("case-1"), eq(MicroCaseStage.SETUP_RECORDED), eq("42"), eq("setup complete")))
                .thenReturn(updated);
        when(caseService.getCaseDetail("case-1")).thenReturn(new MicroCaseDetailForm());

        controller(caseService, org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                org.mockito.Mockito.mock(UserModuleService.class), stateService,
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class))
                .recordActivity("case-1", request, requestFor("42"));

        verify(stateService).advanceStage(eq("case-1"), eq(MicroCaseStage.SETUP_RECORDED), eq("42"),
                eq("setup complete"));
    }

    @Test
    public void setupLotSelectionsUseAuthenticatedActorAndTypedSelection() {
        MicroCaseService caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseStateService stateService = org.mockito.Mockito.mock(MicroCaseStateService.class);
        MicroCaseActivityRequestForm request = new MicroCaseActivityRequestForm();
        request.nextStage = MicroCaseStage.SETUP_RECORDED.name();
        request.note = "setup with media";
        MicroLotSelectionRequestForm selection = new MicroLotSelectionRequestForm();
        selection.analysisId = "41";
        selection.testReagentLinkId = "link-1";
        selection.lotId = 7L;
        request.lotSelections.add(selection);
        when(caseService.getCaseDetail("case-1")).thenReturn(new MicroCaseDetailForm());

        controller(caseService, org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                org.mockito.Mockito.mock(UserModuleService.class), stateService,
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class))
                .recordActivity("case-1", request, requestFor("42"));

        verify(stateService).advanceStage("case-1", MicroCaseStage.SETUP_RECORDED, "42", "setup with media",
                java.util.List.of(new MicroLotSelection("41", "link-1", 7L)));
    }

    @Test
    public void changeWorkflowUsesAuthenticatedActorAndReturnsUpdatedDetail() {
        MicroCaseService caseService = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseWorkflowService workflowService = org.mockito.Mockito.mock(MicroCaseWorkflowService.class);
        MicroCaseWorkflowChangeRequestForm request = new MicroCaseWorkflowChangeRequestForm();
        request.workflowType = MicroWorkflowType.BACTERIOLOGY.name();
        request.cultureMethodId = "method-1";
        request.reason = "Correct routing";
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.workflowType = MicroWorkflowType.BACTERIOLOGY.name();
        when(caseService.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = controller(caseService,
                org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class),
                org.mockito.Mockito.mock(UserModuleService.class),
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class), workflowService)
                .changeWorkflow("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), response.getBody().workflowType);
        verify(workflowService).changeWorkflow("case-1", MicroWorkflowType.BACTERIOLOGY, "method-1", "Correct routing",
                false, "42");
    }

    @Test
    public void reportNonconformanceUsesAuthenticatedActor() {
        MicroCaseNonconformanceService service = org.mockito.Mockito.mock(MicroCaseNonconformanceService.class);
        MicroCaseNonconformanceRequestForm request = new MicroCaseNonconformanceRequestForm();
        MicroCaseNonconformanceResult result = new MicroCaseNonconformanceResult("1", "NCE-2026-00001", "FLAG_ONLY",
                "NONCONFORMANCE", java.util.List.of("case-1"));
        when(service.report("case-1", request, "42")).thenReturn(result);

        ResponseEntity<MicroCaseNonconformanceResult> response = new MicroCaseNonconformanceRestController(service)
                .report("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("NCE-2026-00001", response.getBody().nceNumber());
        verify(service).report("case-1", request, "42");
    }

    private MicroCaseRestController controller(MicroCaseService caseService,
            MicrobiologyCaseAccessService accessService, UserModuleService userModuleService,
            MicroCaseStateService stateService, MicroCaseOrderDetailService orderDetailService,
            MicroCaseWorkflowService workflowService) {
        return new MicroCaseRestController(caseService, accessService, userModuleService, stateService,
                orderDetailService, workflowService);
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
