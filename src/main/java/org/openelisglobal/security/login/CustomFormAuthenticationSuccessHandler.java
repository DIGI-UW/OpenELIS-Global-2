package org.openelisglobal.security.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.RequestContextUtils;

@Component
public class CustomFormAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler
        implements IActionConstants {

    @Autowired
    private LoginUserService loginService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private NotificationDAO notificationDAO;

    @Value("${org.openelisglobal.timezone:}")
    private String timezone;

    public static final int DEFAULT_SESSION_TIMEOUT_IN_MINUTES = 20;

    public CustomFormAuthenticationSuccessHandler() {
        super();
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
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

        String homePath = "/Dashboard";
        LoginUser loginInfo = null;
        boolean apiLogin = "true".equals(request.getParameter("apiCall"));
        boolean samlLogin = false;
        boolean oauthLogin = false;
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails user = (UserDetails) principal;
                loginInfo = loginService.getUserProfile(user.getUsername());
            }
        }
        try {
            setupUserSession(request, loginInfo);
        } catch (IllegalStateException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "onAuthenticationSuccess",
                    "the login user doesn't exist in OE this is usually caused by login being handled by an"
                            + " external application that contains a user that OE is missing");
            SecurityContextHolder.getContext().setAuthentication(null);
            BaseErrors errors = new BaseErrors();
            errors.reject("login.error.noOeUser");

            request.getSession().setAttribute(Constants.LOGIN_ERRORS, errors);
            getRedirectStrategy().sendRedirect(request, response, "/LoginPage");
            return;
        } catch (RuntimeException e) {
            LogEvent.logError(e);

            SecurityContextHolder.getContext().setAuthentication(null);
            BaseErrors errors = new BaseErrors();
            errors.reject("login.error.sessionsetup");

            request.getSession().setAttribute(Constants.LOGIN_ERRORS, errors);
            getRedirectStrategy().sendRedirect(request, response, "/LoginPage");
            return;
        }

        if (passwordExpiringSoon(loginInfo)) {
            homePath += "?passReminder=true";
        }

        if (apiLogin) {
            this.handleApiLogin(request, response);
        } else if (samlLogin) {
            this.handleApiLogin(request, response);
        } else if (oauthLogin) {
            this.handleApiLogin(request, response);
        } else {
            // redirectStrategy.sendRedirect(request, response, homePath);
            super.onAuthenticationSuccess(request, response, authentication);
            clearCustomAuthenticationAttributes(request);
        }
    }

    private void handleApiLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");

        out.print(new JSONObject().put("success", true));
    }

    private void setupUserSession(HttpServletRequest request, LoginUser loginInfo) {
        int timeout;
        if (loginInfo == null) {
            throw new IllegalStateException("no loginUser during user session setup");
        }
        if (loginInfo.getUserTimeOut() != null) {
            timeout = Integer.parseInt(loginInfo.getUserTimeOut()) * 60;
        } else {
            timeout = DEFAULT_SESSION_TIMEOUT_IN_MINUTES * 60;
        }
        request.getSession().setMaxInactiveInterval(timeout);

        // get system user and link to login user
        SystemUser su = systemUserService.get(String.valueOf(loginInfo.getSystemUserId()));
        // create usersessiondata and store in session
        UserSessionData usd = new UserSessionData();

        usd.setSytemUserId(loginInfo.getSystemUserId());
        usd.setLoginName(loginInfo.getLoginName());
        usd.setElisUserName(su.getNameForDisplay());
        usd.setUserTimeOut(timeout * 60);
        usd.setAdmin(loginService.isUserAdmin(loginInfo));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        request.getSession().setAttribute("timezone", timezone);

        // get permitted actions map (available modules for the current user)
        if (ConfigurationProperties.getInstance().getPropertyValue("permissions.agent").equalsIgnoreCase("ROLE")) {
            Set<String> permittedPages = getPermittedForms(usd.getSystemUserId());
            request.getSession().setAttribute(IActionConstants.PERMITTED_ACTIONS_MAP, permittedPages);
            // showAdminMenu |= permittedPages.contains("MasterList");
        }

        if (passwordExpiringSoon(loginInfo)) {
            Notification notification = new Notification();
            notification.setMessage("Your password will expire in " + loginInfo.getPasswordExpiredDayNo()
                    + " day(s). Please update it soon.");
            notification.setUser(su);
            notification.setCreatedDate(OffsetDateTime.now());
            notification.setReadAt(null);
            notificationDAO.save(notification);
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
