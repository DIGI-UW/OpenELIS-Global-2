package org.openelisglobal.systemuser.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;

public class UserServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UserService userService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private LoginUserService loginUserService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/user-service.xml");
    }

    @Test
    public void updateLoginUser_shouldCreateNewUserAndRoles() {
        SystemUser newSystemUser = new SystemUser();
        newSystemUser.setLoginName("newuser");
        newSystemUser.setFirstName("New");
        newSystemUser.setLastName("User");
        newSystemUser.setIsActive("Y");
        newSystemUser.setIsEmployee("Y");

        LoginUser newLoginUser = new LoginUser();
        newLoginUser.setLoginName("newuser");
        newLoginUser.setPassword("password123");
        newLoginUser.setIsAdmin("N");
        newLoginUser.setAccountDisabled("N");
        newLoginUser.setAccountLocked("N");
        newLoginUser.setPasswordExpiredDate(new java.sql.Date(System.currentTimeMillis()));
        newLoginUser.setUserTimeOut("30");

        List<String> roles = new ArrayList<>();
        roles.add("2");

        userService.updateLoginUser(newLoginUser, true, newSystemUser, true, roles, "100");

        SystemUser savedSystemUser = systemUserService.getDataForLoginUser("newuser");
        assertEquals("New", savedSystemUser.getFirstName());
        assertEquals("User", savedSystemUser.getLastName());
        assertEquals("Y", savedSystemUser.getIsActive());
        assertEquals("Y", savedSystemUser.getIsEmployee());

        LoginUser savedLoginUser = loginUserService.getUserProfile("newuser");
        assertEquals("newuser", savedLoginUser.getLoginName());
        assertEquals("N", savedLoginUser.getIsAdmin());
        assertEquals("N", savedLoginUser.getAccountDisabled());
        assertEquals("N", savedLoginUser.getAccountLocked());
        assertEquals("30", savedLoginUser.getUserTimeOut());
        assertTrue("Password expired date should be set", savedLoginUser.getPasswordExpiredDate() != null);
    }

    @Test
    public void updateLoginUser_shouldUpdateExistingUserAndRoles() {
        SystemUser existingSystemUser = systemUserService.get("101");
        existingSystemUser.setFirstName("Johnny");

        LoginUser existingLoginUser = loginUserService.get(Integer.valueOf(101));
        existingLoginUser.setIsAdmin("Y");

        List<String> roles = new ArrayList<>();
        roles.add("1");

        userService.updateLoginUser(existingLoginUser, false, existingSystemUser, false, roles, "100");

        SystemUser updatedSystemUser = systemUserService.get("101");
        assertEquals("Johnny", updatedSystemUser.getFirstName());
        assertEquals("Doe", updatedSystemUser.getLastName());

        LoginUser updatedLoginUser = loginUserService.get(Integer.valueOf(101));
        assertEquals("Y", updatedLoginUser.getIsAdmin());
        assertEquals("jdoe", updatedLoginUser.getLoginName());
    }

    @Test
    public void getUserLabUnitRoles_shouldReturnLabUnitRoles() {
        SystemUser user102 = systemUserService.get("102");
        Map<String, Set<String>> selectedLabUnitRolesMap = new HashMap<>();
        Set<String> assignedRoles = new HashSet<>();
        assignedRoles.add("2");
        selectedLabUnitRolesMap.put("chemistry", assignedRoles);
        userService.saveUserLabUnitRoles(user102, selectedLabUnitRolesMap, "100");

        UserLabUnitRoles roles = userService.getUserLabUnitRoles("102");
        assertEquals("Should have exactly one lab unit role map", 1, roles.getLabUnitRoleMap().size());

        LabUnitRoleMap roleMap = roles.getLabUnitRoleMap().iterator().next();
        assertEquals("Lab unit should be chemistry", "chemistry", roleMap.getLabUnit());
        assertEquals("Should have exactly one role mapped", 1, roleMap.getRoles().size());
        assertTrue("Role 2 (labTech) should be mapped", roleMap.getRoles().contains("2"));
    }

    @Test
    public void saveUserLabUnitRoles_shouldSaveAndRetrieveLabUnitRoles() {
        SystemUser user102 = systemUserService.get("102");

        Map<String, Set<String>> selectedLabUnitRolesMap = new HashMap<>();
        Set<String> roles = new HashSet<>();
        roles.add("2");
        selectedLabUnitRolesMap.put("hematology", roles);

        userService.saveUserLabUnitRoles(user102, selectedLabUnitRolesMap, "100");

        UserLabUnitRoles userRoles = userService.getUserLabUnitRoles("102");
        assertEquals("User ID should be 102", Integer.valueOf(102), userRoles.getId());
        assertEquals("Should have exactly one lab unit role map", 1, userRoles.getLabUnitRoleMap().size());

        LabUnitRoleMap savedRoleMap = userRoles.getLabUnitRoleMap().iterator().next();
        assertEquals("Lab unit should be hematology", "hematology", savedRoleMap.getLabUnit());
        assertEquals("Should have exactly one role mapped to lab unit", 1, savedRoleMap.getRoles().size());
        assertTrue("Role 2 (labTech) should be in the mapped roles", savedRoleMap.getRoles().contains("2"));
    }

    @Test
    public void getAllUserLabUnitRoles_shouldReturnAllMappings() {
        List<UserLabUnitRoles> allRoles = userService.getAllUserLabUnitRoles();
        assertFalse("Role list should not be empty", allRoles.isEmpty());
        assertEquals("Should return exactly 1 role mapping from the DBUnit fixture", 1, allRoles.size());

        UserLabUnitRoles jdoeRoles = allRoles.get(0);
        assertEquals("The seeded role should belong to jdoe (id=101)", Integer.valueOf(101), jdoeRoles.getId());
        assertTrue("Seeded lab unit role map collection should be empty (no join rows seeded)",
                jdoeRoles.getLabUnitRoleMap().isEmpty());
    }
}
