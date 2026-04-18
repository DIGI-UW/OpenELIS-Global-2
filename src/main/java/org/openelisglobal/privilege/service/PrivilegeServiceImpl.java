package org.openelisglobal.privilege.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.constants.Privileges;
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrivilegeServiceImpl implements PrivilegeService {

    @Autowired
    private PrivilegeDAO privilegeDAO;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Override
    public List<Privilege> getAllPrivileges() {
        return privilegeDAO.getAll();
    }

    @Override
    public List<Privilege> getDirectPrivilegesForRole(String roleId) {
        if (GenericValidator.isBlankOrNull(roleId)) {
            return Collections.emptyList();
        }
        try {
            return getDirectPrivilegesForRole(Integer.valueOf(roleId));
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }

    private List<Privilege> getDirectPrivilegesForRole(Integer roleId) {
        return privilegeDAO.getPrivilegesForRole(roleId);
    }

    @Override
    public Set<String> resolveAllPrivilegesForRole(String roleId) {
        if (GenericValidator.isBlankOrNull(roleId)) {
            return Collections.emptySet();
        }
        try {
            return resolveAllPrivilegesForRole(Integer.parseInt(roleId), new HashSet<>());
        } catch (NumberFormatException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Internal overload that carries a visited set to guard against circular parent
     * references.
     */
    private Set<String> resolveAllPrivilegesForRole(Integer roleId, Set<Integer> visited) {
        if (roleId == null) {
            return Collections.emptySet();
        }

        // Circular reference guard
        if (!visited.add(roleId)) {
            return Collections.emptySet();
        }

        Role role = roleService.getRoleById(roleId);
        if (role == null) {
            return Collections.emptySet();
        }

        // Global Administrator gets every privilege
        if (Constants.ROLE_GLOBAL_ADMIN.equals(role.getName())) {
            return Set.of(Privileges.GLOBAL_ADMIN_SENTINEL);
        }

        Set<String> privileges = new HashSet<>();

        // Direct privileges for this role
        for (Privilege p : privilegeDAO.getPrivilegesForRole(roleId)) {
            privileges.add(p.getName());
        }

        // Inherit privileges from parent role recursively
        Integer parentId = role.getGroupingParent();
        if (parentId != null) {
            Set<String> parentPrivileges = resolveAllPrivilegesForRole(parentId, visited);
            if (parentPrivileges.contains(Privileges.GLOBAL_ADMIN_SENTINEL)) {
                return Set.of(Privileges.GLOBAL_ADMIN_SENTINEL);
            }
            privileges.addAll(parentPrivileges);
        }

        return privileges;
    }

    @Override
    public Set<String> getAllPrivilegesForUser(String systemUserId) {
        if (GenericValidator.isBlankOrNull(systemUserId)) {
            return Collections.emptySet();
        }

        List<Integer> roleIds = userRoleService.getRoleIdsForUser(systemUserId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> allPrivileges = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        for (Integer roleId : roleIds) {
            Set<String> rolePrivileges = resolveAllPrivilegesForRole(roleId, visited);
            if (rolePrivileges.contains(Privileges.GLOBAL_ADMIN_SENTINEL)) {
                // Global Admin — short-circuit, no need to check remaining roles
                return Set.of(Privileges.GLOBAL_ADMIN_SENTINEL);
            }
            allPrivileges.addAll(rolePrivileges);
        }

        return allPrivileges;
    }
}
