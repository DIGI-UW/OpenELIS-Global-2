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

    // Ungated identity primitive, like getRoleById below. Name->role resolution is
    // used across operational workflows (getUserTestSections,
    // logbook/results/reports
    // screens, and UserServiceImpl resolving the Global-Admin role id) — NOT the
    // role-administration surface, which stays gated (getAllRoles/getPageOfRoles/
    // getReferencingRoles keep PRIV_ROLE_*). It was briefly gated on
    // PRIV_ORDER_VIEW
    // to stop a report-page 500, but that coupled role lookup to the order domain:
    // it under-privileged roles without order:view (User Account Administrator,
    // Audit Trail — breaking the user-admin screen) and over-privileged everyone
    // else. A plain name->id read leaks no sensitive data, so leave it ungated.
    Role getRoleByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getAllRoles();

    /**
     * Creates an assignable role from the role editor.
     *
     * <p>
     * Both parent pointers matter and mean different things (see
     * liquibase/3.5.x.x/012-005): {@code groupingParentName} is the UI container
     * the role renders under — "Global Roles", or "Lab Unit Roles" when the role is
     * scoped per test section — and a role placed under neither is created but can
     * never be assigned to anyone. {@code parentRoleName} is privilege inheritance:
     * the role whose privileges this one absorbs, or null for none.
     *
     * @return the persisted role
     * @throws IllegalArgumentException if the name is blank or already taken, or if
     *                                  either named parent does not exist
     */
    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    Role createAssignableRole(String name, String description, String displayKey, String groupingParentName,
            String parentRoleName, String sysUserId);

    /**
     * Ungated identity read: authentication and /session resolve role names for the
     * caller's own role ids before/while the privilege context is established.
     */
    Role getRoleById(Integer roleId);
}
