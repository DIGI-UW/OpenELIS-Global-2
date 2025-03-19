package org.openelisglobal.security.login;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component("basicAuthFilter")
public class BasicAuthFilter extends BasicAuthenticationFilter {

    @Autowired
    private LoginUserService loginService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;

    @Autowired
    public BasicAuthFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void onSuccessfulAuthentication(jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response, Authentication authResult) {
        LoginUser loginInfo = loginService.getMatch("loginName", authResult.getName()).get();
        setupUserRequest(request, loginInfo);
    }

    private void setupUserRequest(HttpServletRequest request, LoginUser loginInfo) {
        // get system user and link to login user
        SystemUser su = systemUserService.get(String.valueOf(loginInfo.getSystemUserId()));
        // create usersessiondata and store in session
        UserSessionData usd = new UserSessionData();

        usd.setSytemUserId(loginInfo.getSystemUserId());
        usd.setLoginName(loginInfo.getLoginName());
        usd.setElisUserName(su.getNameForDisplay());
        usd.setAdmin(loginService.isUserAdmin(loginInfo));
        request.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        // get permitted actions map (available modules for the current user)
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            Set<String> permittedPages = getPermittedForms(usd.getSystemUserId());
            request.setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, permittedPages);
            // showAdminMenu |= permittedPages.contains("MasterList");
        }
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
}
