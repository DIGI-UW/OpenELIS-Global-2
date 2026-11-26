package org.openelisglobal.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

public class URLUtil {

    /**
     * The path within the application as the servlet container resolves it:
     * percent-encoding decoded, path parameters removed and {@code ..} segments
     * collapsed, so an authorization lookup sees the same path the controller
     * mapping does.
     */
    public static String getRequestPath(HttpServletRequest request) {
        return StringUtils.cleanPath(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
    }

    public static String getReourcePathFromRequest(HttpServletRequest request) {
        return getResourcePath(getRequestPath(request));
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
