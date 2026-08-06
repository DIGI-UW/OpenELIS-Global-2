package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class URLUtil {

    public static String getReourcePathFromRequest(HttpServletRequest request) {
        return getResourcePath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    /**
     * Normalizes a raw path (optionally carrying a query string) to the form stored
     * in {@code system_module_url.url_path}. Extracted verbatim from
     * {@link #getReourcePathFromRequest(HttpServletRequest)} so the menu privilege
     * filter resolves paths exactly as the interceptor does — divergent
     * normalization would let the menu advertise a link the interceptor rejects.
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
