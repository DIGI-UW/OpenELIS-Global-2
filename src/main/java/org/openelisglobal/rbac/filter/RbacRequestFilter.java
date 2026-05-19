package org.openelisglobal.rbac.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TR-04/TR-05: Populates RbacContext for each request so services can enforce
 * department filtering without needing the HttpServletRequest.
 */
@Component("rbacRequestFilter")
public class RbacRequestFilter implements Filter {

    @Autowired
    private RbacPermissionService rbacPermissionService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                Object sessionAttr = httpRequest.getSession(false) != null
                        ? httpRequest.getSession().getAttribute(IActionConstants.USER_SESSION_DATA)
                        : null;
                if (sessionAttr instanceof UserSessionData usd) {
                    try {
                        String userId = String.valueOf(usd.getSystemUserId());
                        String username = usd.getLoginName();
                        String ip = httpRequest.getRemoteAddr();
                        List<String> allowedDepts = rbacPermissionService.getAllowedDepartments(userId);
                        List<String> allowedProjects = rbacPermissionService.getAllowedProjects(userId);
                        // Active department = the lab unit the user logged into this session.
                        // This is the hard data-scope boundary for storage/inventory/equipment.
                        String activeDeptId = usd.getLoginLabUnit() > 0 ? String.valueOf(usd.getLoginLabUnit()) : null;
                        // Admins (unrestricted) have no active-department constraint
                        if (allowedDepts == null)
                            activeDeptId = null;
                        RbacContext.set(userId, username, ip, allowedDepts, allowedProjects, activeDeptId);
                    } catch (Exception e) {
                        // DB not ready or schema missing — skip context population
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            RbacContext.clear();
        }
    }
}
