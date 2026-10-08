package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class URLUtil {

    public static String getReourcePathFromRequest(HttpServletRequest request) {
        return getResourcePath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    /** Normalizes a path to the form {@code system_module_url} stores. */
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
