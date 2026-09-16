package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.reports.dataexport.service.ReportingAccess;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;

public class ReportingAccessTest {
    private UserRoleService roles;
    private SystemUserService users;
    private UserService userService;
    private ReportingAccess access;

    @Before
    public void setUp() {
        roles = mock(UserRoleService.class);
        users = mock(SystemUserService.class);
        userService = mock(UserService.class);
        access = new ReportingAccess(roles, users, userService);
    }

    @Test
    public void ordinaryActiveReportUserReceivesExistingSectionsWithoutExtraSetup() {
        allowReports("42");
        List<IdValuePair> sections = List.of(new IdValuePair("7", "Virology"));
        when(userService.getUserTestSections("42", null)).thenReturn(sections);

        assertEquals(sections, access.requestSections("42"));
    }

    @Test
    public void missingOrInactiveUsersReceiveAnAccessDenial() {
        assertEquals(403, assertThrows(ReportingException.class, () -> access.requireReports("missing")).status());
        SystemUser inactive = new SystemUser();
        inactive.setIsActive("N");
        when(users.get("43")).thenReturn(inactive);

        assertEquals(403, assertThrows(ReportingException.class, () -> access.requireReports("43")).status());
    }

    @Test
    public void currentLabUnitRolesBoundTheRequestedScope() {
        allowReports("42");
        when(roles.userInRole("42", Constants.ROLE_GLOBAL_ADMIN)).thenReturn(false);
        LabUnitRoleMap permitted = new LabUnitRoleMap();
        permitted.setLabUnit("7");
        permitted.setRoles(Set.of("reporting-role"));
        UserLabUnitRoles mapping = new UserLabUnitRoles();
        mapping.setLabUnitRoleMap(Set.of(permitted));
        when(roles.getUserLabUnitRoles("42")).thenReturn(mapping);

        access.requireScope("42", List.of("7"));
        ReportingException changed = assertThrows(ReportingException.class,
                () -> access.requireScope("42", List.of("7", "8")));
        assertEquals(403, changed.status());
        assertEquals("reporting.access.scopeChanged", changed.getMessage());
    }

    private void allowReports(String owner) {
        SystemUser user = new SystemUser();
        user.setIsActive("Y");
        when(users.get(owner)).thenReturn(user);
        when(roles.userInRole(owner, List.of(Constants.ROLE_GLOBAL_ADMIN, Constants.ROLE_REPORTS))).thenReturn(true);
    }
}
