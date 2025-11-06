package org.openelisglobal.systemusermodule.dao;

import org.openelisglobal.systemusermodule.valueholder.RoleModule;

public interface RoleModuleDAO extends PermissionModuleDAO<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);

    boolean duplicateRoleModuleExists(RoleModule roleModule);
}
