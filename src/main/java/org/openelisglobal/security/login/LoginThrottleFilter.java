package org.openelisglobal.security.login;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.security.LoginAttemptTracker;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers a throttled form sign-in before the password is checked, through the
 * same failure handler as a wrong password.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/ValidateLogin";

    private final LoginAttemptTracker tracker;
    private final AuthenticationFailureHandler failureHandler;

    public LoginThrottleFilter(LoginAttemptTracker tracker, AuthenticationFailureHandler failureHandler) {
        this.tracker = tracker;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (isLoginAttempt(request)
                && tracker.isThrottled(request.getParameter("loginName"), LoginAttemptTracker.clientAddress(request))) {
            failureHandler.onAuthenticationFailure(request, response,
                    new LoginThrottledException(tracker.windowMinutes()));
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isLoginAttempt(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(path);
    }
}
