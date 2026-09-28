package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ControllerUtills {

    public static String getSysUserId(HttpServletRequest request) {
        // Strategy 1: request-scoped data (stateless auth), then an existing OE
        // session (interactive login). Reading the actor must not create a session.
        UserSessionData usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                usd = (UserSessionData) session.getAttribute(IActionConstants.USER_SESSION_DATA);
            }
        }
        if (usd != null) {
            return String.valueOf(usd.getSystemUserId());
        }

        // Strategy 2: UserContextHolder (handles SecurityContext, daemon, and all other
        // contexts)
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
                UserSessionData usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
                if (usd == null) {
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        usd = (UserSessionData) session.getAttribute(IActionConstants.USER_SESSION_DATA);
                    }
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

    /**
     * OGC-1234: a save the server refuses for invalid input answers 400 naming the
     * fields, instead of 200 with the form, which the screens read as a success.
     */
    protected static ResponseEntity<Map<String, Object>> validationRefusal(Errors errors) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("field", fieldError.getField());
            entry.put("defaultMessage", fieldError.getDefaultMessage() == null ? "" : fieldError.getDefaultMessage());
            fieldErrors.add(entry);
        }
        List<String> globalErrors = new ArrayList<>();
        for (ObjectError globalError : errors.getGlobalErrors()) {
            globalErrors.add(globalError.getCode());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "validation");
        body.put("fieldErrors", fieldErrors);
        body.put("globalErrors", globalErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * OGC-1234: a save that failed in the service answers 409 when it collided with
     * an existing record and 500 otherwise, instead of the exception being logged
     * and the form returned with 200.
     */
    protected static ResponseEntity<Map<String, Object>> saveFailure(Exception e) {
        Map<String, Object> body = new HashMap<>();
        if (e instanceof LIMSDuplicateRecordException) {
            body.put("error", "duplicate");
            body.put("message", "A record with this name already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        }
        LogEvent.logError(e);
        body.put("error", "saveFailed");
        body.put("message", "The change was not saved.");
        return ResponseEntity.internalServerError().body(body);
    }
}
