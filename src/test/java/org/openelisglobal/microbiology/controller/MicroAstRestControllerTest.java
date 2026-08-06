package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstRunForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.form.MicroLotSelectionRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroAstRestControllerTest {

    @Test
    public void recordReadingRejectsMissingMethodBeforeCallingTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstReadingRequestForm request = new MicroAstReadingRequestForm();
        request.antibioticId = "abx-1";

        try {
            new MicroAstRestController(service).recordReading("run-1", request, requestFor("42"));
            fail("Expected a missing AST method to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("method is required", exception.getMessage());
        }

        verify(service, never()).recordReading(any(), any(), any(), any(), any());
    }

    @Test
    public void overrideReadingRejectsMissingInterpretationBeforeCallingTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstOverrideRequestForm request = new MicroAstOverrideRequestForm();
        request.overrideReason = "confirmed on repeat";

        try {
            new MicroAstRestController(service).overrideReading("reading-1", request, requestFor("42"));
            fail("Expected a missing override interpretation to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("overrideInterpretation is required", exception.getMessage());
        }

        verify(service, never()).overrideReading(any(), any(), any(), any());
    }

    @Test
    public void setupReturnsTheServerResolvedOrderedPanel() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstSetupForm setup = new MicroAstSetupForm();
        setup.isolateId = "isolate-1";
        setup.orderedPanelId = "panel-1";
        setup.orderedPanelVersion = 3;
        when(service.getSetup("isolate-1")).thenReturn(setup);

        MicroAstSetupForm response = new MicroAstRestController(service).getSetup("isolate-1").getBody();

        assertEquals("panel-1", response.orderedPanelId);
        assertEquals(Integer.valueOf(3), response.orderedPanelVersion);
    }

    @Test
    public void repeatAndSelectionUseTheAuthenticatedActor() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        repeat.setSourceRunId("run-1");
        repeat.setAttemptType(MicroAstAttemptType.REPEAT.name());
        when(service.startRepeatRun("run-1", MicroAstAttemptType.REPEAT, "Control failed", MicroAstMethod.MIC, "42"))
                .thenReturn(repeat);
        when(service.selectReportableRun("run-2", "42")).thenReturn(repeat);
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.attemptType = MicroAstAttemptType.REPEAT.name();
        request.reason = "Control failed";
        request.method = MicroAstMethod.MIC.name();
        MicroAstRestController controller = new MicroAstRestController(service);

        ResponseEntity<MicroAstRunForm> created = controller.startRepeatRun("run-1", request, requestFor("42"));
        controller.selectReportableRun("run-2", requestFor("42"));

        assertEquals("run-1", created.getBody().sourceRunId);
        assertEquals(MicroAstAttemptType.REPEAT.name(), created.getBody().attemptType);
        verify(service).startRepeatRun("run-1", MicroAstAttemptType.REPEAT, "Control failed", MicroAstMethod.MIC, "42");
        verify(service).selectReportableRun("run-2", "42");
    }

    @Test
    public void repeatAndSelectionRequireAuthentication() throws Exception {
        PreAuthorize repeat = MicroAstRestController.class.getMethod("startRepeatRun", String.class,
                MicroAstRunRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize selection = MicroAstRestController.class
                .getMethod("selectReportableRun", String.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("isAuthenticated()", repeat.value());
        assertEquals("isAuthenticated()", selection.value());
    }

    @Test
    public void astSetupPassesLotSelectionsWithAuthenticatedActor() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.isolateId = "isolate-1";
        request.panelId = "panel-1";
        request.breakpointStandardId = "standard-1";
        request.panelAdjustmentReason = "Urine-specific panel required";
        MicroLotSelectionRequestForm selection = new MicroLotSelectionRequestForm();
        selection.analysisId = "41";
        selection.testReagentLinkId = "link-1";
        selection.lotId = 7L;
        request.lotSelections.add(selection);
        when(service.startRun("isolate-1", "panel-1", "standard-1", "Urine-specific panel required",
                java.util.List.of(new MicroLotSelection("41", "link-1", 7L)), "42")).thenReturn(run);

        new MicroAstRestController(service).startRun(request, requestFor("42"));

        verify(service).startRun("isolate-1", "panel-1", "standard-1", "Urine-specific panel required",
                java.util.List.of(new MicroLotSelection("41", "link-1", 7L)), "42");
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
