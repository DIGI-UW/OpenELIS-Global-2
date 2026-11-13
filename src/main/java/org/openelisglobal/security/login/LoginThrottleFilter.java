package org.openelisglobal.security.login;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.security.LoginAttemptTracker;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Answers a throttled form sign-in before the password is checked, through the
 * same failure handler as a wrong password.
 */
public class LoginThrottleFilter extends OncePerRequestFilter {

    /**
     * The same decoded matching the form login filter uses, so an encoded path
     * cannot slip past.
     */
    private static final RequestMatcher LOGIN_REQUEST = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.POST, "/ValidateLogin");

    private final LoginAttemptTracker tracker;
    private final AuthenticationFailureHandler failureHandler;

    public LoginThrottleFilter(LoginAttemptTracker tracker, AuthenticationFailureHandler failureHandler) {
        this.tracker = tracker;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LOGIN_REQUEST.matches(request)) {
            chain.doFilter(request, response);
            return;
        }
        try (LoginAttemptTracker.Attempt attempt = tracker.tryAcquire(request.getParameter("loginName"),
                tracker.clientAddress(request))) {
            if (attempt == null) {
                failureHandler.onAuthenticationFailure(request, response,
                        new LoginThrottledException(tracker.windowMinutes()));
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
