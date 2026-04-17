package org.openelisglobal.privilege.service;

import static org.junit.Assert.assertFalse;
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
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;

/**
 * T030 — Verifies that {@link PrivilegeServiceImpl#getAllPrivilegesForUser}
 * correctly resolves the full inherited privilege set for a user whose role
 * sits in an inheritance chain.
 *
 * <p>
 * Role chain under test: Validation (id=3) → Results (id=2) → Reception (id=1)
 *
 * <p>
 * A user assigned only the Validation role must receive all privileges from all
 * three roles in the chain. A user assigned only Reception must NOT receive
 * Validation-only privileges.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrivilegeResolutionTest {

    @Mock
    private PrivilegeDAO privilegeDAO;

    @Mock
    private RoleService roleService;

    @Mock
    private UserRoleService userRoleService;

    @InjectMocks
    private PrivilegeServiceImpl privilegeService;

    // Roles: Validation → Results → Reception
    private Role receptionRole;
    private Role resultsRole;
    private Role validationRole;

    // Privileges per role
    private Privilege orderCreate; // Reception direct
    private Privilege patientView; // Reception direct
    private Privilege resultView; // Results direct
    private Privilege resultEnter; // Results direct
    private Privilege resultValidate; // Validation direct

    @Before
    public void setup() {
        receptionRole = new Role();
        receptionRole.setId(1);
        receptionRole.setName(Constants.ROLE_RECEPTION);
        // Reception has no parent

        resultsRole = new Role();
        resultsRole.setId(2);
        resultsRole.setName(Constants.ROLE_RESULTS);
        resultsRole.setGroupingParent(1); // inherits from Reception

        validationRole = new Role();
        validationRole.setId(3);
        validationRole.setName(Constants.ROLE_VALIDATION);
        validationRole.setGroupingParent(2); // inherits from Results

        orderCreate = privilege("order:create");
        patientView = privilege("patient:view");
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

    // --- Validation user inherits the full chain ---

    @Test
    public void validationUser_receivesOwnPrivileges() {
        stubValidationChain();
        when(userRoleService.getRoleIdsForUser("10")).thenReturn(Arrays.asList(3));

        Set<String> result = privilegeService.getAllPrivilegesForUser("10");

        assertTrue(result.contains("result:validate"));
    }

    @Test
    public void validationUser_inheritsResultsPrivileges() {
        stubValidationChain();
        when(userRoleService.getRoleIdsForUser("10")).thenReturn(Arrays.asList(3));

        Set<String> result = privilegeService.getAllPrivilegesForUser("10");

        assertTrue(result.contains("result:view"));
        assertTrue(result.contains("result:enter"));
    }

    @Test
    public void validationUser_inheritsReceptionPrivileges() {
        stubValidationChain();
        when(userRoleService.getRoleIdsForUser("10")).thenReturn(Arrays.asList(3));

        Set<String> result = privilegeService.getAllPrivilegesForUser("10");

        assertTrue(result.contains("order:create"));
        assertTrue(result.contains("patient:view"));
    }

    @Test
    public void validationUser_receivesAllFivePrivileges() {
        stubValidationChain();
        when(userRoleService.getRoleIdsForUser("10")).thenReturn(Arrays.asList(3));

        Set<String> result = privilegeService.getAllPrivilegesForUser("10");

        // All 5 privileges across the 3-level chain
        assertTrue(result.containsAll(
                Arrays.asList("result:validate", "result:view", "result:enter", "order:create", "patient:view")));
    }

    // --- Reception user does NOT receive Validation-only privileges ---

    @Test
    public void receptionUser_doesNotReceiveValidationPrivileges() {
        when(userRoleService.getRoleIdsForUser("20")).thenReturn(Arrays.asList(1));
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate, patientView));

        Set<String> result = privilegeService.getAllPrivilegesForUser("20");

        assertFalse(result.contains("result:validate"));
        assertFalse(result.contains("result:enter"));
        assertFalse(result.contains("result:view"));
    }

    @Test
    public void receptionUser_receivesOnlyDirectPrivileges() {
        when(userRoleService.getRoleIdsForUser("20")).thenReturn(Arrays.asList(1));
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate, patientView));

        Set<String> result = privilegeService.getAllPrivilegesForUser("20");

        assertTrue(result.contains("order:create"));
        assertTrue(result.contains("patient:view"));
    }

    // --- User with multiple roles gets union ---

    @Test
    public void userWithMultipleRoles_getsUnionOfPrivileges() {
        // User has both Reception (id=1) and Validation (id=3) assigned directly
        when(userRoleService.getRoleIdsForUser("30")).thenReturn(Arrays.asList(1, 3));
        stubValidationChain();

        Set<String> result = privilegeService.getAllPrivilegesForUser("30");

        assertTrue(result.containsAll(
                Arrays.asList("result:validate", "result:view", "result:enter", "order:create", "patient:view")));
    }

    // --- No roles ---

    @Test
    public void userWithNoRoles_returnsEmpty() {
        when(userRoleService.getRoleIdsForUser("99")).thenReturn(Collections.emptyList());

        Set<String> result = privilegeService.getAllPrivilegesForUser("99");

        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    /** Stubs the full Validation → Results → Reception chain. */
    private void stubValidationChain() {
        when(roleService.getRoleById(3)).thenReturn(validationRole);
        when(roleService.getRoleById(2)).thenReturn(resultsRole);
        when(roleService.getRoleById(1)).thenReturn(receptionRole);
        when(privilegeDAO.getPrivilegesForRole(3)).thenReturn(Arrays.asList(resultValidate));
        when(privilegeDAO.getPrivilegesForRole(2)).thenReturn(Arrays.asList(resultView, resultEnter));
        when(privilegeDAO.getPrivilegesForRole(1)).thenReturn(Arrays.asList(orderCreate, patientView));
    }
}
