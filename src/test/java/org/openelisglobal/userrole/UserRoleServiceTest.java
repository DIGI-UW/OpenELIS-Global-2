package org.openelisglobal.userrole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRoleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UserRoleService userRoleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/user-role.xml");
    }

    @Test
    public void testDataInDataBase() {
        List<UserRole> userRoles = userRoleService.getAll();
        userRoles.forEach(userRole -> {
            System.out.print(userRole.getSystemUserId() + " - " + userRole.getRoleId() + " ");
        });
    }

    @Test
    public void getRoleIdsForUser_shouldReturnAllRolesForUser() {
        List<String> roleIds = userRoleService.getRoleIdsForUser("1");
        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains("1"));
        assertTrue(roleIds.contains("4"));
    }

    @Test
    public void userInRole_withCollection_shouldReturnTrueWhenUserHasRole() {
        boolean hasRole = userRoleService.userInRole("1", Arrays.asList("Admin", "QA"));
        assertTrue(hasRole);
    }

    @Test
    public void userInRole_withCollection_shouldReturnFalseWhenUserDoesNotHaveRole() {
        boolean hasRole = userRoleService.userInRole("2", Arrays.asList("Admin"));
        assertFalse(hasRole);
    }

    @Test
    public void userInRole_withSingleRole_shouldReturnTrueWhenUserHasRole() {
        boolean hasRole = userRoleService.userInRole("1", "Admin");
        assertTrue(hasRole);
    }

    @Test
    public void userInRole_withSingleRole_shouldReturnFalseWhenUserDoesNotHaveRole() {
        boolean hasRole = userRoleService.userInRole("2", "Admin");
        assertFalse(hasRole);
    }

    @Test
    public void getUserIdsForRole_shouldReturnAllUserIdsForRole() {
        List<String> userIds = userRoleService.getUserIdsForRole("Admin");
        assertEquals(1, userIds.size());
        assertTrue(userIds.contains("1"));
    }
}