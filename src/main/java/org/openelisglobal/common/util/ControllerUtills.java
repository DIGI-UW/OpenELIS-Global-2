package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ControllerUtills {

    public static String getSysUserId(HttpServletRequest request) {
        // Strategy 1: OE session attribute (set by login flow)
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        }
        if (usd != null) {
            return String.valueOf(usd.getSystemUserId());
        }

        // Strategy 2: Spring Security principal (fallback for authenticated REST calls)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                && auth.getName() != null) {
            try {
                org.openelisglobal.login.service.LoginUserService loginService = org.openelisglobal.spring.util.SpringContext
                        .getBean(org.openelisglobal.login.service.LoginUserService.class);
                org.openelisglobal.login.valueholder.LoginUser loginUser = loginService.getUserProfile(auth.getName());
                if (loginUser != null) {
                    return String.valueOf(loginUser.getSystemUserId());
                }
            } catch (Exception e) {
                org.openelisglobal.common.log.LogEvent.logDebug(ControllerUtills.class.getSimpleName(), "getSysUserId",
                        "SecurityContext fallback failed: " + e.getMessage());
            }
        }

        return null;
    }

    public static boolean isRestCall() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = null;
        if (requestAttributes instanceof ServletRequestAttributes) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (path.startsWith("/rest")) {
                    return true;
                }
            }
        }
        return false;
    }

}
