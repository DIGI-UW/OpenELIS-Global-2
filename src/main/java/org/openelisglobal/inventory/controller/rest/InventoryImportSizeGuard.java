package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Turns an oversized catalogue upload away before its body is read.
 *
 * <p>
 * {@code InventoryImportRestController} also checks the size, but it can only
 * do so once {@code @RequestBody} has deserialised the whole request into a
 * string — by which point the megabytes the check exists to refuse are already
 * held in memory. An interceptor runs before argument resolution, so this is
 * the first point in the request where refusing costs nothing.
 *
 * <p>
 * The bound here is deliberately coarse: it is a byte count against a character
 * limit, and one character of CSV can reach six bytes once JSON escaping is
 * applied, so anything the controller would accept passes here untouched. The
 * exact limit stays where it can be applied exactly.
 *
 * <p>
 * A request that declares no {@code Content-Length} — a chunked upload — is
 * passed through, because there is nothing to compare. Those still meet the
 * controller's check, one buffer later.
 */
@Component
public class InventoryImportSizeGuard implements HandlerInterceptor, WebMvcConfigurer {

    /** The URL space this guard owns. */
    public static final String GUARDED_PATH_PATTERN = "/rest/inventory/import/**";

    /**
     * Six bytes per allowed character, the widest a character can be once escaped
     * into a JSON string, plus room for the envelope around it.
     */
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
