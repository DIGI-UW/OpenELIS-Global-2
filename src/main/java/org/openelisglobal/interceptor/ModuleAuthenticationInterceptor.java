package org.openelisglobal.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@Qualifier(value = "ModuleAuthenticationInterceptor")
public class ModuleAuthenticationInterceptor implements HandlerInterceptor {

    private static final boolean USE_PARAMETERS = true;

    // whether to reject access to protected pages if no modules are assigned
    public static final boolean REQUIRE_MODULE = true;

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Autowired
    private UserModuleService userModuleService;
    @Autowired
    private SystemModuleUrlService systemModuleUrlService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;
    String path;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        path = request.getRequestURI().substring(request.getContextPath().length());
        Errors errors = new BaseErrors();
        if (!hasPermission(errors, request)) {
            LogEvent.logInfo("ModuleAuthenticationInterceptor", "preHandle()",
                    "======> NOT ALLOWED ACCESS TO THIS MODULE");
            LogEvent.logInfo(this.getClass().getSimpleName(), "preHandle", "has no permission"); //
            if (isRestFullPath()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String jsonResponse = "{ \"status\": 401, \"message\": \"Not Authorized\" }";
                response.getWriter().write(jsonResponse);
                response.getWriter().flush();
            } else {
                redirectStrategy.sendRedirect(request, response, "/Home?access=denied");
            }
            return false;
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
    }

    protected boolean hasPermission(Errors errors, HttpServletRequest request) {
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            return hasPermissionForUrl(request, USE_PARAMETERS) || userModuleService.isUserAdmin(request);
        } else {
            return userModuleService.isVerifyUserModule(request) || userModuleService.isUserAdmin(request);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasPermissionForUrl(HttpServletRequest request, boolean useParameters) {
        HashSet<String> accessMap = (HashSet<String>) request.getSession()
                .getAttribute(IActionConstants.PERMITTED_ACTIONS_MAP);
        if (accessMap == null) {
            accessMap = (HashSet<String>) request.getAttribute(IActionConstants.PERMITTED_ACTIONS_MAP);
        }

        if (accessMap == null) {
            Set<String> permittedPages = getPermittedForms(getSysUserId(request));
            accessMap = (HashSet<String>) permittedPages;
        }
        List<SystemModuleUrl> sysModsByUrl = systemModuleUrlService.getByRequest(request);

        if (useParameters) {
            sysModsByUrl = filterParamMatches(request, sysModsByUrl);
        }
        if (sysModsByUrl.isEmpty() && REQUIRE_MODULE) {
            if (isRestFullPath()) {
                return true;
            }
            LogEvent.logWarn("ModuleAuthenticationInterceptor", "hasPermissionForUrl()",
                    "This page has no modules assigned to it");
            return false;
        }
        for (SystemModuleUrl sysModUrl : sysModsByUrl) {
            if (accessMap.contains(sysModUrl.getSystemModule().getSystemModuleName())) {
                return true;
            }
        }
        return false;
    }

    private List<SystemModuleUrl> filterParamMatches(HttpServletRequest request, List<SystemModuleUrl> sysModsByUrl) {
        List<SystemModuleUrl> filteredSysModsByUrl = new ArrayList<>();
        for (SystemModuleUrl sysModUrl : sysModsByUrl) {
            boolean matchAll = true;
            SystemModuleParam param = sysModUrl.getParam();
            if (param != null) {
                if (!param.getValue().equals(request.getParameter(param.getName()))) {
                    matchAll = false;
                }
            }
            if (matchAll) {
                filteredSysModsByUrl.add(sysModUrl);
            }
        }
        return filteredSysModsByUrl;
    }

    private Set<String> getPermittedForms(int systemUserId) {
        Set<String> allPermittedPages = new HashSet<>();

        List<String> roleIds = userRoleService.getRoleIdsForUser(Integer.toString(systemUserId));

        for (String roleId : roleIds) {
            Set<String> permittedPagesForRole = permissionModuleService
                    .getAllPermittedPagesFromAgentId(Integer.parseInt(roleId));
            allPermittedPages.addAll(permittedPagesForRole);
        }

        return allPermittedPages;
    }

    protected int getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(IActionConstants.USER_SESSION_DATA);
        if (usd == null) {
            usd = (UserSessionData) request.getAttribute(IActionConstants.USER_SESSION_DATA);
            if (usd == null) {
                return 0;
            }
        }
        return usd.getSystemUserId();
    }

    private boolean isRestFullPath() {
        if (path.startsWith("/rest") || path.startsWith("/Provider")) {
            return true;
        }
        return false;
    }
}
