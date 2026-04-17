package org.openelisglobal.role.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RoleService extends BaseObjectService<Role, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    void getData(Role role);

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<Role> getAllActiveRoles();

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<Role> getReferencingRoles(Role role);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getPageOfRoles(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    Role getRoleByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getAllRoles();

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    Role getRoleById(Integer roleId);
}
