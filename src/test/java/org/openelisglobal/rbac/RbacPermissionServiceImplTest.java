package org.openelisglobal.rbac;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
public class RbacPermissionServiceImplTest {

    @InjectMocks
    private RbacPermissionServiceImpl service;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private RoleService roleService;

    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(42);
        usd.setLoginLabUnit(178);
        request = new MockHttpServletRequest();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(false);
        when(userRoleService.getRoleIdsForUser("42")).thenReturn(List.of());
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(null);
    }

    @Test
    public void globalAdminCanDoEveryAction() {
        when(userRoleService.userInRole("42", Constants.ROLE_GLOBAL_ADMIN)).thenReturn(true);

        assertTrue(service.hasPermission(request, RbacAction.SYSTEM_ADMIN));
        assertTrue(service.hasPermission(request, RbacAction.MANAGE_QA));
    }

    @Test
    public void systemAdminCanOnlyDoSystemAdminAction() {
        when(userRoleService.userInRole("42", Constants.ROLE_SYSTEM_ADMIN)).thenReturn(true);

        assertTrue(service.hasPermission(request, RbacAction.SYSTEM_ADMIN));
        assertFalse(service.hasPermission(request, RbacAction.MANAGE_QA));
        assertFalse(service.hasPermission(request, RbacAction.REGISTER_SAMPLES));
    }

    @Test
    public void sampleCollectorCanRegisterButCannotValidate() {
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(labUnitRoles("178", "role-sample-collector"));
        when(roleService.getRoleById("role-sample-collector")).thenReturn(role(Constants.ROLE_SAMPLE_COLLECTOR));

        assertTrue(service.hasPermission(request, RbacAction.REGISTER_SAMPLES));
        assertFalse(service.hasPermission(request, RbacAction.VALIDATE_RESULTS));
    }

    @Test
    public void foreignDepartmentRoleDoesNotApplyToActiveDepartment() {
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(labUnitRoles("177", "role-lab-manager"));

        assertFalse(service.hasPermission(request, RbacAction.MANAGE_QA));
    }

    @Test
    public void labUnitRoleNamesAreAcceptedWhenRoleLookupCannotParseAnId() {
        when(userRoleService.getUserLabUnitRoles("42")).thenReturn(labUnitRoles("178", Constants.ROLE_LAB_MANAGER));
        doThrow(NumberFormatException.class).when(roleService).getRoleById(Constants.ROLE_LAB_MANAGER);

        assertTrue(service.hasPermission(request, RbacAction.MANAGE_QA));
    }

    private UserLabUnitRoles labUnitRoles(String labUnit, String roleId) {
        LabUnitRoleMap map = new LabUnitRoleMap();
        map.setLabUnit(labUnit);
        map.setRoles(Set.of(roleId));
        UserLabUnitRoles roles = new UserLabUnitRoles();
        roles.setLabUnitRoleMap(Set.of(map));
        return roles;
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
