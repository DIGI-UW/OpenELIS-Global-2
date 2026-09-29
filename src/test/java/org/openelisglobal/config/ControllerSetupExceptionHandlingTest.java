package org.openelisglobal.config;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.coldstorage.controller.FreezerMonitoringExceptionHandler;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The status contract of the app-wide advice.
 *
 * <p>
 * AccessDeniedException is a RuntimeException, so before it had a handler of
 * its own the fallback answered every @PreAuthorize denial with 500 - the
 * DispatcherServlet resolves a @ControllerAdvice handler before Spring
 * Security's ExceptionTranslationFilter ever sees the exception. Callers could
 * not tell "you may not do this" from "the server broke".
 */
public class ControllerSetupExceptionHandlingTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/denied")
        public String denied() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/broken")
        public String broken() {
            throw new RuntimeException("db down");
        }

        @GetMapping("/invalid")
        public String invalid() {
            throw new LIMSRuntimeException("that name is already taken");
        }
    }

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController()).setControllerAdvice(new ControllerSetup())
                .build();
    }

    @Test
    public void accessDenied_answers403() throws Exception {
        mockMvc.perform(get("/denied")).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    public void unexpectedRuntimeException_still500() throws Exception {
        mockMvc.perform(get("/broken")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    public void limsRuntimeException_still500() throws Exception {
        mockMvc.perform(get("/invalid")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    public void limsRuntimeException_outsideColdStorage_stillAnswers500WithNoDetail() throws Exception {
        MockMvc withColdStorageAdvice = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new FreezerMonitoringExceptionHandler(), new ControllerSetup()).build();

        withColdStorageAdvice.perform(get("/invalid")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.message").doesNotExist());
    }

    /**
     * The 403 log line must say WHICH gate refused and from WHERE. The gated method
     * never runs, so it is absent from the stack; the proxy that refused it is
     * present, and the caller is the first application frame past the security
     * plumbing. Before this line existed a denied save left nothing behind but a
     * status code.
     */
    @Test
    public void describeDenial_namesTheRefusedMethodAndItsCaller() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");
        ex.setStackTrace(new StackTraceElement[] {
                new StackTraceElement(
                        "org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor",
                        "invoke", "AuthorizationManagerBeforeMethodInterceptor.java", 198),
                new StackTraceElement("jdk.proxy2.$Proxy248", "insert", null, -1),
                new StackTraceElement("org.openelisglobal.sample.action.util.SamplePatientUpdateData",
                        "resolveOrCreateSamplingSiteId", "SamplePatientUpdateData.java", 1165) });
        assertEquals("denied at insert (called from SamplePatientUpdateData.resolveOrCreateSamplingSiteId:1165)",
                ControllerSetup.describeDenial(ex));
    }

    /** Inversion: with no recognisable frames it must say so, not invent names. */
    @Test
    public void describeDenial_withoutRecognisableFrames_saysSo() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");
        ex.setStackTrace(
                new StackTraceElement[] { new StackTraceElement("java.lang.Thread", "run", "Thread.java", 1) });
        assertEquals("denied at unknown method (called from unknown)", ControllerSetup.describeDenial(ex));
    }
}
