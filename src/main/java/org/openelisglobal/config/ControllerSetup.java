package org.openelisglobal.config;

import java.net.URI;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.propertyeditor.CaseInsensitiveEnumPropertyEditor;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.AuthType;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.beans.propertyeditors.URIEditor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Leaves HIGHEST_PRECEDENCE free for a package-scoped @ControllerAdvice.
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ControllerAdvice
public class ControllerSetup extends ResponseEntityExceptionHandler {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(2048);
        StringTrimmerEditor stringTrimmer = new StringTrimmerEditor(false);
        binder.registerCustomEditor(String.class, stringTrimmer);
        binder.registerCustomEditor(URI.class, new URIEditor(false));
        binder.registerCustomEditor(AuthType.class, new CaseInsensitiveEnumPropertyEditor<>(AuthType.class));
        binder.registerCustomEditor(ProgrammedConnection.class,
                new CaseInsensitiveEnumPropertyEditor<>(ProgrammedConnection.class));
    }

    /**
     * An authorization denial is a 403, not a 500.
     *
     * <p>
     * {@code AccessDeniedException} is a {@code RuntimeException}, so without this
     * more-specific handler it falls into {@link #handleRuntimeException} below and
     * every {@code @PreAuthorize} denial is reported as HTTP 500. The practical
     * damage is worse than a wrong status code: the frontend treats a 500 on a
     * bootstrap call as a dead backend and bounces to /login, whereas a 403 is
     * handled as "you may not see this". A Reception-only user hitting the
     * PRIV_RESULT_VIEW gate behind {@code GET /rest/menu} was therefore logged out
     * instead of getting a filtered menu — that took out 52 core E2E specs in run
     * 33568673124.
     *
     * <p>
     * Answers every path, not just /rest. Under service-layer authorization the
     * denial is raised inside the controller method, so it reaches this advice via
     * the DispatcherServlet and never reaches Spring Security's
     * ExceptionTranslationFilter — SecurityConfig's /Home?access=denied redirect
     * only ever fires for filter-chain denials (unauthenticated, CSRF). Scoping
     * this handler to API prefixes therefore protects a redirect that cannot
     * happen, while leaving page-request denials as raw 500s. None of the 94
     * ModelAndView controllers carries @PreAuthorize; their gates live in the
     * services they call.
     */
    @ExceptionHandler(value = { AccessDeniedException.class })
    protected ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        // One line, no stack trace: which gate refused and from where. A denial is
        // expected control flow under privilege-based RBAC, but an unexplained 403
        // on a save is not, and until this line the only trace of one was the
        // status code. The exception's own message is "Access Denied", which names
        // nothing.
        LogEvent.logInfo(this.getClass().getName(), "handleAccessDeniedException",
                "403 " + request.getDescription(false) + ": " + describeDenial(ex));
        return new ResponseEntity<>(buildGenericErrorBody(HttpStatus.FORBIDDEN), new HttpHeaders(),
                HttpStatus.FORBIDDEN);
    }

    /**
     * "denied at insert (called from
     * SamplePatientUpdateData.resolveOrCreateSamplingSiteId:1165)". The gated
     * method never runs, so it is not on the stack; the proxy that refused it is,
     * and its method name is the one that matters. The caller is the first
     * application frame outside the security plumbing.
     */
    static String describeDenial(AccessDeniedException ex) {
        String deniedMethod = null;
        String calledFrom = null;
        for (StackTraceElement frame : ex.getStackTrace()) {
            String cls = frame.getClassName();
            boolean proxy = cls.contains("$Proxy") || cls.contains("$$SpringCGLIB$$");
            if (deniedMethod == null && proxy) {
                deniedMethod = frame.getMethodName();
            } else if (calledFrom == null && !proxy && cls.startsWith("org.openelisglobal")
                    && !cls.contains(".security.") && !cls.contains(".config.")) {
                calledFrom = cls.substring(cls.lastIndexOf('.') + 1) + "." + frame.getMethodName() + ":"
                        + frame.getLineNumber();
            }
            if (deniedMethod != null && calledFrom != null) {
                break;
            }
        }
        return "denied at " + (deniedMethod == null ? "unknown method" : deniedMethod) + " (called from "
                + (calledFrom == null ? "unknown" : calledFrom) + ")";
    }

    /**
     * Rejected input, not a server fault. Without this, the broad RuntimeException
     * handler below reports a caller's bad request as a 500 — which tells the UI
     * nothing actionable and looks like an outage in the logs. The message is
     * echoed back because these are validation failures raised deliberately by
     * services (a duplicate role name, an unknown parent role, an uneditable role),
     * never internal detail.
     */
    @ExceptionHandler(value = { IllegalArgumentException.class })
    protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        LogEvent.logDebug(this.getClass().getName(), "handleIllegalArgumentException", ex.getMessage());
        Map<String, Object> body = buildGenericErrorBody(HttpStatus.BAD_REQUEST);
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = { RuntimeException.class })
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        LogEvent.logError(ex);
        return new ResponseEntity<>(buildGenericErrorBody(HttpStatus.INTERNAL_SERVER_ERROR), new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { LIMSRuntimeException.class })
    protected ResponseEntity<Object> handleLIMSRuntimeException(RuntimeException ex, WebRequest request) {
        LogEvent.logError(ex);
        return new ResponseEntity<>(buildGenericErrorBody(HttpStatus.INTERNAL_SERVER_ERROR), new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> buildGenericErrorBody(HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LogEvent.logError(ex);
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LogEvent.logError(ex);
        return super.handleMissingServletRequestParameter(ex, headers, status, request);
    }

    // error handle for @Valid
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        // Log validation errors for debugging
        LogEvent.logWarn(this.getClass().getName(), "handleMethodArgumentNotValid",
                "Validation failed for " + ex.getObjectName() + ": " + ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", new Date());
        body.put("status", status.value());

        // Get all errors
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

        // Get all errors
        List<String> globalErrors = ex.getBindingResult().getGlobalErrors().stream().map(x -> x.getDefaultMessage())
                .collect(Collectors.toList());
        if (!errors.isEmpty()) {
            body.put("errors", errors);
        }
        if (!globalErrors.isEmpty()) {
            body.put("globalErrors", globalErrors);
        }

        return new ResponseEntity<>(body, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        LogEvent.logError(ex);
        return super.handleHttpMediaTypeNotSupported(ex, headers, status, request);
    }
}
