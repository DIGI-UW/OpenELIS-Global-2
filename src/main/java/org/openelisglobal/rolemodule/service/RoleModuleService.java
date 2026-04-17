package org.openelisglobal.rolemodule.service;

import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
public interface RoleModuleService extends PermissionModuleService<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);
}
