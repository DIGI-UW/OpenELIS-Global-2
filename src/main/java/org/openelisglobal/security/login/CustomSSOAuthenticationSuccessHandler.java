package org.openelisglobal.security.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.json.JSONObject;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
public class CustomSSOAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler
        implements IActionConstants {
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private LoginUserService loginUserService;
    @Autowired
    private RoleService roleService;

    @Autowired
    @Qualifier("daemonSysUserId")
    private String daemonSysUserId;

    @Value("${org.openelisglobal.timezone:}")
    private String timezone;

    @Value("${org.itech.login.sso.provisionUnknownUsers:false}")
    private boolean provisionUnknownUsers;

    public static final int DEFAULT_SESSION_TIMEOUT_IN_MINUTES = 20;

    public CustomSSOAuthenticationSuccessHandler() {
        super();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // String homePath = "/Dashboard";
        // LoginUser loginInfo = null;
        boolean apiLogin = "true".equals(request.getParameter("apiCall"));
        boolean samlLogin = false;
        boolean oauthLogin = false;

        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof DefaultSaml2AuthenticatedPrincipal) {
                setupUserSession(request, (DefaultSaml2AuthenticatedPrincipal) principal);
                request.getSession().setAttribute("samlSession", true);
                samlLogin = true;
            } else if (principal instanceof DefaultOAuth2User) {
                setupUserSession(request, (DefaultOAuth2User) principal);
                request.getSession().setAttribute("oauthSession", true);
                oauthLogin = true;
            }
        } catch (DisabledException e) {
            refuseLogin(request, response, "login.error.account.disable");
            return;
        } catch (LockedException e) {
            refuseLogin(request, response, "login.error.account.lock");
            return;
        } catch (IllegalStateException e) {
            refuseLogin(request, response, "login.error.noOeUser");
            return;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            refuseLogin(request, response, "login.error.sessionsetup");
            return;
        }

        // get the X-Forwarded-For header so that we know if the request is from a proxy
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            // no proxy
            LogEvent.logInfo(this.getClass().getSimpleName(), "onSuccess",
                    "Successful login attempt for " + authentication.getName() + " from " + request.getRemoteAddr());
        } else {
            // from proxy
            LogEvent.logInfo(this.getClass().getSimpleName(), "onSuccess",
                    "Successful login attempt for " + authentication.getName() + " from " + xfHeader.split(",")[0]);
        }

        // if (passwordExpiringSoon(loginInfo)) {
        // homePath += "?passReminder=true";
        // }

        if (apiLogin) {
            request.getSession().setAttribute("login_method", "form");
            this.handleApiLogin(request, response);
        } else if (samlLogin) {
            request.getSession().setAttribute("login_method", "samlLogin");
            getRedirectStrategy().sendRedirect(request, response, "/Home");
        } else if (oauthLogin) {
            request.getSession().setAttribute("login_method", "oauthLogin");
            this.handleApiLogin(request, response);
        } else {
            // redirectStrategy.sendRedirect(request, response, homePath);
            super.onAuthenticationSuccess(request, response, authentication);
            clearCustomAuthenticationAttributes(request);
        }
    }

    private void refuseLogin(HttpServletRequest request, HttpServletResponse response, String errorCode)
            throws IOException {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        BaseErrors errors = new BaseErrors();
        errors.reject(errorCode);
        request.getSession().setAttribute(Constants.LOGIN_ERRORS, errors);
        getRedirectStrategy().sendRedirect(request, response, "/LoginPage");
    }

    private void handleApiLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");

        out.print(new JSONObject().put("success", true));
    }

    private void setupUserSession(HttpServletRequest request, DefaultSaml2AuthenticatedPrincipal principal) {
        int timeout;

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities();

        boolean isAdmin = false;
        for (GrantedAuthority authority : authorities) {
            String[] authorityExplode = authority.getAuthority().split("-");
            if (authorityExplode.length >= 2) {
                isAdmin = "admin".equalsIgnoreCase(authorityExplode[1]);
            }
        }
        // if (loginInfo == null) {
        // throw new IllegalStateException("no loginUser during user session setup");
        // }
        // if (loginInfo.getUserTimeOut() != null) {
        // timeout = Integer.parseInt(loginInfo.getUserTimeOut()) * 60;
        // } else {
        // timeout = DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60;
        // }
        request.getSession().setMaxInactiveInterval(DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60);

        // get system user and link to login user
        // SystemUser su =
        // systemUserService.get(String.valueOf(loginInfo.getSystemUserId()));
        // create usersessiondata and store in session
        UserSessionData usd = new UserSessionData();

        SystemUser systemUser = accountFor(principal.getName());
        usd.setSytemUserId(Integer.parseInt(systemUser.getId()));
        usd.setLoginName(principal.getName());
        // usd.setElisUserName(su.getNameForDisplay());
        usd.setElisUserName(principal.getName());
        usd.setUserTimeOut(DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60);
        usd.setAdmin(isAdmin);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        request.getSession().setAttribute("timezone", timezone);

        // get permitted actions map (available modules for the current user)
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            Set<String> permittedPages = getPermittedForms(authorities);
            request.getSession().setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, permittedPages);
            // showAdminMenu |= permittedPages.contains("MasterList");
        }
    }

    private void setupUserSession(HttpServletRequest request, DefaultOAuth2User principal) {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities();

        boolean isAdmin = false;
        for (GrantedAuthority authority : authorities) {
            String[] authorityExplode = authority.getAuthority().split("-");
            if (authorityExplode.length >= 2) {
                isAdmin = "admin".equalsIgnoreCase(authorityExplode[1]);
            }
        }
        request.getSession().setMaxInactiveInterval(DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60);

        UserSessionData usd = new UserSessionData();
        SystemUser systemUser = accountFor(principal.getName());
        usd.setSytemUserId(Integer.parseInt(systemUser.getId()));
        usd.setLoginName(principal.getName());
        // usd.setElisUserName(su.getNameForDisplay());
        usd.setElisUserName(principal.getName());
        usd.setUserTimeOut(DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60);
        usd.setAdmin(isAdmin);
        request.getSession().setAttribute("authorities", usd);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        request.getSession().setAttribute("timezone", timezone);

        // get permitted actions map (available modules for the current user)
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            Set<String> permittedPages = getPermittedForms(authorities);
            request.getSession().setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, permittedPages);
            // showAdminMenu |= permittedPages.contains("MasterList");
        }
    }

    /**
     * The identity provider asserts who signed in, not who may use this laboratory.
     * The account has to exist here already; an installation that wants sign-ins to
     * create accounts opts in with
     * {@code org.itech.login.sso.provisionUnknownUsers}. A missing account refuses
     * the login as "no OpenELIS user" and creates nothing. Existing inactive,
     * disabled or locked accounts are refused even when provisioning is enabled.
     */
    private SystemUser accountFor(String loginName) {
        Optional<SystemUser> user = systemUserService.getMatch("loginName", loginName);
        // Local account controls still apply after the identity provider authenticates.
        // A password expiry belongs to form login, not to the external credential.
        Optional<LoginUser> login = loginUserService.getMatch("loginName", loginName);
        if (login.isPresent()) {
            if (YES.equalsIgnoreCase(login.get().getAccountDisabled())) {
                throw new DisabledException("OpenELIS account is disabled");
            }
            if (YES.equalsIgnoreCase(login.get().getAccountLocked())) {
                throw new LockedException("OpenELIS account is locked");
            }
        }
        if (user.isPresent()) {
            if (!YES.equalsIgnoreCase(user.get().getIsActive())) {
                throw new DisabledException("OpenELIS account is inactive");
            }
            return user.get();
        }
        if (!provisionUnknownUsers) {
            throw new IllegalStateException("no OpenELIS account for the identity provider user " + loginName);
        }
        SystemUser systemUser = new SystemUser();
        systemUser.setFirstName(loginName);
        systemUser.setLastName("");
        systemUser.setLoginName(loginName);
        systemUser.setIsActive("Y");
        systemUser.setIsEmployee("Y");
        systemUser.setExternalId("1");
        systemUser.setInitials(GenericValidator.isBlankOrNull(loginName) ? "" : loginName.substring(0, 1));
        systemUser.setSysUserId(daemonSysUserId);
        return systemUserService.save(systemUser);
    }

    private Set<String> getPermittedForms(Collection<? extends GrantedAuthority> authorities) {
        Set<String> allPermittedPages = new HashSet<>();

        // List<String> roleIds =
        // userRoleService.getRoleIdsForUser(Integer.toString(systemUserId));
        List<String> roleIds = new ArrayList<>();

        for (GrantedAuthority authority : authorities) {
            String[] authorityExplode = authority.getAuthority().split("-");
            if (authorityExplode.length >= 2) {
                String role = getRoleForAuthority(authorityExplode[1]);
                if (!GenericValidator.isBlankOrNull(role)) {
                    roleIds.add(role);
                }
            }
        }

        for (String roleId : roleIds) {
            Set<String> permittedPagesForRole = permissionModuleService
                    .getAllPermittedPagesFromAgentId(Integer.parseInt(roleId));
            allPermittedPages.addAll(permittedPagesForRole);
        }

        return allPermittedPages;
    }

    private String getRoleForAuthority(String string) {
        Optional<Role> sysRole = roleService.getMatch("name", string);
        if (sysRole.isPresent()) {
            return sysRole.get().getId();
        }
        LogEvent.logWarn(this.getClass().getSimpleName(), "getRoleForAuthority",
                "could not find a role for the authority: " + string);
        return null;
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

    private boolean passwordExpiringSoon(LoginUser loginInfo) {
        return loginInfo.getPasswordExpiredDayNo() <= Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("login.user.expired.reminder.day"))
                && (loginInfo.getPasswordExpiredDayNo() > Integer.parseInt(
                        ConfigurationProperties.getInstance().getPropertyValue("login.user.change.allow.day")));
    }

    protected void clearCustomAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute("login_errors");
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    protected void addFlashMsgsToRequest(HttpServletRequest request) {
        Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
        if (inputFlashMap != null) {
            Boolean success = (Boolean) inputFlashMap.get(FWD_SUCCESS);
            request.setAttribute(FWD_SUCCESS, success);

            String successMessage = (String) inputFlashMap.get(Constants.SUCCESS_MSG);
            request.setAttribute(Constants.SUCCESS_MSG, successMessage);

            Errors errors = (Errors) inputFlashMap.get(Constants.REQUEST_ERRORS);
            request.setAttribute(Constants.SUCCESS_MSG, errors);

            List<String> messages = (List<String>) inputFlashMap.get(Constants.REQUEST_MESSAGES);
            request.setAttribute(Constants.REQUEST_MESSAGES, messages);

            List<String> warnings = (List<String>) inputFlashMap.get(Constants.REQUEST_WARNINGS);
            request.setAttribute(Constants.SUCCESS_MSG, warnings);
        }
    }
}
