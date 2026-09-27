package org.openelisglobal.systemusermodule.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;

public interface RoleModuleDAO extends PermissionModuleDAO<RoleModule> {

    RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId);

    boolean duplicateRoleModuleExists(RoleModule roleModule);

    /**
     * Distinct names of the modules granted with select access to any of these
     * roles, restricted to names starting with the given prefix. One query for the
     * whole set: this runs on every login.
     */
    List<String> getSelectableModuleNames(Collection<String> roleNames, String namePrefix);
}
