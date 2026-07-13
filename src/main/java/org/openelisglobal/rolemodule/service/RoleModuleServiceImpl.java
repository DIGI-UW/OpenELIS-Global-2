package org.openelisglobal.rolemodule.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemusermodule.dao.RoleModuleDAO;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleModuleServiceImpl extends AuditableBaseObjectServiceImpl<RoleModule, String>
        implements RoleModuleService {

    @Autowired
    RoleModuleDAO baseObjectDAO;

    @Autowired
    RoleService roleService;

    public RoleModuleServiceImpl() {
        super(RoleModule.class);
    }

    @Override
    protected RoleModuleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(RoleModule roleModule) {
        baseObjectDAO.getData(roleModule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getAllPermissionModules() {
        return baseObjectDAO.getAllPermissionModules();
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPermissionModuleCount() {
        return baseObjectDAO.getTotalPermissionModuleCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getPageOfPermissionModules(int startingRecNo) {
        return baseObjectDAO.getPageOfPermissionModules(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getAllPermissionModulesByAgentId(int systemUserId) {
        return baseObjectDAO.getAllPermissionModulesByAgentId(systemUserId);
    }

    @Override
    public boolean doesUserHaveAnyModules(int userId) {
        return baseObjectDAO.doesUserHaveAnyModules(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId) {
        return baseObjectDAO.getRoleModuleByRoleAndModuleId(roleId, moduleId);
    }

    @Override
    public String insert(RoleModule roleModule) {
        if (getBaseObjectDAO().duplicateRoleModuleExists(roleModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + roleModule.getPermissionAgentId());
        }
        return super.insert(roleModule);
    }

    @Override
    public RoleModule save(RoleModule roleModule) {
        if (getBaseObjectDAO().duplicateRoleModuleExists(roleModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + roleModule.getPermissionAgentId());
        }
        return super.save(roleModule);
    }

    @Override
    public RoleModule update(RoleModule roleModule) {
        if (getBaseObjectDAO().duplicateRoleModuleExists(roleModule)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + roleModule.getPermissionAgentId());
        }
        return super.update(roleModule);
    }

    @Override
    public Set<String> getAllPermittedPagesFromAgentId(int roleId) {
        Set<String> permittedPages = new HashSet<>();
        List<RoleModule> permissionModules = getAllPermissionModulesByAgentId((roleId));

        for (PermissionModule permissionModule : permissionModules) {
            permittedPages.add(permissionModule.getSystemModule().getSystemModuleName());
        }
        return permittedPages;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getPermittedModuleNames(Collection<String> roleNames, String prefix) {
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
            for (RoleModule roleModule : getAllPermissionModulesByAgentId(roleId)) {
                if (!"Y".equals(roleModule.getHasSelect()) || roleModule.getSystemModule() == null) {
                    continue;
                }
                String moduleName = roleModule.getSystemModule().getSystemModuleName();
                if (moduleName != null && moduleName.trim().startsWith(prefix)) {
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
