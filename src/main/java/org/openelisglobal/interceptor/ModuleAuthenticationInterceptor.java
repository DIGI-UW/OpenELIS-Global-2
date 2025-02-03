package org.openelisglobal.interceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        Errors errors = new BaseErrors();
        if (!hasPermission(errors, request)) {
            LogEvent.logInfo("ModuleAuthenticationInterceptor", "preHandle()",
                    "======> NOT ALLOWED ACCESS TO THIS MODULE");
            LogEvent.logInfo(this.getClass().getSimpleName(), "preHandle", "has no permission"); //
            redirectStrategy.sendRedirect(request, response, "/Home?access=denied");
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
        List<SystemModuleUrl> sysModsByUrl = systemModuleUrlService.getByRequest(request);

        if (useParameters) {
            sysModsByUrl = filterParamMatches(request, sysModsByUrl);
        }
        if (sysModsByUrl.isEmpty() && REQUIRE_MODULE) {
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
}
