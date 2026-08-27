package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroAstRestControllerTest {

    @Test
    public void astMutationEndpointsRequireResultEntryRole() throws Exception {
        assertMutationRole("startRun", MicroAstRunRequestForm.class, jakarta.servlet.http.HttpServletRequest.class);
        assertMutationRole("recordReading", String.class, MicroAstReadingRequestForm.class,
                jakarta.servlet.http.HttpServletRequest.class);
        assertMutationRole("reviewRun", String.class, MicroAstRunRequestForm.class,
                jakarta.servlet.http.HttpServletRequest.class);
        assertSupervisorRole("overrideReading", String.class, MicroAstOverrideRequestForm.class,
                jakarta.servlet.http.HttpServletRequest.class);
        assertSupervisorRole("revertOverride", String.class, MicroAstOverrideRequestForm.class,
                jakarta.servlet.http.HttpServletRequest.class);
    }

    @Test
    public void recordReadingDerivesMeasurementInTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstReadingRequestForm request = new MicroAstReadingRequestForm();
        request.antibioticId = "abx-1";
        request.rawValue = new BigDecimal("4");
        MicroAstReading reading = new MicroAstReading();
        reading.setAstRunId("run-1");
        when(service.recordReading("run-1", "abx-1", new BigDecimal("4"), "42")).thenReturn(reading);

        new MicroAstRestController(service).recordReading("run-1", request, requestFor("42"));

        verify(service).recordReading("run-1", "abx-1", new BigDecimal("4"), "42");
        verify(service, never()).recordReading(any(), any(), any(), any(), any());
    }

    @Test
    public void startRunPassesTechniqueAndOrderedDrugSetToTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstRunRequestForm request = new MicroAstRunRequestForm();
        request.isolateId = "iso-1";
        request.panelId = "panel-1";
        request.breakpointStandardId = "std-1";
        request.panelAdjustmentReason = "additional resistance screen";
        request.technique = "DISK_DIFFUSION";
        request.orderedAntibioticIds = List.of("abx-2", "abx-1");
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        when(service.startRun("iso-1", "panel-1", "std-1", "additional resistance screen",
                MicroAstTechnique.DISK_DIFFUSION, List.of("abx-2", "abx-1"), "42")).thenReturn(run);
        when(service.getOrderedAntibioticsForRun("run-1")).thenReturn(List.of());

        new MicroAstRestController(service).startRun(request, requestFor("42"));

        verify(service).startRun("iso-1", "panel-1", "std-1", "additional resistance screen",
                MicroAstTechnique.DISK_DIFFUSION, List.of("abx-2", "abx-1"), "42");
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

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    private void assertMutationRole(String methodName, Class<?>... parameterTypes) throws Exception {
        PreAuthorize authorization = MicroAstRestController.class.getMethod(methodName, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')", authorization.value());
    }

    private void assertSupervisorRole(String methodName, Class<?>... parameterTypes) throws Exception {
        PreAuthorize authorization = MicroAstRestController.class.getMethod(methodName, parameterTypes)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasAnyRole('ADMIN', 'VALIDATION')", authorization.value());
    }
}
