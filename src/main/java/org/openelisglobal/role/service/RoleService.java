package org.openelisglobal.role.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.role.valueholder.Role;

public interface RoleService extends BaseObjectService<Role, String> {
    void getData(Role role);

    List<Role> getAllActiveRoles();

    List<Role> getReferencingRoles(Role role);

    List<Role> getPageOfRoles(int startingRecNo);

    Role getRoleByName(String name);

    List<Role> getAllRoles();

    Role getRoleById(String roleId);
}
