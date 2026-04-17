package org.openelisglobal.interceptor;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.Errors;

/**
 * T017 — Verifies the auto-allow gap in ModuleAuthenticationInterceptor is
 * closed: a REST endpoint with no module mapping must return HTTP 401, not be
 * auto-allowed through.
 *
 * <p>
 * Uses a subclass to override {@code hasPermission()} — bypassing
 * {@code ConfigurationProperties.getInstance()} which requires a live Spring
 * context — so the test focuses on the denial/response behaviour of
 * {@code preHandle()} when access is refused.
 */
public class ModuleAuthenticationInterceptorTest {

    /**
     * Subclass that short-circuits hasPermission() to always deny — simulates a
     * user with no module access on an unregistered REST path.
     */
    private static class DenyingInterceptor extends ModuleAuthenticationInterceptor {
        @Override
        protected boolean hasPermission(Errors errors, HttpServletRequest request) {
            return false;
        }
    }

    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(request.getContextPath()).thenReturn("");
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
    }

    // --- denial path ---

    @Test
    public void restEndpoint_denied_returns401() throws Exception {
        when(request.getRequestURI()).thenReturn("/rest/some-unregistered-endpoint");

        boolean allowed = new DenyingInterceptor().preHandle(request, response, new Object());

        assertFalse("Unregistered REST endpoint must be denied, not auto-allowed", allowed);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void restEndpoint_denied_returnsJsonNotRedirect() throws Exception {
        when(request.getRequestURI()).thenReturn("/rest/another-endpoint");

        new DenyingInterceptor().preHandle(request, response, new Object());

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
    }

}
