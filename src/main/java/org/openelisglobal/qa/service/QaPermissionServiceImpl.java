package org.openelisglobal.qa.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the qa.* permission keys granted to a set of roles via
 * system_role_module. Shared by authentication (Spring authorities backing
 * hasAuthority checks on QA REST controllers) and the /session payload
 * (frontend route/tile gating), so both layers derive from the same grants.
 */
@Service
public class QaPermissionServiceImpl implements QaPermissionService {

    public static final String QA_PERMISSION_PREFIX = "qa.";

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleModuleService roleModuleService;

    @Override
    @Transactional(readOnly = true)
    public Set<String> getQaPermissionsForRoleNames(Collection<String> roleNames) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roleNames == null) {
            return permissions;
        }
        for (String roleName : roleNames) {
            if (roleName == null || roleName.trim().isEmpty()) {
                continue;
            }
            // getRoleByName returns an id "-1" stub for unknown names
            Role role = roleService.getRoleByName(roleName.trim());
            int roleId = parseRoleId(role);
            if (roleId <= 0) {
                continue;
            }
            for (RoleModule roleModule : roleModuleService.getAllPermissionModulesByAgentId(roleId)) {
                if (!"Y".equals(roleModule.getHasSelect()) || roleModule.getSystemModule() == null) {
                    continue;
                }
                String moduleName = roleModule.getSystemModule().getSystemModuleName();
                if (moduleName != null && moduleName.trim().startsWith(QA_PERMISSION_PREFIX)) {
                    permissions.add(moduleName.trim());
                }
            }
        }
        return permissions;
    }

    private int parseRoleId(Role role) {
        if (role == null || role.getId() == null) {
            return -1;
        }
        try {
            return Integer.parseInt(role.getId().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
