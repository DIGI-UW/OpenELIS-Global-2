package org.openelisglobal.role.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Authentication (CustomUserDetailsService) and session introspection resolve the"
        + " caller's own roles by id before the privilege context exists (getRoleById is ungated); role"
        + " administration methods remain individually gated with PRIV_ROLE_*")
public interface RoleService extends BaseObjectService<Role, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    void getData(Role role);

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<Role> getAllActiveRoles();

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<Role> getReferencingRoles(Role role);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getPageOfRoles(int startingRecNo);

    // Name->role resolution is an operational primitive (getUserTestSections,
    // logbook/results/reports screens all resolve a role id by name for the
    // current user's own workflow), NOT the role-administration surface — those
    // methods (getAllRoles/getPageOfRoles/getReferencingRoles) keep PRIV_ROLE_VIEW.
    // Gate at PRIV_ORDER_VIEW, the privilege every operational lab-unit role holds,
    // so ordinary Results/Reports/Validation/Reception users don't get denied
    // (which crashed the report page: the 500 made labUnits a non-array).
    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    Role getRoleByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getAllRoles();

    /**
     * Ungated identity read: authentication and /session resolve role names for the
     * caller's own role ids before/while the privilege context is established.
     */
    Role getRoleById(Integer roleId);
}
