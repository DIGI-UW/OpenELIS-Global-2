package org.openelisglobal.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;

public class SecurityFilter implements Filter {

    private static final int MAX_LOG_VALUE_LENGTH = 50;

    private static final Pattern XSS_PATTERN = Pattern.compile("(?i)" + "(" + "<\\s*script[^>]*>|</\\s*script[^>]*>"
            + "|" + "\\bon\\w+\\s*=\\s*[\"']?[^\"']*[\"']?" + "|" + "javascript\\s*:" + "|" + "data\\s*:[^,]*;base64"
            + "|" + "vbscript\\s*:" + "|" + "expression\\s*\\(" + ")");

    public SecurityFilter() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isModifyingRequest(httpRequest)) {
            String detectedXss = detectXssInParameters(httpRequest);
            if (detectedXss != null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "doFilter",
                        "Suspected XSS attack blocked - URI: "
                                + StringUtil.snipToMaxLength(httpRequest.getRequestURI(), MAX_LOG_VALUE_LENGTH)
                                + ", Method: " + httpRequest.getMethod() + ", Remote: " + httpRequest.getRemoteAddr()
                                + ", Details: " + detectedXss);
                httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request parameters");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isModifyingRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || uri.contains("Update")
                || uri.contains("Save");
    }

    private String detectXssInParameters(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null) {
                for (String value : paramValues) {
                    if (value != null && XSS_PATTERN.matcher(value).find()) {
                        return "XSS pattern in parameter '" + paramName + "': "
                                + StringUtil.snipToMaxLength(value, MAX_LOG_VALUE_LENGTH);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
