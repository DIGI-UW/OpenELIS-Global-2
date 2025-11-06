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

    private ArrayList<String> exceptions = new ArrayList<>();

    public SecurityFilter() {
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean suspectedAttack = false;
        ArrayList<String> attackList = new ArrayList<>();

        // persistent XSS check
        if (httpRequest.getMethod().equals("POST") || httpRequest.getRequestURI().contains("Update")
                || httpRequest.getRequestURI().contains("Save")) {
            Enumeration<String> parameterNames = httpRequest.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String curParam = parameterNames.nextElement();
                String paramValue = httpRequest.getParameter(curParam);
                // String paramValue = java.net.URLDecoder.decode(param, "UTF-8");

                paramValue = paramValue.replaceAll("\\s", "");
                if (paramValue.contains("<script>") || paramValue.contains("</script>")) {
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
            // should log suspected attempt
            LogEvent.logWarn(this.getClass().getSimpleName(), "doFilter()", attackMessage.toString());
            // send to safe page
            httpResponse.sendRedirect("Dashboard");
        } else {
            chain.doFilter(request, httpResponse);
        }
    }

    public void addException(String exception) {
        exceptions.add(exception);
    }

    private void addExceptions() {
        exceptions.add("importAnalyzer");
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        // TODO Auto-generated method stub
        addExceptions();
    }
}
