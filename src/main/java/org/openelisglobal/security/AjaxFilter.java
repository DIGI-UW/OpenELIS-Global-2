package org.openelisglobal.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AjaxFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(AjaxFilter.class);

    public AjaxFilter() {
        log.info("AjaxFilter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes((HttpServletRequest) request));
        }
        chain.doFilter(request, response);
        RequestContextHolder.resetRequestAttributes(); // Clean up after request is done
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}
