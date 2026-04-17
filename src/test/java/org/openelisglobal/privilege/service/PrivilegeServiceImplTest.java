package org.openelisglobal.privilege.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.constants.Privileges;
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;

@RunWith(MockitoJUnitRunner.class)
public class PrivilegeServiceImplTest {

    @Mock
    private PrivilegeDAO privilegeDAO;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private PrivilegeServiceImpl privilegeService;

    private Role receptionRole;
    private Role resultsRole;
    private Role validationRole;
    private Role globalAdminRole;

    private Privilege orderCreate;
    private Privilege resultView;
    private Privilege resultEnter;
    private Privilege resultValidate;

    @Before
    public void setup() {
        receptionRole = new Role();
        receptionRole.setId(1);
        receptionRole.setName(Constants.ROLE_RECEPTION);

        resultsRole = new Role();
        resultsRole.setId(2);
        resultsRole.setName(Constants.ROLE_RESULTS);

        validationRole = new Role();
        validationRole.setId(3);
        validationRole.setName(Constants.ROLE_VALIDATION);

        globalAdminRole = new Role();
        globalAdminRole.setId(10);
        globalAdminRole.setName(Constants.ROLE_GLOBAL_ADMIN);

        orderCreate = privilege("order:create");
        resultView = privilege("result:view");
        resultEnter = privilege("result:enter");
        resultValidate = privilege("result:validate");
    }

    private Privilege privilege(String name) {
        Privilege p = new Privilege();
        p.setName(name);
        p.setActive(true);
        return p;
    }

    // --- resolveAllPrivilegesForRole ---

    @Test
    public void resolveAllPrivilegesForRole_directOnly_returnsDirectPrivileges() {
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate));

        Set<String> result = privilegeService.resolveAllPrivilegesForRole("1");

        assertEquals(1, result.size());
        assertTrue(result.contains("order:create"));
    }

    @Test
    public void resolveAllPrivilegesForRole_withParent_includesParentPrivileges() {
        // Results inherits from Reception
        resultsRole.setGroupingParent(1);

        when(roleService.getRoleById(2)).thenReturn(resultsRole);
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(2)).thenReturn(Arrays.asList(resultView, resultEnter));
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate));

        Set<String> result = privilegeService.resolveAllPrivilegesForRole("2");

        assertEquals(3, result.size());
        assertTrue(result.contains("result:view"));
        assertTrue(result.contains("result:enter"));
        assertTrue(result.contains("order:create"));
    }

    @Test
    public void resolveAllPrivilegesForRole_multiLevel_walksFullChain() {
        // Validation -> Results -> Reception
        validationRole.setGroupingParent(2);
        resultsRole.setGroupingParent(1);

        when(roleService.getRoleById(3)).thenReturn(validationRole);
        when(roleService.getRoleById(2)).thenReturn(resultsRole);
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(3)).thenReturn(Arrays.asList(resultValidate));
        when(privilegeDAO.getPrivilegesForRole(2)).thenReturn(Arrays.asList(resultView, resultEnter));
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate));

        Set<String> result = privilegeService.resolveAllPrivilegesForRole("3");

        assertEquals(4, result.size());
        assertTrue(result.contains("result:validate"));
        assertTrue(result.contains("result:view"));
        assertTrue(result.contains("result:enter"));
        assertTrue(result.contains("order:create"));
    }

    @Test
    public void resolveAllPrivilegesForRole_globalAdmin_returnsSentinel() {
        when(roleService.getRoleById(10)).thenReturn(globalAdminRole);

        Set<String> result = privilegeService.resolveAllPrivilegesForRole("10");

        assertEquals(1, result.size());
        assertTrue(result.contains(Privileges.GLOBAL_ADMIN_SENTINEL));
    }

    @Test
    public void resolveAllPrivilegesForRole_nullRoleId_returnsEmpty() {
        Set<String> result = privilegeService.resolveAllPrivilegesForRole(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void resolveAllPrivilegesForRole_blankRoleId_returnsEmpty() {
        Set<String> result = privilegeService.resolveAllPrivilegesForRole("  ");
        assertTrue(result.isEmpty());
    }

    @Test
    public void resolveAllPrivilegesForRole_stubId_returnsEmpty() {
        Set<String> result = privilegeService.resolveAllPrivilegesForRole("-1");
        assertTrue(result.isEmpty());
    }

    @Test
    public void resolveAllPrivilegesForRole_circularReference_doesNotInfiniteLoop() {
        // Role A has parent B, role B has parent A — circular
        Role roleA = new Role();
        roleA.setId(100);
        roleA.setName("Role A");
        roleA.setGroupingParent(101);

        Role roleB = new Role();
        roleB.setId(101);
        roleB.setName("Role B");
        roleB.setGroupingParent(100);

        when(roleService.getRoleById(100)).thenReturn(roleA);
        when(roleService.getRoleById(101)).thenReturn(roleB);
        when(privilegeDAO.getPrivilegesForRole(100)).thenReturn(Arrays.asList(orderCreate));
        when(privilegeDAO.getPrivilegesForRole(101)).thenReturn(Arrays.asList(resultView));

        // Must terminate — no StackOverflowError
        Set<String> result = privilegeService.resolveAllPrivilegesForRole("100");

        assertTrue(result.contains("order:create"));
        assertTrue(result.contains("result:view"));
    }

    // --- getAllPrivilegesForUser ---

    @Test
    public void getAllPrivilegesForUser_singleRole_returnsResolvedPrivileges() {
        when(userRoleService.getRoleIdsForUser("42")).thenReturn(Arrays.asList(1));
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate));

        Set<String> result = privilegeService.getAllPrivilegesForUser("42");

        assertTrue(result.contains("order:create"));
    }

    @Test
    public void getAllPrivilegesForUser_multipleRoles_mergesPrivileges() {
        when(userRoleService.getRoleIdsForUser("42")).thenReturn(Arrays.asList(1, 2));
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(roleService.getRoleById(2)).thenReturn(resultsRole);
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate));
        when(privilegeDAO.getPrivilegesForRole(2)).thenReturn(Arrays.asList(resultView, resultEnter));

        Set<String> result = privilegeService.getAllPrivilegesForUser("42");

        assertEquals(3, result.size());
        assertTrue(result.contains("order:create"));
        assertTrue(result.contains("result:view"));
        assertTrue(result.contains("result:enter"));
    }

    @Test
    public void getAllPrivilegesForUser_globalAdmin_returnsSentinelAndShortCircuits() {
        when(userRoleService.getRoleIdsForUser("1")).thenReturn(Arrays.asList(10, 1));
        when(roleService.getRoleById(10)).thenReturn(globalAdminRole);
        // Role 1 should never be queried — Global Admin short-circuits

        Set<String> result = privilegeService.getAllPrivilegesForUser("1");

        assertEquals(1, result.size());
        assertTrue(result.contains(Privileges.GLOBAL_ADMIN_SENTINEL));
    }

    @Test
    public void getAllPrivilegesForUser_noRoles_returnsEmpty() {
        when(userRoleService.getRoleIdsForUser("99")).thenReturn(Collections.emptyList());

        Set<String> result = privilegeService.getAllPrivilegesForUser("99");

        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllPrivilegesForUser_nullUserId_returnsEmpty() {
        Set<String> result = privilegeService.getAllPrivilegesForUser(null);
        assertTrue(result.isEmpty());
    }
}
