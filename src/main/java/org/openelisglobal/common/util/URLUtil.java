package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class URLUtil {

    public static String getReourcePathFromRequest(HttpServletRequest request) {

        String pathAndQuery = request.getRequestURI().substring(request.getContextPath().length());
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
