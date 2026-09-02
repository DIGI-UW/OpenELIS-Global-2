package org.openelisglobal.config;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
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
     * more-specific handler it fell into {@link #handleRuntimeException} below and
     * every {@code @PreAuthorize} denial reaching a controller was reported as HTTP
     * 500. That also pre-empted Spring Security's {@code AccessDeniedHandler}
     * (SecurityConfig), which never saw the exception because this
     * {@code @ControllerAdvice} had already converted it into a normal response.
     *
     * <p>
     * The practical damage was much worse than a wrong status code: the frontend
     * treats a 500 on a bootstrap call as a dead backend and bounces to /login,
     * whereas a 403 is handled as "you may not see this". A Reception-only user
     * hitting the {@code PRIV_RESULT_VIEW} gate behind {@code GET /rest/menu} was
     * therefore logged out instead of simply getting a filtered menu — which is
     * what took out 52 core E2E specs in run 33568673124.
     *
     * <p>
     * Only API-shaped paths are answered here. For a page controller the
     * established behaviour is SecurityConfig's redirect to /Home?access=denied,
     * which is friendlier than a JSON body rendered into a browser window, so those
     * are rethrown and left to that handler — this advice must not silently change
     * the outcome for the ~98 ModelAndView controllers.
     */
    @ExceptionHandler(value = { org.springframework.security.access.AccessDeniedException.class })
    protected ResponseEntity<Object> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex, WebRequest request)
            throws org.springframework.security.access.AccessDeniedException {
        if (!isApiRequest(request)) {
            // Page request: let SecurityConfig's AccessDeniedHandler redirect.
            throw ex;
        }
        // Denials are expected control flow under privilege-based RBAC, so log at
        // warn without a stack trace rather than as an error.
        LogEvent.logWarn(this.getClass().getSimpleName(), "handleAccessDeniedException",
                "Access denied: " + ex.getMessage());
        return new ResponseEntity<>(buildGenericErrorBody(HttpStatus.FORBIDDEN), new HttpHeaders(),
                HttpStatus.FORBIDDEN);
    }

    /**
     * Mirrors the prefixes SecurityConfig's AccessDeniedHandler answers with JSON,
     * so the two stay consistent about what counts as an API call.
     */
    private boolean isApiRequest(WebRequest request) {
        if (!(request instanceof ServletWebRequest servletRequest)) {
            return false;
        }
        HttpServletRequest raw = servletRequest.getRequest();
        String path = raw.getRequestURI().substring(raw.getContextPath().length());
        return path.startsWith("/rest") || path.startsWith("/api") || path.startsWith("/Provider");
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
