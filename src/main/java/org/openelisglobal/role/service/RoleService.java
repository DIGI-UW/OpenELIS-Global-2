package org.openelisglobal.role.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Spring Security login pipeline (CustomUserDetailsService), session setup (CustomFormAuthenticationSuccessHandler), PluginPermissionService — read methods are public; write methods guarded by PRIV_ROLE_MANAGE")
public interface RoleService extends BaseObjectService<Role, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    void getData(Role role);

    List<Role> getAllActiveRoles();

    List<Role> getReferencingRoles(Role role);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getPageOfRoles(int startingRecNo);

    Role getRoleByName(String name);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<Role> getAllRoles();

    Role getRoleById(Integer roleId);
}
