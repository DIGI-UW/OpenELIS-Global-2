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
import java.util.ArrayList;
import java.util.Enumeration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;

public class SecurityFilter implements Filter {

    public SecurityFilter() {
    }

    @Override
    public void destroy() {
        // Nothing to clean up
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean suspectedAttack = false;
        ArrayList<String> attackList = new ArrayList<>();

        // Persistent XSS check on POST or sensitive URIs
        if ("POST".equalsIgnoreCase(httpRequest.getMethod())
                || httpRequest.getRequestURI().contains("Update")
                || httpRequest.getRequestURI().contains("Save")) {

            Enumeration<String> parameterNames = httpRequest.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String curParam = parameterNames.nextElement();
                String paramValue = httpRequest.getParameter(curParam);

                if (paramValue != null && paramValue.toLowerCase().contains("<script")) {
                    suspectedAttack = true;
                    attackList.add("XSS on " + curParam + ": " + StringUtil.snipToMaxLength(paramValue, 50));
                }
            }
        }

        if (suspectedAttack) {
            StringBuilder attackMessage = new StringBuilder();
            String separator = "";
            attackMessage.append(StringUtil.snipToMaxLength(httpRequest.getRequestURI(), 50));
            attackMessage.append(" suspected attack(s) of type: ");
            for (String attack : attackList) {
                attackMessage.append(separator);
                separator = ",";
                attackMessage.append(attack);
            }

            // Log suspected attempt
            LogEvent.logWarn(this.getClass().getSimpleName(), "doFilter()", attackMessage.toString());

            // Respond with error instead of silent redirect
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Suspected XSS attack detected");
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // No exceptions list needed anymore
    }
}
