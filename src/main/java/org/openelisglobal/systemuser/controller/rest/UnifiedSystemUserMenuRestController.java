package org.openelisglobal.systemuser.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.controller.BaseMenuController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.form.AdminOptionMenuForm;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.form.UnifiedSystemUserMenuForm;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UnifiedSystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemuser.valueholder.UnifiedSystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class UnifiedSystemUserMenuRestController extends BaseMenuController<UnifiedSystemUser> {

    // private static final String FWD_SUCCESS = "success";
    private static final String FWD_FAIL = "fail";
    private static final String FWD_SUCCESS_DELETE = "User Delete Success";
    private static final String FWD_FAIL_DELETE = "User Delete Fail";

    private static final String[] ALLOWED_FIELDS = new String[] { "selectedIDs*" };

    @Autowired
    SystemUserService systemUserService;
    @Autowired
    LoginUserService loginService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    UnifiedSystemUserService unifiedSystemUserService;
    @Autowired
    private UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = { "/UnifiedSystemUserMenu", "/SearchUnifiedSystemUserMenu" })
    public UnifiedSystemUserMenuForm showUnifiedSystemUserMenu(HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        UnifiedSystemUserMenuForm form = new UnifiedSystemUserMenuForm();

        form.setFormAction("UnifiedSystemUserMenu");
        List<IdValuePair> testSections = DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE);
        form.setTestSections(testSections);
        String forward = performMenuAction(form, request);
        // request.setAttribute(IActionConstants.FORM_NAME, "unifiedSystemUserMenu");
        // request.setAttribute(IActionConstants.MENU_PAGE_INSTRUCTION,
        // "user.select.instruction");
        // request.setAttribute(IActionConstants.MENU_OBJECT_TO_ADD,
        // "label.button.new.user");
        // request.setAttribute(IActionConstants.APPLY_FILTER, "true");
        if (FWD_FAIL.equals(forward)) {
            Errors errors = new BaseErrors();
            errors.reject("error.generic");
            // redirectAttributes.addFlashAttribute(Constants.REQUEST_ERRORS, errors);
            // return findForward(FWD_FAIL, form);
            // result.addError(errors);
            return form;
        }
        // else {
        // return findForward(forward, form);
        // }

        return form;
    }

    @Override
    protected List<UnifiedSystemUser> createMenuList(AdminOptionMenuForm<UnifiedSystemUser> form,
            HttpServletRequest request) {
        List<SystemUser> systemUsers = new ArrayList<>();

        int startingRecNo = this.getCurrentStartingRecNo(request);

        systemUsers = systemUserService.getPage(startingRecNo);

        if (YES.equals(request.getParameter("search"))) {
            systemUsers = systemUserService.getPagesOfSearchedUsers(startingRecNo,
                    request.getParameter("searchString"));
            request.setAttribute(MENU_TOTAL_RECORDS,
                    String.valueOf(systemUserService.getTotalSearchedUserCount(request.getParameter("searchString"))));
            request.setAttribute(SEARCHED_STRING, request.getParameter("searchString"));
        } else {
            systemUsers = systemUserService.getOrderedPage("loginName", false, startingRecNo);
            request.setAttribute(MENU_TOTAL_RECORDS, String.valueOf(systemUserService.getCount()));
        }
        List<UnifiedSystemUser> unifiedUsers = getUnifiedUsers(systemUsers);

        if (request.getParameter("filter") != null) {
            request.setAttribute(PAGE_SIZE, getPageSize());
            if (request.getParameter("filter").contains("isActive")) {
                request.setAttribute(IActionConstants.FILTER_CHECK_ACTIVE, "true");
                unifiedUsers = unifiedUsers.stream().filter(user -> user.getActive().equals("Y"))
                        .collect(Collectors.toList());
            }
            if (request.getParameter("filter").contains("isAdmin")) {
                request.setAttribute(IActionConstants.FILTER_CHECK_ADMIN, "true");
                unifiedUsers = filterUnifiedUsersByAdmin(unifiedUsers);
            }
        }
        if (StringUtils.isNotEmpty(request.getParameter("roleFilter"))) {
            request.setAttribute(IActionConstants.FILTER_ROLE, request.getParameter("roleFilter").toString());
            unifiedUsers = filterUnifiedUsersByLabUnitRole(unifiedUsers, request.getParameter("roleFilter"));
        }

        request.setAttribute("menuDefinition", "UnifiedSystemUserMenuDefinition");

        request.setAttribute(MENU_FROM_RECORD, String.valueOf(startingRecNo));

        int numOfRecs = 0;
        if (unifiedUsers != null) {
            numOfRecs = Math.min(unifiedUsers.size(), getPageSize());
            numOfRecs--;
        }
        int endingRecNo = startingRecNo + numOfRecs;
        request.setAttribute(MENU_TO_RECORD, String.valueOf(endingRecNo));

        request.setAttribute(MENU_SEARCH_BY_TABLE_COLUMN, "user.userSearch");

        if (YES.equals(request.getParameter("search"))) {
            request.setAttribute(IN_MENU_SELECT_LIST_HEADER_SEARCH, "true");
        }

        form.setToRecordCount(String.valueOf(endingRecNo));
        form.setFromRecordCount(String.valueOf(startingRecNo));
        form.setTotalRecordCount(String.valueOf(String.valueOf(systemUserService.getCount())));

        return unifiedUsers;
    }

    private List<UnifiedSystemUser> filterUnifiedUsersByAdmin(List<UnifiedSystemUser> users) {
        List<UnifiedSystemUser> unifiedUsers = new ArrayList<>();
        List<LoginUser> loginUsers = loginService.getAll();
        HashMap<String, LoginUser> loginMap = createLoginMap(loginUsers, true);

        for (UnifiedSystemUser user : users) {
            if (loginMap.containsKey(user.getLoginName())) {
                unifiedUsers.add(user);
            }
        }
        return unifiedUsers;
    }

    private List<UnifiedSystemUser> filterUnifiedUsersByLabUnitRole(List<UnifiedSystemUser> users, String labUnit) {
        List<UnifiedSystemUser> unifiedUsers = new ArrayList<>();
        List<UserLabUnitRoles> allLabUnitRoles = userService.getAllUserLabUnitRoles();
        List<Integer> systemUserIds = new ArrayList<>();
        if (allLabUnitRoles != null && allLabUnitRoles.size() > 0) {
            for (UserLabUnitRoles userRoles : allLabUnitRoles) {
                for (LabUnitRoleMap roleMap : userRoles.getLabUnitRoleMap()) {
                    if (roleMap.getLabUnit().trim().equals(labUnit.trim())) {
                        systemUserIds.add(userRoles.getId());
                        break;
                    }
                }
            }
            for (UnifiedSystemUser user : users) {
                if (systemUserIds.contains(Integer.valueOf(user.getSystemUserId()))) {
                    unifiedUsers.add(user);
                }
            }
        }
        return unifiedUsers;
    }

    private List<UnifiedSystemUser> getUnifiedUsers(List<SystemUser> systemUsers) {

        List<UnifiedSystemUser> unifiedUsers = new ArrayList<>();

        List<LoginUser> loginUsers = loginService.getAll();

        HashMap<String, LoginUser> loginMap = createLoginMap(loginUsers, false);

        for (SystemUser user : systemUsers) {
            UnifiedSystemUser unifiedUser = createUnifiedSystemUser(loginMap, user);
            unifiedUsers.add(unifiedUser);
        }

        return unifiedUsers;
    }

    private UnifiedSystemUser createUnifiedSystemUser(HashMap<String, LoginUser> loginMap, SystemUser user) {

        UnifiedSystemUser unifiedUser = new UnifiedSystemUser();
        unifiedUser.setFirstName(user.getFirstName());
        unifiedUser.setLastName(user.getLastName());
        unifiedUser.setLoginName(user.getLoginName());
        unifiedUser.setSystemUserId(user.getId());
        unifiedUser.setActive(user.getIsActive());

        LoginUser login = loginMap.get(user.getLoginName());

        if (login != null) {
            unifiedUser.setExpDate(DateUtil.formatDateAsText(login.getPasswordExpiredDate()));
            unifiedUser.setDisabled(login.getAccountDisabled());
            unifiedUser.setLocked(login.getAccountLocked());
            unifiedUser.setTimeout(login.getUserTimeOut());
            unifiedUser.setLoginUserId(Integer.toString(login.getId()));
        }
        return unifiedUser;
    }

    private HashMap<String, LoginUser> createLoginMap(List<LoginUser> loginUsers, Boolean filter) {
        HashMap<String, LoginUser> loginMap = new HashMap<>();

        for (LoginUser login : loginUsers) {
            if (filter) {
                if (login.getIsAdmin().equals("Y")) {
                    loginMap.put(login.getLoginName(), login);
                }
            } else {
                loginMap.put(login.getLoginName(), login);
            }
        }
        return loginMap;
    }

    @Override
    protected String getDeactivateDisabled() {
        return "false";
    }

    @Override
    protected int getPageSize() {
        return Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
    }

    @PostMapping(value = "/DeleteUnifiedSystemUser")
    public String showDeleteUnifiedSystemUser(HttpServletRequest request, @RequestBody UnifiedSystemUserMenuForm form,
            BindingResult result) {
        if (result.hasErrors()) {
            saveErrors(result);
            return findForward(FWD_FAIL_DELETE);
        }
        List<String> selectedIDs = form.getSelectedIDs();
        List<LoginUser> loginUsers = new ArrayList<>();
        List<SystemUser> systemUsers = new ArrayList<>();
        List<UserRole> userRoles = new ArrayList<>();

        String sysUserId = getSysUserId(request);

        for (int i = 0; i < selectedIDs.size(); i++) {
            String systemUserId = UnifiedSystemUser.getSystemUserIDFromCombinedID(selectedIDs.get(i));
            Integer loginUserId = UnifiedSystemUser.getLoginUserIDFromCombinedID(selectedIDs.get(i));

            LoginUser user = loginService.get(loginUserId);
            if (loginService.isUserAdmin(user)) {
                continue;
            }

            if (!GenericValidator.isBlankOrNull(systemUserId)) {
                SystemUser systemUser = new SystemUser();
                systemUser.setId(systemUserId);
                systemUser.setSysUserId(sysUserId);
                systemUsers.add(systemUser);
            }

            if (null != loginUserId) {
                LoginUser loginUser = new LoginUser();
                loginUser.setId(loginUserId);
                loginUser.setSysUserId(sysUserId);
                loginUsers.add(loginUser);
            }
        }

        for (SystemUser systemUser : systemUsers) {
            List<String> roleIds = userRoleService.getRoleIdsForUser(systemUser.getId());

            for (String roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setSystemUserId(systemUser.getId());
                userRole.setRoleId(roleId);
                userRole.setSysUserId(sysUserId);
                userRoles.add(userRole);
            }
        }

        try {
            unifiedSystemUserService.deleteData(userRoles, systemUsers, loginUsers, getSysUserId(request));
        } catch (LIMSRuntimeException e) {

            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                result.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else {
                result.reject("errors.DeleteException", "errors.DeleteException");
            }
            // saveErrors(result);
            return findLocalForward(FWD_FAIL_DELETE);

        }

        // return findForward(FWD_SUCCESS_DELETE, form);
        return findLocalForward(FWD_SUCCESS_DELETE);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "userMasterListsPageDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";
        } else if (FWD_SUCCESS_DELETE.equals(forward)) {
            return "redirect:/UnifiedSystemUserMenu";
        } else if (FWD_FAIL_DELETE.equals(forward)) {
            return "redirect:/UnifiedSystemUserMenu";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return "unifiedSystemUser.browser.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        return "unifiedSystemUser.browser.title";
    }
}
