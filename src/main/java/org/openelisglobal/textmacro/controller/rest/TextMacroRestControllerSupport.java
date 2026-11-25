package org.openelisglobal.textmacro.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.textmacro.service.TextMacroConflictException;
import org.openelisglobal.textmacro.service.TextMacroRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

abstract class TextMacroRestControllerSupport extends BaseRestController {

    @ExceptionHandler(TextMacroConflictException.class)
    protected ResponseEntity<Map<String, Object>> handleConflict(TextMacroConflictException exception) {
        return error(HttpStatus.CONFLICT, "MACRO_CODE_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(TextMacroRequestException.class)
    protected ResponseEntity<Map<String, Object>> handleInvalid(TextMacroRequestException exception) {
        String code = exception.getCode();
        HttpStatus status = "MACRO_NOT_FOUND".equals(code) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return error(status, code, exception.getMessage());
    }

    protected String authenticatedUserId(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated system user is required");
        }
        return userId;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("status", status.value(), "error", code, "message", message == null ? code : message));
    }
}
