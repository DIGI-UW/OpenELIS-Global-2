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

    @ExceptionHandler(value = { RuntimeException.class })
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex, WebRequest request) {
        LogEvent.logError(ex);
        return new ResponseEntity<>("Check server logs", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = { LIMSRuntimeException.class })
    protected ResponseEntity<Object> handleLIMSRuntimeException(RuntimeException ex, WebRequest request) {
        LogEvent.logError(ex);
        return new ResponseEntity<>("Check server logs", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
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
