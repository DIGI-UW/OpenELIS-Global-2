package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** A chunked request (no Content-Length) passes; the controller checks it. */
@Component
public class InventoryImportSizeGuard implements HandlerInterceptor, WebMvcConfigurer {

    public static final String GUARDED_PATH_PATTERN = "/rest/inventory/import/**";

    /** Six bytes per character, the widest once JSON-escaped, plus the envelope. */
    static final long MAX_BYTES = 6L * InventoryImportRestController.MAX_CHARACTERS + 1024L;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns(GUARDED_PATH_PATTERN);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getContentLengthLong() <= MAX_BYTES) {
            return true;
        }
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        return false;
    }
}
