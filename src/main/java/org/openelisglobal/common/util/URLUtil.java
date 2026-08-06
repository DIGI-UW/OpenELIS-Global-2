package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class URLUtil {

    public static String getReourcePathFromRequest(HttpServletRequest request) {
        return getResourcePath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    /**
     * Normalizes a raw path (optionally carrying a query string) to the form stored
     * in {@code system_module_url.url_path}.
     *
     * <p>
     * Extracted from {@link #getReourcePathFromRequest(HttpServletRequest)} so
     * callers that hold a path string rather than a request — notably the menu
     * privilege filter, which normalizes {@code menu.action_url} — resolve paths
     * identically to the interceptor. Divergent normalization would let the menu
     * advertise a link the interceptor rejects.
     *
     * <p>
     * The body is a verbatim move; the pre-existing {@code /rest} prefix handling
     * is intentionally unchanged here so the extraction cannot alter behaviour.
     */
    public static String getResourcePath(String pathAndQuery) {
        String pathWithoutQuery;
        if (pathAndQuery.contains("?")) {
            pathWithoutQuery = pathAndQuery.substring(0, pathAndQuery.indexOf('?'));
        } else {
            pathWithoutQuery = pathAndQuery;
        }
        String pathWithoutSuffix;
        if (pathWithoutQuery.contains(".do") || pathWithoutQuery.contains(".html")) {
            pathWithoutSuffix = pathWithoutQuery.substring(0, pathWithoutQuery.lastIndexOf('.'));
        } else {
            pathWithoutSuffix = pathWithoutQuery;
        }
        if (pathWithoutSuffix.startsWith("/rest")) {
            pathWithoutSuffix = pathWithoutSuffix.split("/rest")[1];
        }
        return pathWithoutSuffix;
    }
}
