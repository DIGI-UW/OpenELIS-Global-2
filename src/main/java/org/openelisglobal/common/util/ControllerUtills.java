package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.spring.util.SpringContext;
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

        // Strategy 2: UserContextHolder (handles SecurityContext, daemon, and all other
        // contexts — with cached DB lookups)
        try {
            UserContextHolder holder = SpringContext.getBean(UserContextHolder.class);
            return holder.getCurrentSysUserId();
        } catch (IllegalStateException | org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            // Spring context not yet initialized (early startup)
        }

        return null;
    }

    /**
     * Get the current user's system user ID without requiring an
     * HttpServletRequest. Works in HTTP, scheduled, async, and daemon contexts.
     */
    public static String getSysUserId() {
        // Try session-based resolution first if we're in a web context
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null) {
                UserSessionData usd = (UserSessionData) request.getSession()
                        .getAttribute(IActionConstants.USER_SESSION_DATA);
                if (usd == null) {
                    usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
                }
                if (usd != null) {
                    return String.valueOf(usd.getSystemUserId());
                }
            }
        }

        // Fall back to UserContextHolder (works in all contexts)
        try {
            UserContextHolder holder = SpringContext.getBean(UserContextHolder.class);
            return holder.getCurrentSysUserId();
        } catch (IllegalStateException | org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            // Spring context not yet initialized (early startup)
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
