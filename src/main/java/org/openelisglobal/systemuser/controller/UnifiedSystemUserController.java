package org.openelisglobal.systemuser.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.role.action.bean.DisplayRole;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.form.UnifiedSystemUserForm;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemuser.valueholder.UnifiedSystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UnifiedSystemUserController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "systemUserId", "loginUserId", "userLoginName",
            "userPassword", "confirmPassword", "userFirstName", "userLastName", "expirationDate", "timeout",
            "accountLocked", "accountDisabled", "accountActive", "selectedRoles*", "selectedLabUnitRoles",
            "testSectionId", "systemUsers", "systemUserIdToCopy", "allowCopyUserRoles" };

    @Autowired
    private LoginUserService loginService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private UserModuleService userModuleService;
    @Autowired
    private UserService userService;

    private static String GLOBAL_ADMIN_ID;
    public static final char DEFAULT_OBFUSCATED_CHARACTER = '@';
    public static final String ALL_LAB_UNITS = "AllLabUnits";

    @PostConstruct
    private void initialize() {
        Role globalAdmin = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN);
        if (globalAdmin == null) {
            // Some DB seeds use "Admin" instead of "Global Administrator".
            // Don't crash the entire app on startup; fall back to legacy role name.
            LogEvent.logWarn(this.getClass().getSimpleName(), "initialize",
                    "Role '" + Constants.ROLE_GLOBAL_ADMIN + "' not found; falling back to 'Admin'");
            globalAdmin = roleService.getRoleByName("Admin");
        }

        if (globalAdmin == null) {
            LogEvent.logError(this.getClass().getSimpleName(), "initialize",
                    "No suitable global admin role found (expected '" + Constants.ROLE_GLOBAL_ADMIN + "' or 'Admin')");
            GLOBAL_ADMIN_ID = null;
            return;
        }

        GLOBAL_ADMIN_ID = globalAdmin.getId();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/UnifiedSystemUser", method = RequestMethod.GET)
    public ModelAndView showUnifiedSystemUser(HttpServletRequest request,
            @RequestParam(name = "ID", defaultValue = "") String id)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        UnifiedSystemUserForm form = new UnifiedSystemUserForm();
        form.setFormAction("UnifiedSystemUser");
        form.setCancelAction("UnifiedSystemUserMenu");

        boolean doFiltering = true;
        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "true");
        request.setAttribute(NEXT_DISABLED, "true");
        request.setAttribute(DISPLAY_PREV_NEXT, false);

        boolean isNew = GenericValidator.isBlankOrNull(id) || "0".equals(id);

        setDefaultProperties(form);
        if (!isNew) {
            setPropertiesForExistingUser(form, id, doFiltering);
        }
        setupRoles(form, request, doFiltering);

        // load testSections for drop down
        List<IdValuePair> testSections = DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE);
        form.setTestSections(testSections);
        form.setSystemUsers(getDisplaySystemUsersJsonArray());
        addFlashMsgsToRequest(request);
        return findForward(FWD_SUCCESS, form);
    }

    private void setupRoles(UnifiedSystemUserForm form, HttpServletRequest request, boolean doFiltering) {
        List<Role> roles = getAllRoles();
        doFiltering &= !userModuleService.isUserAdmin(request);

        if (doFiltering) {
            roles = doRoleFiltering(roles, getSysUserId(request));
        }

        List<DisplayRole> displayRoles = convertToDisplayRoles(roles);
        displayRoles = sortAndGroupRoles(displayRoles);
        String globalParentRoleId = roleService.getRoleByName(Constants.GLOBAL_ROLES_GROUP).getId();
        String labUnitRoleId = roleService.getRoleByName(Constants.LAB_ROLES_GROUP).getId();

        List<DisplayRole> globalRoles = displayRoles.stream().filter(role -> role.getParentRole() != null)
                .filter(role -> role.getParentRole().equals(globalParentRoleId)).collect(Collectors.toList());
        List<DisplayRole> labUnitRoles = displayRoles.stream().filter(role -> role.getParentRole() != null)
                .filter(role -> role.getParentRole().equals(labUnitRoleId)).collect(Collectors.toList());
        form.setGlobalRoles(globalRoles);
        form.setLabUnitRoles(labUnitRoles);
    }

    private List<DisplayRole> convertToDisplayRoles(List<Role> roles) {
        int elementCount = 0;

        List<DisplayRole> displayRoles = new ArrayList<>();

        for (Role role : roles) {
            elementCount++;
            displayRoles.add(convertToDisplayRole(role, elementCount));
        }

        return displayRoles;
    }

    private DisplayRole convertToDisplayRole(Role role, int count) {
        DisplayRole displayRole = new DisplayRole();

        displayRole.setRoleName(role.getLocalizedName());
        displayRole.setElementID(String.valueOf(count));
        displayRole.setRoleId(role.getId());
        displayRole.setGroupingRole(role.getGroupingRole());
        displayRole.setParentRole(role.getGroupingParent());

        return displayRole;
    }

    private List<DisplayRole> sortAndGroupRoles(List<DisplayRole> roles) {
        /*
         * The sorting we want to end up with is first alphabetical and then by groups
         * What makes things a little more difficult is that we may have roles which
         * have parents which don't exist, we shouldn't but we might. So... First sweep
         * is to find all the orphaned roles and set their parents to null Then move all
         * the first generation groups to a new list. Then scan for all for all groups
         * and move their members, repeat until the first list is empty, which is why we
         * didn't want orphans. Lastly we will add the role ID as a child to all of it's
         * parents
         */

        Collections.sort(roles, new Comparator() {
            @Override
            public int compare(Object obj1, Object obj2) {
                DisplayRole role1 = (DisplayRole) obj1;
                DisplayRole role2 = (DisplayRole) obj2;
                return role1.getRoleName().toUpperCase().compareTo(role2.getRoleName().toUpperCase());
            }
        });

        /*
         * The reason we're not making a map is that we want to preserve the order
         * during this whole process
         */
        List<String> groupIds = new ArrayList<>();

        for (DisplayRole role : roles) {
            if (role.isGroupingRole()) {
                groupIds.add(role.getRoleId());
            }
        }

        for (DisplayRole role : roles) {
            if (!GenericValidator.isBlankOrNull(role.getParentRole()) && !groupIds.contains(role.getParentRole())) {
                role.setParentRole(null);
            }
        }

        List<DisplayRole> mergeList = new ArrayList<>();
        List<DisplayRole> currentWorkingList = new ArrayList<>();
        List<DisplayRole> unplacedList = new ArrayList<>();

        for (DisplayRole role : roles) {
            if (GenericValidator.isBlankOrNull(role.getParentRole())) {
                role.setNestingLevel(0);
                currentWorkingList.add(role);
            } else {
                unplacedList.add(role);
            }
        }

        int indentCount = 0;
        while (unplacedList.size() > 0) {
            indentCount++;
            for (DisplayRole placedRole : currentWorkingList) {
                mergeList.add(placedRole);

                if (placedRole.isGroupingRole()) {
                    List<DisplayRole> removeList = new ArrayList<>();
                    for (DisplayRole unplacedRole : unplacedList) {
                        if (unplacedRole.getParentRole().equals(placedRole.getRoleId())) {
                            unplacedRole.setNestingLevel(indentCount);
                            mergeList.add(unplacedRole);
                            removeList.add(unplacedRole);
                            placedRole.addChildID(unplacedRole.getRoleId());
                        }
                    }
                    unplacedList.removeAll(removeList);
                }
            }

            currentWorkingList = mergeList;
            mergeList = new ArrayList<>();
        }

        /*
         * For finding all parents we are going to iterate backwards since all parents
         * are in front of children role
         */
        for (int i = currentWorkingList.size() - 1; i > 0; i--) {
            DisplayRole role = currentWorkingList.get(i);

            if (!GenericValidator.isBlankOrNull(role.getParentRole())) {
                String roleID = role.getRoleId();
                String currentParentID = role.getParentRole();

                for (int parent = i - 1; parent >= 0; parent--) {
                    if (currentWorkingList.get(parent).getRoleId().equals(currentParentID)) {
                        DisplayRole parentRole = currentWorkingList.get(parent);

                        parentRole.addChildID(roleID);

                        if (GenericValidator.isBlankOrNull(parentRole.getParentRole())) {
                            break;
                        } else {
                            currentParentID = parentRole.getParentRole();
                        }
                    }
                }
            }
        }

        return currentWorkingList;
    }

    private List<Role> doRoleFiltering(List<Role> roles, String loggedInUserId) {

        List<String> rolesForLoggedInUser = userRoleService.getRoleIdsForUser(loggedInUserId);

        if (!rolesForLoggedInUser.contains(GLOBAL_ADMIN_ID)) {
            List<Role> tmpRoles = new ArrayList<>();

            for (Role role : roles) {
                if (!GLOBAL_ADMIN_ID.equals(role.getId())) {
                    tmpRoles.add(role);
                }
            }

            roles = tmpRoles;
        }

        return roles;
    }

    private void setDefaultProperties(UnifiedSystemUserForm form)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        String expireDate = getYearsFromNow(10);
        form.setExpirationDate(expireDate);
        form.setTimeout("480");
        form.setSystemUserLastupdated(new Timestamp(System.currentTimeMillis()));
    }

    private void setPropertiesForExistingUser(UnifiedSystemUserForm form, String id, boolean doFiltering)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        LoginUser login = getLoginFromCombinedId(id);
        SystemUser systemUser = getSystemUserFromCombinedId(id);

        if (login != null) {
            String proxyPassword = getProxyPassword(login);
            form.setLoginUserId(Integer.toString(login.getId()));
            form.setAccountDisabled(login.getAccountDisabled());
            form.setAccountLocked(login.getAccountLocked());
            form.setUserLoginName(login.getLoginName());
            form.setUserPassword(proxyPassword);
            form.setConfirmPassword(proxyPassword);
            form.setExpirationDate(login.getPasswordExpiredDateForDisplay());
            form.setTimeout(login.getUserTimeOut());
        }

        if (systemUser != null) {
            form.setSystemUserId(systemUser.getId());
            form.setUserFirstName(systemUser.getFirstName());
            form.setUserLastName(systemUser.getLastName());
            form.setAccountActive(systemUser.getIsActive());
            form.setSystemUserLastupdated(systemUser.getLastupdated());

            List<String> roleIds = userRoleService.getRoleIdsForUser(systemUser.getId());
            form.setSelectedRoles(roleIds);
            setLabunitRolesForExistingUser(form);

            // is this meant to be returned?
            // doFiltering = !roleIds.contains(MAINTENANCE_ADMIN_ID);
        }
    }

    private String getProxyPassword(LoginUser login) {
        char[] chars = new char[9];
        Arrays.fill(chars, DEFAULT_OBFUSCATED_CHARACTER);
        return new String(chars);
        // return StringUtil.replaceAllChars(login.getPassword(),
        // DEFAULT_PASSWORD_FILLER);
    }

    private LoginUser getLoginFromCombinedId(String id) {
        LoginUser login = null;
        Integer loginId = UnifiedSystemUser.getLoginUserIDFromCombinedID(id);

        if (null != loginId) {
            login = loginService.get(loginId);
        }

        return login;
    }

    private SystemUser getSystemUserFromCombinedId(String id) {
        SystemUser systemUser = null;
        String systemUserId = UnifiedSystemUser.getSystemUserIDFromCombinedID(id);

        if (!GenericValidator.isBlankOrNull(systemUserId)) {
            systemUser = systemUserService.get(systemUserId);
        }

        return systemUser;
    }

    private String getYearsFromNow(int years) {
        Calendar today = Calendar.getInstance();

        today.add(Calendar.YEAR, years);

        return DateUtil.formatDateAsText(today.getTime());
    }

    private List<Role> getAllRoles() {
        return roleService.getAllActiveRoles();
    }

    private void setLabunitRolesForExistingUser(UnifiedSystemUserForm form) {
        UserLabUnitRoles roles = userService.getUserLabUnitRoles(form.getSystemUserId());
        if (roles != null) {
            Set<LabUnitRoleMap> roleMaps = roles.getLabUnitRoleMap();
            List<String> userLabUnits = new ArrayList<>();
            roleMaps.forEach(map -> userLabUnits.add(map.getLabUnit()));
            JSONObject userLabData = new JSONObject();
            if (userLabUnits.contains(ALL_LAB_UNITS)) {
                roleMaps.stream().filter(map -> map.getLabUnit().equals(ALL_LAB_UNITS)).forEach(
                        map -> userLabData.put(map.getLabUnit(), map.getRoles().stream().collect(Collectors.toList())));
            } else {
                for (LabUnitRoleMap map : roleMaps) {
                    userLabData.put(map.getLabUnit(), map.getRoles().stream().collect(Collectors.toList()));
                }
            }

            form.setUserLabRoleData(userLabData);
        }
    }

    private JSONArray getDisplaySystemUsersJsonArray() {
        JSONArray displayUsers = new JSONArray();
        systemUserService.getAll().stream().filter(user -> user.getIsActive().equals(YES))
                .map(user -> new JSONObject()
                        .put("label", user.getLoginName() + " | " + user.getFirstName() + " " + user.getLastName())
                        .put("value", user.getId()))
                .forEach(userJson -> displayUsers.put(userJson));
        return displayUsers;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "unifiedSystemUserDefinition";
        } else if (FWD_FAIL.equals(forward)) {
            return "redirect:/MasterListsPage";

        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        String id = request.getParameter("ID");
        boolean isNew = GenericValidator.isBlankOrNull(id) || "0".equals(id);
        return isNew ? "unifiedSystemUser.add.title" : "unifiedSystemUser.edit.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        String id = request.getParameter("ID");
        boolean isNew = GenericValidator.isBlankOrNull(id) || "0".equals(id);
        return isNew ? "unifiedSystemUser.add.title" : "unifiedSystemUser.edit.title";
    }
}
