package org.openelisglobal.rolemodule.service;

import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;

@CrossDomainService(callers = "PluginPermissionService (analyzer registration, all users), UserModuleService (request auth resolution) — getRoleModuleByRoleAndModuleId is public; inherited write methods guarded at call site")
public interface RoleModuleService extends PermissionModuleService<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);
}
