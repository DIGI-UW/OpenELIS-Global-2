package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.controller.rest.MicroIsolateRestController;
import org.openelisglobal.microbiology.form.MicroCaseActivityRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseOrderDetail;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseRestControllerTest {

    @Test
    public void getCaseDetailReturnsCompiledServiceDto() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.stage = MicroCaseStage.RECEIVED.name();
        when(service.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(service,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class)).getCaseDetail("case-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("case-1", response.getBody().id);
        assertEquals(MicroCaseStage.RECEIVED.name(), response.getBody().stage);
    }

    @Test
    public void getCaseDetailReturns404WhenMissing() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(service,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class)).getCaseDetail("missing");

        assertEquals(404, response.getStatusCode().value());
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

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(caseService, stateService,
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class))
                .recordActivity("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MicroCaseStage.SETUP_RECORDED.name(), response.getBody().stage);
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

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(caseService, stateService,
                orderDetailService).saveOrderDetail("case-1", request, requestFor("42"));

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
        isolate.setPreliminaryOrganismText("Escherichia coli");
        isolate.setSignificance(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name());
        when(isolateService.createIsolate(eq("case-1"), eq("ISO-1"), eq(null), eq("Escherichia coli"),
                eq(MicroIsolateSignificance.CLINICALLY_SIGNIFICANT), eq("42"))).thenReturn(isolate);
        MicroIsolateRequestForm request = new MicroIsolateRequestForm();
        request.caseId = "case-1";
        request.isolateLabel = "ISO-1";
        request.preliminaryOrganismText = "Escherichia coli";
        request.significance = MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name();

        ResponseEntity<MicroIsolateForm> response = new MicroIsolateRestController(isolateService)
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

        new MicroCaseRestController(caseService, stateService,
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class))
                .recordActivity("case-1", request, requestFor("42"));

        verify(stateService).advanceStage(eq("case-1"), eq(MicroCaseStage.SETUP_RECORDED), eq("42"),
                eq("setup complete"));
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
