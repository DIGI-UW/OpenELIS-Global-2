package org.openelisglobal.microbiology.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.*;

public class MicrobiologyCaseAccessServiceTest {
    private MicroCaseDAO cases;
    private UserService users;
    private UserRoleService userRoles;
    private org.openelisglobal.systemuser.service.SystemUserService systemUsers;
    private org.openelisglobal.login.service.LoginUserService logins;
    private MicrobiologyCaseAccessServiceImpl access;

    @Before
    public void setup() {
        cases = mock(MicroCaseDAO.class);
        users = mock(UserService.class);
        userRoles = mock(UserRoleService.class);
        RoleService roles = mock(RoleService.class);
        Role results = new Role();
        results.setId("r");
        Role validation = new Role();
        validation.setId("v");
        when(roles.getRoleByName(Constants.ROLE_RESULTS)).thenReturn(results);
        when(roles.getRoleByName(Constants.ROLE_VALIDATION)).thenReturn(validation);
        systemUsers = mock(org.openelisglobal.systemuser.service.SystemUserService.class);
        logins = mock(org.openelisglobal.login.service.LoginUserService.class);
        access = new MicrobiologyCaseAccessServiceImpl(cases, users, roles, userRoles, systemUsers, logins);
    }

    @Test
    public void loginAdministratorNeedsNoGlobalRoleRecordOrLabAssignment() {
        var user = new org.openelisglobal.systemuser.valueholder.SystemUser();
        user.setLoginName("manager");
        var login = new org.openelisglobal.login.valueholder.LoginUser();
        login.setIsAdmin("Y");
        when(systemUsers.get("7")).thenReturn(user);
        when(logins.getUserProfile("manager")).thenReturn(login);
        assertNull(access.permittedLabUnitIds("7", Constants.ROLE_RESULTS));
        assertTrue(access.hasLabUnitRole("7", "10", Constants.ROLE_VALIDATION));
        assertFalse(access.canAccessCase("missing", "7", true));
    }

    @Test
    public void fallbackAdministratorNeedsNoGlobalAdministratorRole() {
        when(userRoles.userInRole("7", "Admin")).thenReturn(true);
        assertNull(access.permittedLabUnitIds("7", Constants.ROLE_RESULTS));
        assertTrue(access.hasLabUnitRole("7", "10", Constants.ROLE_VALIDATION));
    }

    private void assign(String unit, String role) {
        LabUnitRoleMap map = new LabUnitRoleMap();
        map.setLabUnit(unit);
        map.setRoles(Set.of(role));
        UserLabUnitRoles assignments = new UserLabUnitRoles();
        assignments.setLabUnitRoleMap(Set.of(map));
        when(users.getUserLabUnitRoles("7")).thenReturn(assignments);
    }

    @Test
    public void pendingCaseUsesCurrentUnitWithoutAnAnalysis() {
        MicroCase c = new MicroCase();
        c.setLabUnitId("10");
        when(cases.get("case")).thenReturn(Optional.of(c));
        assign("10", "r");
        assertTrue(access.canAccessCase("case", "7", false));
        c.setLabUnitId("11");
        assertFalse(access.canAccessCase("case", "7", false));
    }

    @Test
    public void validationAllowsReadButDoesNotGrantResultsWrites() {
        assign("10", "v");
        assertTrue(access.canReadLabUnit("7", "10"));
        assertFalse(access.hasLabUnitRole("7", "10", Constants.ROLE_RESULTS));
    }

    @Test
    public void missingAssignmentsAndMissingCaseAreDenied() {
        assertFalse(access.canReadLabUnit("7", "10"));
        assertFalse(access.canAccessCase("missing", "7", false));
        assertFalse(access.canReadLabUnit(null, "10"));
    }

    @Test
    public void allUnitAssignmentIsRoleSpecific() {
        assign(org.openelisglobal.systemuser.controller.UnifiedSystemUserController.ALL_LAB_UNITS, "r");
        assertTrue(access.hasLabUnitRole("7", "19", Constants.ROLE_RESULTS));
        assertFalse(access.hasLabUnitRole("7", "19", Constants.ROLE_VALIDATION));
    }

    @Test public void administratorStillCannotFetchANonexistentCase() {
        when(userRoles.userInRole("7",Constants.ROLE_GLOBAL_ADMIN)).thenReturn(true);
        assertTrue(access.canReadLabUnit("7","10"));assertFalse(access.canAccessCase("missing","7",true));
    }

    @org.junit.After
    public void clearRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void samlAndOauthUseRoleSpecificSessionUnitsWithoutDatabaseAssignments() {
        for (Object principal : List.of(
                new org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal(
                        "sso-user", Map.of("name", List.of("sso-user"))),
                new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("oeg-Results-Culture")),
                        Map.of("name", "sso-user"), "name"))) {
            var request = new org.springframework.mock.web.MockHttpServletRequest();
            var user = new org.openelisglobal.login.valueholder.UserSessionData();
            user.setSytemUserId(7);
            request.getSession().setAttribute("userSessionData", user);
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("oeg-Results-Culture"))));
            request.getSession().setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                    new org.springframework.web.context.request.ServletRequestAttributes(request));
            when(users.getUserTestSections("7", "r")).thenReturn(List.of(new org.openelisglobal.common.util.IdValuePair("10", "Culture")));
            assertTrue(access.hasLabUnitRole("7", "10", Constants.ROLE_RESULTS));
            assertFalse(access.hasLabUnitRole("7", "11", Constants.ROLE_RESULTS));
            assertFalse(access.hasLabUnitRole("7", "10", Constants.ROLE_VALIDATION));
            assertFalse(access.hasLabUnitRole("8", "10", Constants.ROLE_RESULTS));
            verify(users, never()).getUserTestSections(eq("8"), anyString());
        }
    }
}
