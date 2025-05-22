package org.openelisglobal.systemUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UnifiedSystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;

public class UnifiedSystemUserServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UnifiedSystemUserService unifiedSystemUserService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private LoginUserService loginUserService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/system-user.xml");
    }

    @Test
    public void deleteData() {
        // showing exiting system users
        List<SystemUser> systemUsers = systemUserService.getAll();

        assertEquals("John", systemUsers.get(1).getFirstName());
        assertEquals("Alice", systemUsers.get(2).getFirstName());
        assertEquals("Bob", systemUsers.get(3).getFirstName());

        // showing existing user roles
        List<UserRole> userRoles = userRoleService.getAll();

        assertEquals("1", userRoles.get(0).getRoleId());
        assertEquals("2", userRoles.get(1).getRoleId());
        assertEquals("3", userRoles.get(2).getRoleId());

        // showing existing login users
        List<LoginUser> loginUsers = loginUserService.getAll();
        assertEquals("jdoe", loginUsers.get(5).getLoginName());
        assertEquals("asmith", loginUsers.get(6).getLoginName());
        assertEquals("bwhite", loginUsers.get(7).getLoginName());

        /*
         * unifiedSytemUser service method deleting a system user this deletes all
         * existing login users and user roles and the system user whose id is used
         */

        unifiedSystemUserService.deleteData(userRoles, systemUsers, loginUsers, "3");

        List<SystemUser> systemUsersd = systemUserService.getAll();
        systemUsersd.forEach(user -> {
            System.out.print(user.getLoginName() + " ");
        });

        assertEquals("John", systemUsersd.get(1).getFirstName());
        assertEquals("Alice", systemUsersd.get(2).getFirstName());
        assertEquals("Bob", systemUsersd.get(3).getFirstName());

        // expect no exiting user after method invocation
        List<UserRole> userRolesd = userRoleService.getAll();
        assertTrue(userRolesd.size() == 0);

        // expect no login users after delete method invocation
        List<LoginUser> loginUsersd = loginUserService.getAll();
        assertTrue(loginUsersd.size() == 0);

    }

}
