package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MicrobiologyCaseAccessServiceTest {
    private MicroCaseDAO caseDAO;
    private UserRoleService userRoleService;
    private MicrobiologyCaseAccessService accessService;

    @Before
    public void setUp() {
        caseDAO = mock(MicroCaseDAO.class);
        userRoleService = mock(UserRoleService.class);
        RoleService roleService = mock(RoleService.class);
        accessService = new MicrobiologyCaseAccessServiceImpl(caseDAO, userRoleService, roleService);
        Role results = new Role();
        results.setId("11");
        Role validation = new Role();
        validation.setId("12");
        when(roleService.getRoleByName(Constants.ROLE_RESULTS)).thenReturn(results);
        when(roleService.getRoleByName(Constants.ROLE_VALIDATION)).thenReturn(validation);
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setTestSectionId("21");
        microCase.setSampleId("100");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        when(caseDAO.getForUpdate("case-1")).thenReturn(microCase);
        when(caseDAO.get("missing")).thenReturn(Optional.empty());
    }

    @Test
    public void authenticatedReaderWithoutUnitRightsCanOpenOnlyReadOnly() {
        assertTrue(accessService.canReadCase("case-1", "7"));
        assertFalse(accessService.canViewOnWorklist("case-1", "7"));
        assertFalse(accessService.canEnterResults("case-1", "7"));
        assertFalse(accessService.canValidateResults("case-1", "7"));
    }

    @Test
    public void anUnauthenticatedReaderCannotOpenACase() {
        assertFalse(accessService.canReadCase("case-1", null));
        assertFalse(accessService.canReadCase("case-1", " "));
        assertFalse(accessService.canEnterResults("case-1", null));
    }

    @Test
    public void missingCaseCannotBeReadOrWrittenEvenByAdministrator() {
        when(userRoleService.userInRole("7", Constants.ROLE_GLOBAL_ADMIN)).thenReturn(true);
        assertFalse(accessService.canReadCase("missing", "7"));
        assertFalse(accessService.canEnterResults("missing", "7"));
        assertFalse(accessService.canValidateResults("missing", "7"));
    }

    @Test
    public void resultsInTheCaseUnitAllowTechnicianActionsButNotValidation() {
        assign("21", "11");
        assertTrue(accessService.canViewOnWorklist("case-1", "7"));
        assertTrue(accessService.canEnterResults("case-1", "7"));
        assertFalse(accessService.canValidateResults("case-1", "7"));
    }

    @Test
    public void validationDoesNotImplyResultsPermission() {
        assign("21", "12");
        assertTrue(accessService.canViewOnWorklist("case-1", "7"));
        assertTrue(accessService.canValidateResults("case-1", "7"));
        assertFalse(accessService.canEnterResults("case-1", "7"));
    }

    @Test
    public void permissionInAnotherUnitDoesNotExposeTheCaseOnTheWorklist() {
        assign("22", "11", "12");
        assertFalse(accessService.canViewOnWorklist("case-1", "7"));
        assertFalse(accessService.canEnterResults("case-1", "7"));
        assertFalse(accessService.canValidateResults("case-1", "7"));
        assertTrue(accessService.canReadCase("case-1", "7"));
    }

    @Test
    public void allLabUnitsGrantOnlyTheirConfiguredRole() {
        assign(UnifiedSystemUserController.ALL_LAB_UNITS, "11");
        assertTrue(accessService.canEnterResults("case-1", "7"));
        assertTrue(accessService.canEnterResultsInUnit("22", "7"));
        assertFalse(accessService.canValidateResults("case-1", "7"));
    }

    @Test
    public void administratorAuthorityIsReadFromStoredRoles() {
        when(userRoleService.userInRole("7", Constants.ROLE_GLOBAL_ADMIN)).thenReturn(true);
        assertTrue(accessService.canEnterResults("case-1", "7"));
        assertTrue(accessService.canValidateResults("case-1", "7"));
        assertFalse(accessService.canEnterResults("case-1", "8"));
    }

    @Test
    public void transferRequiresResultsInEachUnit() {
        assign("21", "11");
        assertTrue(accessService.canEnterResults("case-1", "7"));
        assertFalse(accessService.canEnterResultsInUnit("22", "7"));
    }

    @Test
    public void forbiddenWritesReturn403() {
        try {
            accessService.requireResults("case-1", "7");
            fail("Expected a forbidden mutation");
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
        assign("21", "11");
        try {
            accessService.requireValidation("case-1", "7");
            fail("Results permission must not allow validation");
        } catch (ResponseStatusException exception) {
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    private void assign(String unitId, String... roleIds) {
        LabUnitRoleMap map = new LabUnitRoleMap();
        map.setLabUnit(unitId);
        map.setRoles(Set.of(roleIds));
        UserLabUnitRoles roles = new UserLabUnitRoles();
        roles.setLabUnitRoleMap(Set.of(map));
        when(userRoleService.getUserLabUnitRoles("7")).thenReturn(roles);
    }

    @Test
    public void resultsMutationRejectsTheOldUnitAfterRefreshingATransferredCase() {
        assign("21", "11");
        MicroCase moved = new MicroCase();
        moved.setId("case-1");
        moved.setTestSectionId("22");
        when(caseDAO.getForUpdate("case-1")).thenReturn(moved);
        ResponseStatusException denied = assertThrows(ResponseStatusException.class,
                () -> accessService.requireResults("case-1", "7"));
        assertEquals(403, denied.getStatusCode().value());
    }

    @Test
    public void validationMutationRejectsTheOldUnitAfterRefreshingATransferredCase() {
        assign("21", "12");
        MicroCase moved = new MicroCase();
        moved.setId("case-1");
        moved.setTestSectionId("22");
        when(caseDAO.getForUpdate("case-1")).thenReturn(moved);
        ResponseStatusException denied = assertThrows(ResponseStatusException.class,
                () -> accessService.requireValidation("case-1", "7"));
        assertEquals(403, denied.getStatusCode().value());
    }

    @Test
    public void mutationAuthorizationLocksOrderThenRefreshesCaseBeforeCheckingRoles() {
        assign("21", "11");
        accessService.requireResults("case-1", "7");
        var order = inOrder(caseDAO, userRoleService);
        order.verify(caseDAO).lockOrder("100");
        order.verify(caseDAO).getForUpdate("case-1");
        order.verify(userRoleService).getUserLabUnitRoles("7");
    }

    @Test
    public void readOnlyPermissionChecksDoNotTakeMutationLocks() {
        assign("21", "11");
        assertTrue(accessService.canEnterResults("case-1", "7"));
        verify(caseDAO, never()).lockOrder(anyString());
        verify(caseDAO, never()).getForUpdate(anyString());
    }

    @Test
    public void worklistScopeIncludesOnlyUnitsWithResultsOrValidation() {
        LabUnitRoleMap results = new LabUnitRoleMap();
        results.setLabUnit("21");
        results.setRoles(Set.of("11"));
        LabUnitRoleMap validation = new LabUnitRoleMap();
        validation.setLabUnit("22");
        validation.setRoles(Set.of("12"));
        LabUnitRoleMap unrelated = new LabUnitRoleMap();
        unrelated.setLabUnit("23");
        unrelated.setRoles(Set.of("other-role"));
        UserLabUnitRoles roles = new UserLabUnitRoles();
        roles.setLabUnitRoleMap(Set.of(results, validation, unrelated));
        when(userRoleService.getUserLabUnitRoles("7")).thenReturn(roles);

        MicrobiologyWorklistAccess scope = accessService.getWorklistAccess("7");
        assertFalse(scope.allUnits());
        assertEquals(Set.of("21", "22"), scope.unitIds());
    }

    @Test
    public void allUnitsWithAnUnrelatedRoleDoNotGrantWorklistAccess() {
        assign(UnifiedSystemUserController.ALL_LAB_UNITS, "other-role");
        MicrobiologyWorklistAccess scope = accessService.getWorklistAccess("7");
        assertFalse(scope.hasAccess());
        assertFalse(scope.allUnits());
        assertEquals(Set.of(), scope.unitIds());
    }

    @Test
    public void administratorWorklistScopeComesFromStoredAuthority() {
        when(userRoleService.userInRole("7", Constants.ROLE_GLOBAL_ADMIN)).thenReturn(true);
        assertTrue(accessService.getWorklistAccess("7").allUnits());
        assertFalse(accessService.getWorklistAccess("8").hasAccess());
        assertFalse(accessService.getWorklistAccess(null).hasAccess());
    }
}
