package org.openelisglobal.rolemodule.service;

import java.util.Collection;
import java.util.Set;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;

public interface RoleModuleService extends PermissionModuleService<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);

    /**
     * Names of the modules granted with select access to any of the given roles,
     * filtered to the given name prefix (pass "" for all). Unknown or blank role
     * names are ignored. Backs the qa.* permission-key derivation at login (QA
     * release permission model) — the name-based sibling of
     * {@link #getAllPermittedPagesFromAgentId(int)}.
     */
    Set<String> getPermittedModuleNames(Collection<String> roleNames, String prefix);
}
