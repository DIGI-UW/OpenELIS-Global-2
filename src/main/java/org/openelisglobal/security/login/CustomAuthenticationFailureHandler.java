package org.openelisglobal.security.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.security.LoginAttemptTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Autowired
    private LoginAttemptTracker loginAttemptTracker;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        String clientAddress = loginAttemptTracker.clientAddress(request);
        if (!(exception instanceof LoginThrottledException)) {
            loginAttemptTracker.recordFailure(request.getParameter("loginName"), clientAddress);
        }
        LogEvent.logInfo(this.getClass().getSimpleName(), "onFailure",
                "Unsuccessful login attempt from " + clientAddress);
        if ("true".equals(request.getParameter("apiCall"))) {
            this.handleApiLogin(request, response, exception);
            return;
        } else {
            BaseErrors errors = new BaseErrors();
            if (exception instanceof UsernameNotFoundException) {
                errors.reject("login.error.message");
            } else if (exception instanceof BadCredentialsException) {
                errors.reject("login.error.message");
            } else if (exception instanceof CredentialsExpiredException) {
                errors.reject("login.error.password.expired");
            } else if (exception instanceof DisabledException) {
                errors.reject("login.error.account.disable");
            } else if (exception instanceof LockedException) {
                errors.reject("login.error.account.lock");
            } else if (exception instanceof LoginThrottledException) {
                errors.reject("login.error.throttled",
                        new Object[] { ((LoginThrottledException) exception).getWindowMinutes() },
                        "login.error.throttled");
            } else {
                exception.printStackTrace();
                errors.reject("login.error.generic");
            }

            RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
            request.getSession().setAttribute(Constants.LOGIN_ERRORS, errors);
            redirectStrategy.sendRedirect(request, response, "/LoginPage");
        }
    }

    private void handleApiLogin(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setStatus(HttpStatus.SC_UNAUTHORIZED);
        JSONObject sessionDetails = new JSONObject();

        if (exception instanceof UsernameNotFoundException) {
            sessionDetails.put("error", "error.invalidcredentials");
        } else if (exception instanceof BadCredentialsException) {
            sessionDetails.put("error", "error.invalidcredentials");
        } else if (exception instanceof CredentialsExpiredException) {
            sessionDetails.put("error", "error.expiredCredentials");
        } else if (exception instanceof DisabledException) {
            sessionDetails.put("error", "error.disabledCredentials");
        } else if (exception instanceof LockedException) {
            sessionDetails.put("error", "error.lockedCredentials");
        } else if (exception instanceof LoginThrottledException) {
            sessionDetails.put("error", "error.throttled");
        } else {
            sessionDetails.put("error", "error.generic");
        }

        out.print(sessionDetails);
    }
}
