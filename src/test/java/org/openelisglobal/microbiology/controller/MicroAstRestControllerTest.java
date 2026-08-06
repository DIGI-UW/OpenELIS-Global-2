package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingForm;
import org.openelisglobal.microbiology.form.MicroAstRunForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.form.MicroLotSelectionRequestForm;
import org.openelisglobal.microbiology.service.MicroAstRunSetupCommand;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroLotSelection;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroAstRestControllerTest {

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
    public void panelAntibioticsExposeTheConfiguredOrderForAdjustment() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic ordered = new org.openelisglobal.microbiology.valueholder.MicroAstPanelAntibiotic();
        ordered.setAntibioticId("abx-1");
        ordered.setDisplayOrder(2);
        ordered.setTier(1);
        ordered.setReportBehavior("ALWAYS");
        when(service.getPanelAntibiotics("panel-1")).thenReturn(java.util.List.of(ordered));

        org.openelisglobal.microbiology.form.MicroAstRunAntibioticForm response = new MicroAstRestController(service)
                .getPanelAntibiotics("panel-1").getBody().get(0);

        assertEquals("abx-1", response.antibioticId);
        assertEquals(Integer.valueOf(2), response.displayOrder);
        assertEquals(Integer.valueOf(1), response.tier);
        assertEquals("ALWAYS", response.reportBehavior);
    }

    @Test
    public void repeatAndSelectionUseTheAuthenticatedActor() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        repeat.setSourceRunId("run-1");
        repeat.setAttemptType(MicroAstAttemptType.REPEAT.name());
        when(service.startRepeatRun("run-1", MicroAstAttemptType.REPEAT, "Control failed", MicroAstTechnique.VITEK_2,
                "42")).thenReturn(repeat);
        when(service.selectReportableRun("run-2", "42")).thenReturn(repeat);
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.attemptType = MicroAstAttemptType.REPEAT.name();
        request.reason = "Control failed";
        request.technique = MicroAstTechnique.VITEK_2.name();
        MicroAstRestController controller = new MicroAstRestController(service);

        ResponseEntity<MicroAstRunForm> created = controller.startRepeatRun("run-1", request, requestFor("42"));
        controller.selectReportableRun("run-2", new MicroAstRunRequestForm(), requestFor("42"));

        assertEquals("run-1", created.getBody().sourceRunId);
        assertEquals(MicroAstAttemptType.REPEAT.name(), created.getBody().attemptType);
        verify(service).startRepeatRun("run-1", MicroAstAttemptType.REPEAT, "Control failed", MicroAstTechnique.VITEK_2,
                "42");
        verify(service).selectReportableRun("run-2", "42");
    }

    @Test
    public void repeatPassesTheRequestedDrugScopeWithTheAuthenticatedActor() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRun repeat = new MicroAstRun();
        repeat.setId("run-2");
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.attemptType = MicroAstAttemptType.RETEST.name();
        request.reason = "Confirm carbapenem";
        request.technique = MicroAstTechnique.VITEK_2.name();
        request.orderedAntibioticIds = java.util.List.of("abx-2");
        when(service.startRepeatRun("run-1", MicroAstAttemptType.RETEST, "Confirm carbapenem",
                MicroAstTechnique.VITEK_2, java.util.List.of(), java.util.List.of("abx-2"), "42")).thenReturn(repeat);

        new MicroAstRestController(service).startRepeatRun("run-1", request, requestFor("42"));

        verify(service).startRepeatRun("run-1", MicroAstAttemptType.RETEST, "Confirm carbapenem",
                MicroAstTechnique.VITEK_2, java.util.List.of(), java.util.List.of("abx-2"), "42");
    }

    @Test
    public void repeatAndSelectionRequireAuthentication() throws Exception {
        PreAuthorize repeat = MicroAstRestController.class.getMethod("startRepeatRun", String.class,
                MicroAstRunRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize selection = MicroAstRestController.class.getMethod("selectReportableRun", String.class,
                MicroAstRunRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("isAuthenticated()", repeat.value());
        assertEquals("isAuthenticated()", selection.value());
    }

    @Test
    public void astSetupPassesLotSelectionsWithAuthenticatedActor() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        MicroAstRunAntibiotic ordered = new MicroAstRunAntibiotic();
        ordered.setAstRunId("run-1");
        ordered.setAntibioticId("abx-1");
        ordered.setDisplayOrder(1);
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.isolateId = "isolate-1";
        request.panelId = "panel-1";
        request.breakpointStandardId = "standard-1";
        request.panelAdjustmentReason = "Urine-specific panel required";
        request.technique = MicroAstTechnique.DISK_DIFFUSION.name();
        request.orderedAntibioticIds = java.util.List.of("abx-1");
        MicroLotSelectionRequestForm selection = new MicroLotSelectionRequestForm();
        selection.analysisId = "41";
        selection.testReagentLinkId = "link-1";
        selection.lotId = 7L;
        request.lotSelections.add(selection);
        MicroAstRunSetupCommand command = new MicroAstRunSetupCommand("isolate-1", "panel-1", "standard-1",
                "Urine-specific panel required", MicroAstTechnique.DISK_DIFFUSION,
                java.util.List.of(new MicroLotSelection("41", "link-1", 7L)), java.util.List.of("abx-1"), false, null,
                null);
        when(service.startRun(command, "42")).thenReturn(run);
        when(service.getOrderedAntibioticsForRun("run-1")).thenReturn(java.util.List.of(ordered));

        MicroAstRunForm response = new MicroAstRestController(service).startRun(request, requestFor("42")).getBody();

        verify(service).startRun(command, "42");
        assertEquals("abx-1", response.orderedAntibiotics.get(0).antibioticId);
    }

    @Test
    public void readingDerivesMeasurementFromTheRunInsteadOfTheRequest() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstReading reading = new MicroAstReading();
        reading.setId("reading-1");
        reading.setMethod("MIC");
        org.openelisglobal.microbiology.form.MicroAstReadingRequestForm request = new org.openelisglobal.microbiology.form.MicroAstReadingRequestForm();
        request.antibioticId = "abx-1";
        request.rawValue = new java.math.BigDecimal("4");
        when(service.recordReading("run-1", "abx-1", new java.math.BigDecimal("4"), "42")).thenReturn(reading);

        MicroAstReadingForm response = new MicroAstRestController(service)
                .recordReading("run-1", request, requestFor("42")).getBody();

        verify(service).recordReading("run-1", "abx-1", new java.math.BigDecimal("4"), "42");
        assertEquals("MIC", response.measurementType);
        assertEquals("MIC", response.method);
    }

    @Test
    public void overrideAndRevertUseTheAuthenticatedActorAndSupervisorBundle() throws Exception {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstOverrideRequestForm override = new MicroAstOverrideRequestForm();
        override.overrideInterpretation = "RESISTANT";
        override.overrideReason = "Clinical exception";
        MicroAstOverrideRequestForm revert = new MicroAstOverrideRequestForm();
        revert.overrideReason = "Repeat confirmed original";
        MicroAstReading reading = new MicroAstReading();
        reading.setId("reading-1");
        reading.setAstRunId("run-1");
        when(service.overrideReading("reading-1",
                org.openelisglobal.microbiology.valueholder.MicroAstInterpretation.RESISTANT, "Clinical exception",
                "42")).thenReturn(reading);
        when(service.revertOverride("reading-1", "Repeat confirmed original", "84")).thenReturn(reading);
        when(service.getOverrideHistoryForRun("run-1")).thenReturn(java.util.List.of());
        MicroAstRestController controller = new MicroAstRestController(service);

        controller.overrideReading("reading-1", override, requestFor("42"));
        controller.revertOverride("reading-1", revert, requestFor("84"));

        verify(service).overrideReading("reading-1",
                org.openelisglobal.microbiology.valueholder.MicroAstInterpretation.RESISTANT, "Clinical exception",
                "42");
        verify(service).revertOverride("reading-1", "Repeat confirmed original", "84");
        PreAuthorize overrideGuard = MicroAstRestController.class.getMethod("overrideReading", String.class,
                MicroAstOverrideRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize revertGuard = MicroAstRestController.class.getMethod("revertOverride", String.class,
                MicroAstOverrideRequestForm.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasAnyRole('ADMIN', 'VALIDATION')", overrideGuard.value());
        assertEquals("hasAnyRole('ADMIN', 'VALIDATION')", revertGuard.value());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
