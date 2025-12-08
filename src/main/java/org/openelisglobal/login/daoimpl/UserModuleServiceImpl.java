/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.login.daoimpl;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.security.PageIdentityUtil;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.service.SystemUserModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.systemusermodule.valueholder.SystemUserModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
/*
 * N.B. This class has nothing to do with database access
 */
@Service
public class UserModuleServiceImpl implements UserModuleService, IActionConstants {

    @Autowired
    PermissionModuleService<PermissionModule> permissionModuleService;
    @Autowired
    SystemUserModuleService systemUserModuleService;
    @Autowired
    LoginUserService loginService;

    @Override
    public boolean isSessionExpired(HttpServletRequest request) throws LIMSRuntimeException {
        return request.getSession().getAttribute(USER_SESSION_DATA) == null;
    }

    /**
     * Check if the user has any module assign to him/her
     *
     * @param request is HttpServletRequest
     * @return true if found, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    @Deprecated
    public boolean isUserModuleFound(HttpServletRequest request) throws LIMSRuntimeException {
        boolean isFound = false;
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            isFound = permissionModuleService.doesUserHaveAnyModules(usd.getSystemUserId());
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl isUserModuleFound()", e);
        }
        return isFound;
    }

    /**
     * TODO: Setup the user accessible buttons in the user object Preparing and
     * setting the user module select/add/update/delete disable/enable buttons
     *
     * @param request is HttpServletRequest
     * @return true if success, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isVerifyUserModule(HttpServletRequest request) throws LIMSRuntimeException {
        boolean isFound = PageIdentityUtil.isMainPage(request);

        if (!isFound) {
            try {
                UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
                List list = systemUserModuleService.getAllPermissionModulesByAgentId(usd.getSystemUserId());

                for (int i = 0; i < list.size(); i++) {
                    SystemUserModule systemUserModule = (SystemUserModule) list.get(i);
                    String userAssignedModule = systemUserModule.getSystemModule().getSystemModuleName();
                    String actionName = getActionName(request, userAssignedModule);

                    // we want to check only part of action class name
                    // because each module/action can have up to 5 or 6 names
                    if (actionName.equals(userAssignedModule) || actionName.startsWith(userAssignedModule + "Menu")) {
                        isFound = true;
                        setupUserButtons(request, systemUserModule, actionName);
                        // bugzilla 2154
                        LogEvent.logInfo("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> ALLOWED ACCESS TO THIS MODULE");
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> MODULE ID   : " + systemUserModule.getSystemModule().getId());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> MODULE NAME : " + systemUserModule.getSystemModule().getSystemModuleName());

                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> ALLOW_VIEW  : " + systemUserModule.getHasSelect());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> ALLOW_ADD   : " + systemUserModule.getHasAdd());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> ALLOW_UPDATE: " + systemUserModule.getHasUpdate());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> ALLOW_DELETE: " + systemUserModule.getHasDelete());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> SYSTEM MODULE DEFAULT VALUE");
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> HAS_VIEW  : " + systemUserModule.getSystemModule().getHasSelectFlag());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> HAS_ADD   : " + systemUserModule.getSystemModule().getHasAddFlag());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> HAS_UPDATE: " + systemUserModule.getSystemModule().getHasUpdateFlag());
                        LogEvent.logDebug("UserModuleServiceImpl", "isVerifyUserModule()",
                                "======> HAS_DELETE: " + systemUserModule.getSystemModule().getHasDeleteFlag());

                        break;
                    }
                }
            } catch (LIMSRuntimeException e) {
                // bugzilla 2154
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error in UserModuleServiceImpl isVerifyUserModule()", e);
            }
        }
        return isFound;
    }

    /**
     * Get the user login information bases on the user login name
     *
     * @param request is HttpServletRequest
     * @return user information
     */
    @Transactional(readOnly = true)
    private LoginUser getUserLogin(HttpServletRequest request) throws LIMSRuntimeException {
        LoginUser login = null;
        try {
            // UserSessionData usd = (UserSessionData)
            // request.getSession().getAttribute(USER_SESSION_DATA);
            // login = loginService.getUserProfile(usd.getLoginName());
            // Authentication authentication2 =
            // SecurityContextHolder.getContext().getAuthentication();
            // TODO workaround for Security Context authentication is null
            Object sc = request.getSession()
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (!(sc instanceof SecurityContext)) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "getUserLogin",
                        "security context is not of type SecurityContext");
            }
            Authentication authentication = ((SecurityContext) sc).getAuthentication();
            if (authentication != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails) {
                    UserDetails user = (UserDetails) principal;
                    login = loginService.getUserProfile(user.getUsername());
                } else if (principal instanceof DefaultSaml2AuthenticatedPrincipal) {
                    DefaultSaml2AuthenticatedPrincipal samlUser = (DefaultSaml2AuthenticatedPrincipal) principal;
                    login = loginService.getUserProfile(samlUser.getName());
                } else if (principal instanceof DefaultOAuth2User) {
                    DefaultOAuth2User oauthUser = (DefaultOAuth2User) principal;
                    login = loginService.getUserProfile(oauthUser.getName());
                }
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl getUserLogin()", e);
        }
        return login;
    }

    /**
     * Check if the user account in locked
     *
     * @param request is HttpServletRequest
     * @return true if locked, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAccountLocked(HttpServletRequest request) throws LIMSRuntimeException {
        try {
            LoginUser login = getUserLogin(request);
            if (login.getAccountLocked().equalsIgnoreCase(YES)) {
                return true;
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl isAccountLocked()", e);
        }
        return false;
    }

    /**
     * Check if the user account in disabled
     *
     * @param request is HttpServletRequest
     * @return true if disabled, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAccountDisabled(HttpServletRequest request) throws LIMSRuntimeException {
        try {
            LoginUser login = getUserLogin(request);
            if (login.getAccountDisabled().equalsIgnoreCase(YES)) {
                return true;
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl isAccountDisabled()", e);
        }
        return false;
    }

    /**
     * Check if the user password is expired
     *
     * @param request is HttpServletRequest
     * @return true if expired, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordExpired(HttpServletRequest request) throws LIMSRuntimeException {
        try {
            LoginUser login = getUserLogin(request);
            if (login.getPasswordExpiredDayNo() <= 0) {
                return true;
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl isPasswordExpired()", e);
        }
        return false;
    }

    /**
     * Check if the user is admin role
     *
     * @param request is HttpServletRequest
     * @return true if admin, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isUserAdmin(HttpServletRequest request) throws LIMSRuntimeException {
        try {
            LoginUser login = getUserLogin(request);
            if (login != null && login.getIsAdmin().equalsIgnoreCase(YES)) {
                return true;
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl isUserAdmin()", e);
        }
        return false;
    }

    /**
     * Setup the user session time bases on the information in
     *
     * <table>
     * LOGIN_USER
     * </table>
     *
     * @param request is HttpServletRequest
     */
    @Override
    public void setupUserSessionTimeOut(HttpServletRequest request) throws LIMSRuntimeException {
        try {
            LoginUser login = getUserLogin(request);
            int timeOut = Integer.parseInt(login.getUserTimeOut());

            request.getSession().setMaxInactiveInterval(timeOut * 60);
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            usd.setUserTimeOut(timeOut * 60);
            request.getSession().setAttribute(USER_SESSION_DATA, usd);
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl setupUserSessionTimeOut()", e);
        }
    }

    /**
     * TOD): will move hardcoded action names to the property file
     *
     * @param request
     * @param userAssignedModule
     * @return actionName the name of the action form
     */
    private String getActionName(HttpServletRequest request, String userAssignedModule) throws LIMSRuntimeException {

        String actionName = null;
        try {
            actionName = (String) request.getAttribute(ACTION_KEY);

            // bugzilla 2154
            LogEvent.logDebug("UserModuleServiceImpl", "getActionName()",
                    "======> USER ASSIGNED MODULE: " + userAssignedModule);
            LogEvent.logDebug("UserModuleServiceImpl", "getActionName()",
                    "======> ACTION MODULE NAME  : " + actionName);

            // N.B. The effect of this first if is that the first module on the list for the
            // user becomes the
            // actionName. This does not seem correct.
            if ((actionName == null) || (actionName.length() == 0)) {
                actionName = userAssignedModule;
            } else if (actionName.equals("QuickEntryAddTestPopup")) {
                actionName = "QuickEntry";
            } else if (actionName.equals("TestManagementAddTestPopup")) {
                actionName = "TestManagement";
                // bugzilla 1844: removing HumanSampleOneAddTestPopup
            } else if (actionName.equals("TestAnalyteTestResultAddDictionaryRGPopup")
                    || actionName.equals("TestAnalyteTestResultAddNonDictionaryRGPopup")
                    || actionName.equals("TestAnalyteTestResultAddRGPopup")
                    || actionName.equals("TestAnalyteTestResultAssignRGPopup")
                    || actionName.equals("TestAnalyteTestResultEditDictionaryRGPopup")
                    || actionName.equals("TestAnalyteTestResultEditDictionaryRGPopup")
                    || actionName.equals("TestAnalyteTestResultEditNonDictionaryRGPopup")) {
                actionName = "TestAnalyteTestResult";
            } else if (actionName.equals("QaEventsEntryAddQaEventsToTestsPopup")
                    || actionName.equals("QaEventsEntryAddActionsToQaEventsPopup")) {
                actionName = "QaEventsEntry";
            }
            // bugzilla 2204
            else if (actionName.equals("NotesPopup")) {
                if (userAssignedModule.equals("QaEventsEntry")) {
                    actionName = "QaEventsEntry";
                }
                if (userAssignedModule.equals("ResultsEntry")) {
                    actionName = "ResultsEntry";
                }
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl getActionName()", e);
        }
        return actionName;
    }

    /**
     * TODO: Setup the user accessible buttons in the user object Enabled the
     * buttons for user type admin
     *
     * @param request
     *                <p>
     *                N.B. It is not clear why business rules are in a DAO object.
     */
    @Override
    public void enabledAdminButtons(HttpServletRequest request) throws LIMSRuntimeException {
        String active = "true";
        if (request.getAttribute(org.openelisglobal.common.action.IActionConstants.DEACTIVATE_DISABLED) != null) {
            active = (String) request
                    .getAttribute(org.openelisglobal.common.action.IActionConstants.DEACTIVATE_DISABLED);
        }

        try {
            request.setAttribute(VIEW_DISABLED, FALSE); // enabled view
            request.setAttribute(ADD_DISABLED, FALSE); // enabled add
            String actionName = (String) request.getAttribute(ACTION_KEY);
            if (actionName != null) {
                if (!actionName.equals("HumanSampleTwo")) {
                    request.setAttribute(ALLOW_EDITS_KEY, FALSE); // enabled edit
                }

                request.setAttribute(SAVE_DISABLED, FALSE); // enabled save

                // bugzilla 2214
                if (active.equals("false")) {
                    request.setAttribute(DEACTIVATE_DISABLED, FALSE); // enabled delete
                } else {
                    request.setAttribute(DEACTIVATE_DISABLED, TRUE); // disabled delete
                }
            }
        } catch (LIMSRuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserModuleServiceImpl enabledAdminButtons()", e);
        }
    }

    /**
     * Setup the user buttons
     *
     * @param request
     * @param systemUserModule
     * @param actionName
     * @throws LIMSRuntimeException
     */
    private void setupUserButtons(HttpServletRequest request, SystemUserModule systemUserModule, String actionName)
            throws LIMSRuntimeException {

        // system module default setting (SELECT)
        if (systemUserModule.getSystemModule().getHasSelectFlag().equalsIgnoreCase(YES)) {
            // user module default setting
            if (systemUserModule.getHasSelect().equalsIgnoreCase(YES)) {
                request.setAttribute(VIEW_DISABLED, FALSE);
            } else {
                request.setAttribute(VIEW_DISABLED, TRUE);
            }
        } else {
            request.setAttribute(VIEW_DISABLED, TRUE);
        }

        // system module default setting (ADD)
        if (systemUserModule.getSystemModule().getHasAddFlag().equalsIgnoreCase(YES)) {
            // user module default setting
            if (systemUserModule.getHasAdd().equalsIgnoreCase(YES)) {
                request.setAttribute(ADD_DISABLED, FALSE);
            } else {
                request.setAttribute(ADD_DISABLED, TRUE);
            }
        } else {
            request.setAttribute(ADD_DISABLED, TRUE);
        }

        // system module default setting (UPDATE)
        if (systemUserModule.getSystemModule().getHasUpdateFlag().equalsIgnoreCase(YES)) {
            // user module default setting
            if (systemUserModule.getHasUpdate().equalsIgnoreCase(YES)) {
                if (!actionName.equals("HumanSampleTwo")) {
                    request.setAttribute(ALLOW_EDITS_KEY, FALSE); // FALSE = allows??
                }
                request.setAttribute(SAVE_DISABLED, FALSE);
            } else {
                if (!actionName.equals("HumanSampleTwo")) {
                    request.setAttribute(ALLOW_EDITS_KEY, TRUE);
                }
                request.setAttribute(SAVE_DISABLED, TRUE);
            }
        } else {
            request.setAttribute(SAVE_DISABLED, TRUE);
        }

        // bugzilla 2214
        String active = "true";
        if (request.getAttribute(org.openelisglobal.common.action.IActionConstants.DEACTIVATE_DISABLED) != null) {
            active = (String) request
                    .getAttribute(org.openelisglobal.common.action.IActionConstants.DEACTIVATE_DISABLED);
        }

        if (active.equals("false")) {
            // system module default setting (DELETE)
            if (systemUserModule.getSystemModule().getHasDeleteFlag().equalsIgnoreCase(YES)) {
                // user module default setting
                if (systemUserModule.getHasDelete().equalsIgnoreCase(YES)) {
                    request.setAttribute(DEACTIVATE_DISABLED, FALSE);
                } else {
                    request.setAttribute(DEACTIVATE_DISABLED, TRUE);
                }
            } else {
                request.setAttribute(DEACTIVATE_DISABLED, TRUE);
            }
        } else {
            request.setAttribute(DEACTIVATE_DISABLED, TRUE);
        }
    }
}
