package org.openelisglobal.systemuser.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.validation.PasswordValidationFactory;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.validator.BaseErrors;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.role.action.bean.DisplayRole;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.form.UnifiedSystemUserForm;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.validator.UnifiedSystemUserFormValidator;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.systemuser.valueholder.UnifiedSystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UnifiedSystemUserController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "systemUserId", "loginUserId", "userLoginName",
            "userPassword", "confirmPassword", "userFirstName", "userLastName", "expirationDate", "timeout",
            "accountLocked", "accountDisabled", "accountActive", "selectedRoles*", "selectedLabUnitRoles",
            "testSectionId", "systemUsers", "systemUserIdToCopy", "allowCopyUserRoles" };

    @Autowired
    private UnifiedSystemUserFormValidator formValidator;

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
    private static final String RESERVED_ADMIN_NAME = "admin";

    private static String GLOBAL_ADMIN_ID;
    private static String ID;
    public static final char DEFAULT_OBFUSCATED_CHARACTER = '@';
    public static final String ALL_LAB_UNITS = "AllLabUnits";

    @PostConstruct
    private void initialize() {
        GLOBAL_ADMIN_ID = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN).getId();
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

    @RequestMapping(value = "/UnifiedSystemUser", method = RequestMethod.POST)
    public ModelAndView showUpdateUnifiedSystemUser(HttpServletRequest request,
            @ModelAttribute("form") @Valid UnifiedSystemUserForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        boolean doFiltering = true;
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupRoles(form, request, doFiltering);
            return findForward(FWD_FAIL_INSERT, form);
        }

        request.setAttribute(ALLOW_EDITS_KEY, "true");
        request.setAttribute(PREVIOUS_DISABLED, "false");
        request.setAttribute(NEXT_DISABLED, "false");

        if (form.getUserLoginName() != null) {
            form.setUserLoginName(form.getUserLoginName().trim());
        } else {
            form.setUserLoginName("");
        }

        String forward = validateAndUpdateSystemUser(request, form);

        if (forward.equals(FWD_SUCCESS_INSERT)) {
            redirectAttributes.addFlashAttribute(FWD_SUCCESS, true);
            Map<String, String> params = new HashMap<>();
            params.put("forward", FWD_SUCCESS);
            params.put("ID", ID);
            return getForwardWithParameters(findForward(forward, form), params);
        } else {
            setupRoles(form, request, doFiltering);
            return findForward(forward, form);
        }
    }

    private String validateAndUpdateSystemUser(HttpServletRequest request, UnifiedSystemUserForm form) {
        String loginUserId = form.getLoginUserId();
        String systemUserId = form.getSystemUserId();

        Errors errors = new BaseErrors();

        boolean loginUserNew = GenericValidator.isBlankOrNull(loginUserId);
        boolean systemUserNew = GenericValidator.isBlankOrNull(systemUserId);
        boolean passwordUpdated = false;

        passwordUpdated = passwordHasBeenUpdated(loginUserNew, form);
        validateUser(form, errors, loginUserNew, passwordUpdated, loginUserId);

        if (errors.hasErrors()) {
            saveErrors(errors);
            return FWD_FAIL_INSERT;
        }

        String loggedOnUserId = getSysUserId(request);

        LoginUser loginUser = createLoginUser(form, loginUserId, loginUserNew, passwordUpdated, loggedOnUserId);
        SystemUser systemUser = createSystemUser(form, systemUserId, systemUserNew, loggedOnUserId);
        try {
            if (form.getAllowCopyUserRoles().equals(NO)) {
                userService.updateLoginUser(loginUser, loginUserNew, systemUser, systemUserNew, form.getSelectedRoles(),
                        loggedOnUserId);
                saveUserLabUnitRoles(systemUser, form, loggedOnUserId);
            } else if (form.getAllowCopyUserRoles().equals(YES)) {
                if (StringUtils.isNotBlank(form.getSystemUserIdToCopy().trim())) {
                    String globalParentRoleId = roleService.getRoleByName(Constants.GLOBAL_ROLES_GROUP).getId();
                    List<String> globaRolesIds = getAllRoles().stream().filter(role -> role.getGroupingParent() != null)
                            .filter(role -> role.getGroupingParent().equals(globalParentRoleId))
                            .map(role -> role.getId()).collect(Collectors.toList());
                    List<String> copiedRoleIds = userRoleService.getRoleIdsForUser(form.getSystemUserIdToCopy().trim());
                    List<String> globalCopiedRoleIds = copiedRoleIds.stream()
                            .filter(role -> globaRolesIds.contains(role)).collect(Collectors.toList());

                    userService.updateLoginUser(loginUser, loginUserNew, systemUser, systemUserNew, globalCopiedRoleIds,
                            loggedOnUserId);

                    UserLabUnitRoles labRoles = userService.getUserLabUnitRoles(form.getSystemUserIdToCopy().trim());
                    Map<String, Set<String>> copiedLabUnitRolesMap = new HashMap<>();
                    labRoles.getLabUnitRoleMap().forEach(roleMap -> copiedLabUnitRolesMap
                            .put(new String(roleMap.getLabUnit()), new HashSet<>(roleMap.getRoles())));
                    userService.saveUserLabUnitRoles(systemUser, copiedLabUnitRolesMap, loggedOnUserId);
                }
            }
            ID = systemUser.getId() + "-" + loginUser.getId();
        } catch (LIMSRuntimeException e) {
            if (e.getCause() instanceof org.hibernate.StaleObjectStateException) {
                errors.reject("errors.OptimisticLockException", "errors.OptimisticLockException");
            } else if (e.getCause() instanceof LIMSDuplicateRecordException) {
                errors.reject("errors.DuplicateRecordException", "errors.DuplicateRecordException");
            } else {
                errors.reject("errors.UpdateException", "errors.UpdateException");
            }

            saveErrors(errors);
            disableNavigationButtons(request);
            return FWD_FAIL_INSERT;
        }

        return FWD_SUCCESS_INSERT;
    }

    private boolean passwordHasBeenUpdated(boolean loginUserNew, UnifiedSystemUserForm form) {
        if (loginUserNew) {
            return true;
        }

        String password = form.getUserPassword();

        return !StringUtil.containsOnly(password, DEFAULT_OBFUSCATED_CHARACTER);
    }

    private void validateUser(UnifiedSystemUserForm form, Errors errors, boolean loginUserIsNew,
            boolean passwordUpdated, String loginUserId) {
        boolean checkForDuplicateName = loginUserIsNew || userNameChanged(loginUserId, form.getUserLoginName());
        // check login name

        if (GenericValidator.isBlankOrNull(form.getUserLoginName())) {
            errors.reject("errors.loginName.required", "errors.loginName.required");
        } else if (checkForDuplicateName) {
            LoginUser login = loginService.getMatch("loginName", form.getUserLoginName()).orElse(null);
            if (login != null) {
                errors.reject("errors.loginName.duplicated");
            }
        }

        // check first and last name
        if (GenericValidator.isBlankOrNull(form.getUserFirstName())
                || GenericValidator.isBlankOrNull(form.getUserLastName())) {
            errors.reject("errors.userName.required", "errors.userName.required");
        }

        if (passwordUpdated) {
            // check passwords match
            if (GenericValidator.isBlankOrNull(form.getUserPassword())
                    || !form.getUserPassword().equals(form.getConfirmPassword())) {
                errors.reject("errors.password.match", "errors.password.match");
            } else if (!passwordValid(form.getUserPassword())) { // validity
                errors.reject("login.error.password.requirement");
            }
        }

        // check timeout
        if (!timeoutValidAndInRange(form.getTimeout())) {
            errors.reject("errors.timeout.range", "errors.timeout.range");
        }
    }

    private boolean userNameChanged(String loginUserId, String newName) {
        if (GenericValidator.isBlankOrNull(loginUserId)) {
            return false;
        }

        LoginUser login = loginService.get(Integer.parseInt(loginUserId));

        return !newName.equals(login.getLoginName());
    }

    private boolean timeoutValidAndInRange(String timeout) {
        try {
            int timeInMin = Integer.parseInt(timeout);
            return timeInMin > 0 && timeInMin < 601;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean passwordValid(String password) {
        return PasswordValidationFactory.getPasswordValidator().passwordValid(password);
    }

    private LoginUser createLoginUser(UnifiedSystemUserForm form, String loginUserId, boolean loginUserNew,
            boolean passwordUpdated, String loggedOnUserId) {

        LoginUser login = new LoginUser();

        if (!loginUserNew) {
            login = loginService.get(Integer.parseInt(form.getLoginUserId()));
        }
        login.setAccountDisabled(form.getAccountDisabled());
        login.setAccountLocked(form.getAccountLocked());
        login.setLoginName(form.getUserLoginName());
        if (passwordUpdated) {
            login.setPassword(form.getUserPassword());
            loginService.hashPassword(login, login.getPassword());
        }
        login.setPasswordExpiredDateForDisplay(form.getExpirationDate());
        if (RESERVED_ADMIN_NAME.equals(form.getUserLoginName())) {
            login.setIsAdmin("Y");
        } else {
            login.setIsAdmin("N");
        }
        login.setUserTimeOut(form.getTimeout());
        login.setSysUserId(loggedOnUserId);

        return login;
    }

    private SystemUser createSystemUser(UnifiedSystemUserForm form, String systemUserId, boolean systemUserNew,
            String loggedOnUserId) {

        SystemUser systemUser = new SystemUser();

        if (!systemUserNew) {
            systemUser = systemUserService.get(systemUserId);
        }

        systemUser.setFirstName(form.getUserFirstName());
        systemUser.setLastName(form.getUserLastName());
        systemUser.setLoginName(form.getUserLoginName());
        systemUser.setIsActive(form.getAccountActive());
        systemUser.setIsEmployee("Y");
        systemUser.setExternalId("1");
        String initial = systemUser.getFirstName().substring(0, 1) + systemUser.getLastName().substring(0, 1);
        systemUser.setInitials(initial);
        systemUser.setSysUserId(loggedOnUserId);

        return systemUser;
    }

    private void disableNavigationButtons(HttpServletRequest request) {
        request.setAttribute(PREVIOUS_DISABLED, TRUE);
        request.setAttribute(NEXT_DISABLED, TRUE);
    }

    private void saveUserLabUnitRoles(SystemUser user, UnifiedSystemUserForm form, String loggedOnUserId) {
        Map<String, Set<String>> selectedLabUnitRolesMap = new HashMap<>();
        parseUserLabRolesData(form, selectedLabUnitRolesMap);
        userService.saveUserLabUnitRoles(user, selectedLabUnitRolesMap, loggedOnUserId);
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

    private void parseUserLabRolesData(UnifiedSystemUserForm form, Map<String, Set<String>> selectedLabUnitRolesMap) {
        /**
         * The ui-form can dynamically render more fields that are mapped to the same
         * field (ie for testSectionId ,selectedLabUnitRoles )of the form-backing
         * object, so we append a common pre-fix to the values in every New lab Unit
         * Roles Set to identify the distinct values for each set . This method parses
         * the data from the form ,and builds a Map ie <String testSectionId
         * ,Set<LabUnitRoles>> for each Lab Unit Role Set based on the suffix appended
         * ie testSectionId = "1=2,2=5,2=6" ,selectedLabUnitRoles = [1=56, 2=36 ,3=44]
         */
        String labUnitEntryMapString = form.getTestSectionId();
        List<String> labUnitsRolesEntryMaps = form.getSelectedLabUnitRoles();

        List<String> entries = new ArrayList<>();
        List<String> labUnitsEntryMap = new ArrayList<>();
        String labUnitEntries[] = labUnitEntryMapString.split(",");
        for (String part : labUnitEntries) {
            if (part.contains("=")) {
                if (!part.contains("none")) {
                    entries.add(part.split("=")[0]);
                    labUnitsEntryMap.add(part);
                }
            }
        }
        for (String entry : entries) {
            for (String labUnit : labUnitsEntryMap) {
                if (labUnit.startsWith(entry)) {
                    Set<String> labRolesId = new HashSet<>();
                    String labUnitId = labUnit.split("=")[1];
                    for (String labUnitsRolesEntryMap : labUnitsRolesEntryMaps) {
                        if (labUnitsRolesEntryMap.startsWith(entry)) {
                            labRolesId.add(labUnitsRolesEntryMap.split("=")[1]);
                        }
                    }
                    if (labRolesId.size() > 0) {
                        selectedLabUnitRolesMap.put(labUnitId, labRolesId);
                    }
                }
            }
        }

        if (selectedLabUnitRolesMap.containsKey(ALL_LAB_UNITS)) {
            Set<String> labRolesId = selectedLabUnitRolesMap.get(ALL_LAB_UNITS);
            selectedLabUnitRolesMap.clear();
            selectedLabUnitRolesMap.put(ALL_LAB_UNITS, labRolesId);
            List<String> allTestSectionIds = new ArrayList<>();
            DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE)
                    .forEach(testScetion -> allTestSectionIds.add(testScetion.getId()));
            allTestSectionIds.forEach(testScetionId -> selectedLabUnitRolesMap.put(testScetionId, labRolesId));
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
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/UnifiedSystemUser";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "unifiedSystemUserDefinition";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        String id = request.getParameter(ID);
        boolean isNew = GenericValidator.isBlankOrNull(id) || "0".equals(id);
        return isNew ? "unifiedSystemUser.add.title" : "unifiedSystemUser.edit.title";
    }

    @Override
    protected String getPageSubtitleKey() {
        String id = request.getParameter(ID);
        boolean isNew = GenericValidator.isBlankOrNull(id) || "0".equals(id);
        return isNew ? "unifiedSystemUser.add.title" : "unifiedSystemUser.edit.title";
    }
}
