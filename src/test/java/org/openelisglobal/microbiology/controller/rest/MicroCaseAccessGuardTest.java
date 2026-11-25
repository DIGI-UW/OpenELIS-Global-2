package org.openelisglobal.microbiology.controller.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class MicroCaseAccessGuardTest {
    private MicrobiologyCaseAccessService access;
    private MicroCaseAccessGuard guard;
    private MockHttpServletRequest request;
    private MicroCaseResourceAccessService resources;

    @Before
    public void setup() {
        access = mock(MicrobiologyCaseAccessService.class);
        var cases = mock(MicroCaseDAO.class);
        var isolates = mock(MicroIsolateDAO.class);
        var runs = mock(MicroAstRunDAO.class);
        var readings = mock(MicroAstReadingDAO.class);
        var c = new MicroCase();
        c.setLabUnitId("unit");
        var isolate = new MicroIsolate();
        isolate.setCaseId("case");
        var run = new MicroAstRun();
        run.setIsolateId("isolate");
        var reading = new MicroAstReading();
        reading.setAstRunId("run");
        when(cases.get("case")).thenReturn(Optional.of(c));
        when(isolates.get("isolate")).thenReturn(Optional.of(isolate));
        when(runs.get("run")).thenReturn(Optional.of(run));
        when(readings.get("reading")).thenReturn(Optional.of(reading));
        resources = new MicroCaseResourceAccessServiceImpl(access, cases, isolates, runs, readings,
                mock(MicroCriticalCommunicationDAO.class));
        guard = new MicroCaseAccessGuard(resources);
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        var actor = new UserSessionData();
        actor.setSytemUserId(7);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, actor);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @After
    public void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void check(Method method, Object... args) {
        var signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        var invocation = mock(JoinPoint.class);
        when(invocation.getSignature()).thenReturn(signature);
        when(invocation.getArgs()).thenReturn(args);
        guard.check(invocation);
    }

    @Test
    public void reviewAndReportableRequireUnitValidationEvenForResultsUsers() throws Exception {
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_RESULTS)).thenReturn(true);
        Method review = MicroAstRestController.class.getMethod("reviewRun", String.class,
                MicroAstRunRequestForm.class, jakarta.servlet.http.HttpServletRequest.class);
        Method reportable = MicroAstRestController.class.getMethod("selectReportableRun", String.class,
                jakarta.servlet.http.HttpServletRequest.class);
        assertThrows(AccessDeniedException.class, () -> check(review, "run", new MicroAstRunRequestForm(), request));
        assertThrows(AccessDeniedException.class, () -> check(reportable, "run", request));
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_RESULTS)).thenReturn(false);
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_VALIDATION)).thenReturn(true);
        check(review, "run", new MicroAstRunRequestForm(), request);
        var scope = (MicroCaseWriteAccessGuard.WriteScope) request.getAttribute(MicroCaseWriteAccessGuard.REQUEST_SCOPE);
        assertEquals(Constants.ROLE_VALIDATION, scope.role());
        check(reportable, "run", request);
    }

    @Test
    public void reidentificationRequiresValidationInTheCurrentCaseUnit() throws Exception {
        Method update = MicroIsolateRestController.class.getMethod("validate", String.class, String.class,
                MicroIsolateWorkspaceRequestForm.class, jakarta.servlet.http.HttpServletRequest.class);
        var form = new MicroIsolateWorkspaceRequestForm();
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_RESULTS)).thenReturn(true);
        assertThrows(AccessDeniedException.class, () -> check(update, "case", "isolate", form, request));
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_RESULTS)).thenReturn(false);
        when(access.hasLabUnitRole("7", "unit", Constants.ROLE_VALIDATION)).thenReturn(true);
        check(update, "case", "isolate", form, request);
        var scope = (MicroCaseWriteAccessGuard.WriteScope) request
                .getAttribute(MicroCaseWriteAccessGuard.REQUEST_SCOPE);
        assertEquals(Constants.ROLE_VALIDATION, scope.role());
    }

    @Test
    public void childResolutionPreservesReadOnlyDirectAccessAcrossUnits() {
        assertEquals("case", resources.resolveCaseId("readingId", "reading"));
        resources.requireAccess("case", "7", Constants.ROLE_RESULTS, true);
        assertThrows(AccessDeniedException.class,
                () -> resources.requireAccess("case", "7", Constants.ROLE_RESULTS, false));
    }
}
