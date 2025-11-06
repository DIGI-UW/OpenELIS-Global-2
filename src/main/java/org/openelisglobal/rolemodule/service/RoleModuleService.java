package org.openelisglobal.rolemodule.service;

import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;

public interface RoleModuleService extends PermissionModuleService<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);
}
