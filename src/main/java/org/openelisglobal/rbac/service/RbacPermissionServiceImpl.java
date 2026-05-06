package org.openelisglobal.rbac.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserProjectRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TR-03/TR-04/TR-05: Unified permission engine. Evaluates all three
 * authorization scopes: Global, Department, Project.
 */
@Service
@Transactional(readOnly = true)
public class RbacPermissionServiceImpl implements RbacPermissionService {

    // Global roles that bypass department restrictions
    private static final String ROLE_SYSTEM_ADMIN = "Global Administrator";
    private static final String ROLE_ADMIN_STAFF = "Administrative Staff";
    private static final String ROLE_IT_SUPPORT = "IT Support Staff";
    private static final String ROLE_EQA_PERSONNEL = "EQA Personnel";
    private static final String ROLE_EXTERNAL = "External Stakeholders";

    // Department roles
    private static final String ROLE_LAB_MANAGER = "Lab Managers";
    private static final String ROLE_LAB_TECH = "Laboratory Technicians";
    private static final String ROLE_SAMPLE_COLLECTOR = "Sample Collectors";
    private static final String ROLE_RESEARCHER = "Researchers";
    private static final String ROLE_BIOMEDICAL = "Biomedical Staff";

    // Project roles
    private static final String ROLE_PI = "Principal Investigator";
    private static final String ROLE_PROJECT_COORD = "Project Coordinators";
    private static final String ROLE_DATA_MANAGER = "Data Managers";

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserProjectRoleService userProjectRoleService;

    @Override
    public boolean hasPermission(String systemUserId, String module, String action) {
        if (systemUserId == null)
            return false;
        // System Admin has full access
        if (hasGlobalRole(systemUserId, ROLE_SYSTEM_ADMIN))
            return true;
        // Check module-specific global role permissions
        return hasModulePermissionViaGlobalRole(systemUserId, module, action)
                || hasAnyDepartmentRoleForModule(systemUserId, module, action)
                || hasAnyProjectRoleForModule(systemUserId, module, action);
    }

    @Override
    public boolean hasPermission(String systemUserId, String module, String action, String departmentId) {
        if (systemUserId == null)
            return false;
        if (hasGlobalRole(systemUserId, ROLE_SYSTEM_ADMIN))
            return true;
        if (hasModulePermissionViaGlobalRole(systemUserId, module, action))
            return true;
        return hasDepartmentRoleForModule(systemUserId, departmentId, module, action);
    }

    @Override
    public boolean hasPermissionForProject(String systemUserId, String module, String action, String projectId) {
        if (systemUserId == null)
            return false;
        if (hasGlobalRole(systemUserId, ROLE_SYSTEM_ADMIN))
            return true;
        if (hasModulePermissionViaGlobalRole(systemUserId, module, action))
            return true;
        return hasProjectRoleForModule(systemUserId, projectId, module, action);
    }

    @Override
    public List<String> getAllowedDepartments(String systemUserId) {
        if (systemUserId == null)
            return Collections.emptyList();
        // System Admin and global roles with unrestricted access return null (no
        // filter)
        if (hasGlobalRole(systemUserId, ROLE_SYSTEM_ADMIN) || hasGlobalRole(systemUserId, ROLE_ADMIN_STAFF)) {
            return null; // null = unrestricted
        }
        // Returns the departments the user MAY choose to work in (UI chooser).
        // This is NOT the data-scope boundary — use RbacContext#getActiveDepartmentId()
        // (populated from loginLabUnit) for storage/inventory/equipment data filtering.
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null) {
            return Collections.emptyList();
        }
        List<String> departments = new ArrayList<>();
        for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
            if (map.getLabUnit() != null) {
                departments.add(map.getLabUnit());
            }
        }
        return departments;
    }

    @Override
    public List<String> getAllowedProjects(String systemUserId) {
        if (systemUserId == null)
            return Collections.emptyList();
        if (hasGlobalRole(systemUserId, ROLE_SYSTEM_ADMIN))
            return null; // unrestricted
        return userProjectRoleService.getProjectIdsForUser(systemUserId);
    }

    @Override
    public Set<String> getAllRolesForUser(String systemUserId) {
        Set<String> roles = new HashSet<>();
        // Global roles
        List<String> roleIds = userRoleService.getRoleIdsForUser(systemUserId);
        for (String roleId : roleIds) {
            Role role = roleService.getRoleById(roleId);
            if (role != null)
                roles.add(role.getName());
        }
        // Department roles
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles != null && labUnitRoles.getLabUnitRoleMap() != null) {
            for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
                if (map.getRoles() != null)
                    roles.addAll(map.getRoles());
            }
        }
        // Project roles
        roles.addAll(userProjectRoleService.getRoleNamesForUser(systemUserId));
        return roles;
    }

    @Override
    public boolean hasGlobalRole(String systemUserId, String roleName) {
        return userRoleService.userInRole(systemUserId, roleName);
    }

    @Override
    public boolean hasDepartmentRole(String systemUserId, String departmentId, String roleName) {
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null)
            return false;
        for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
            if (departmentId.equals(map.getLabUnit()) && map.getRoles() != null) {
                for (String roleIdOrName : map.getRoles()) {
                    if (roleName.equals(resolveRoleName(roleIdOrName)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Resolve a role ID (numeric string) or role name to the canonical role name.
     */
    private String resolveRoleName(String roleIdOrName) {
        if (roleIdOrName == null)
            return "";
        // If it looks like a numeric ID, resolve it
        try {
            Integer.parseInt(roleIdOrName);
            Role role = roleService.getRoleById(roleIdOrName);
            return role != null ? role.getName() : roleIdOrName;
        } catch (NumberFormatException e) {
            return roleIdOrName; // already a name
        }
    }

    @Override
    public boolean hasProjectRole(String systemUserId, String projectId, String roleName) {
        return userProjectRoleService.hasProjectRole(systemUserId, projectId, roleName);
    }

    // --- Private helpers ---

    private boolean hasModulePermissionViaGlobalRole(String systemUserId, String module, String action) {
        // IT Support: read-only diagnostics, no data modification
        if (hasGlobalRole(systemUserId, ROLE_IT_SUPPORT)) {
            return "READ".equalsIgnoreCase(action) && "DIAGNOSTICS".equalsIgnoreCase(module);
        }
        // EQA Personnel: QC module access
        if (hasGlobalRole(systemUserId, ROLE_EQA_PERSONNEL)) {
            return "QC".equalsIgnoreCase(module) || "REPORTING".equalsIgnoreCase(module);
        }
        // External Stakeholders: read-only reporting/dashboards
        if (hasGlobalRole(systemUserId, ROLE_EXTERNAL)) {
            return "READ".equalsIgnoreCase(action) && "REPORTING".equalsIgnoreCase(module);
        }
        // Administrative Staff: user management and reports
        if (hasGlobalRole(systemUserId, ROLE_ADMIN_STAFF)) {
            return "REPORTING".equalsIgnoreCase(module) || "USER_MANAGEMENT".equalsIgnoreCase(module);
        }
        return false;
    }

    private boolean hasAnyDepartmentRoleForModule(String systemUserId, String module, String action) {
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null)
            return false;
        for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
            if (map.getRoles() != null) {
                for (String roleIdOrName : map.getRoles()) {
                    String roleName = resolveRoleName(roleIdOrName);
                    if (departmentRoleAllowsModuleAction(roleName, module, action))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean hasDepartmentRoleForModule(String systemUserId, String departmentId, String module, String action) {
        UserLabUnitRoles labUnitRoles = userRoleService.getUserLabUnitRoles(systemUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null)
            return false;
        for (LabUnitRoleMap map : labUnitRoles.getLabUnitRoleMap()) {
            if (departmentId.equals(map.getLabUnit()) && map.getRoles() != null) {
                for (String roleIdOrName : map.getRoles()) {
                    String roleName = resolveRoleName(roleIdOrName);
                    if (departmentRoleAllowsModuleAction(roleName, module, action))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean hasAnyProjectRoleForModule(String systemUserId, String module, String action) {
        List<UserProjectRole> projectRoles = userProjectRoleService.getProjectRolesForUser(systemUserId);
        for (UserProjectRole pr : projectRoles) {
            if (pr.getActive() && projectRoleAllowsModuleAction(pr.getRoleName(), module, action))
                return true;
        }
        return false;
    }

    private boolean hasProjectRoleForModule(String systemUserId, String projectId, String module, String action) {
        List<UserProjectRole> projectRoles = userProjectRoleService.getProjectRolesForUser(systemUserId);
        for (UserProjectRole pr : projectRoles) {
            if (pr.getActive() && projectId.equals(pr.getProjectId())
                    && projectRoleAllowsModuleAction(pr.getRoleName(), module, action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * TR-03: Department role → module/action permission matrix. Handles both new
     * AHRI role names and existing legacy role names.
     */
    private boolean departmentRoleAllowsModuleAction(String role, String module, String action) {
        if (role == null)
            return false;
        String r = role.toLowerCase();

        // Lab Manager / Supervisor — full department access
        if (r.contains("lab manager") || r.contains("lab supervisor") || r.contains("supervisor")) {
            return true;
        }
        // Laboratory Technician variants — sample, storage, inventory, equipment (no
        // admin)
        if (r.contains("laboratory technician") || r.contains("lab technician") || r.contains("technician")
                || r.contains("analyst") || r.contains("test executor") || r.contains("results entry")) {
            return !isAdminAction(action);
        }
        // Sample Collector / Receiver / Processor
        if (r.contains("sample collector") || r.contains("sample receiver") || r.contains("sample processor")
                || r.contains("register sample")) {
            return "SAMPLE".equalsIgnoreCase(module)
                    && ("CREATE".equalsIgnoreCase(action) || "READ".equalsIgnoreCase(action));
        }
        // Researcher variants
        if (r.contains("researcher") || r.contains("junior") || r.contains("senior researcher")) {
            return "READ".equalsIgnoreCase(action) || "UPDATE".equalsIgnoreCase(action);
        }
        // Biomedical Staff — equipment only
        if (r.contains("biomedical") || r.contains("manage equipment")) {
            return "EQUIPMENT".equalsIgnoreCase(module);
        }
        // Storage Manager
        if (r.contains("storage manager")) {
            return "STORAGE".equalsIgnoreCase(module);
        }
        // QC Technician
        if (r.contains("qc") || r.contains("quality")) {
            return "QC".equalsIgnoreCase(module);
        }
        // Notebook Entry Creator — notebook access
        if (r.contains("notebook")) {
            return "NOTEBOOK".equalsIgnoreCase(module);
        }
        // Results Validator / Validation
        if (r.contains("validator") || r.contains("validation") || r.contains("validate")) {
            return "SAMPLE".equalsIgnoreCase(module) || "REPORTING".equalsIgnoreCase(module);
        }
        // Report Generator
        if (r.contains("report")) {
            return "REPORTING".equalsIgnoreCase(module) && !isWriteAction(action);
        }
        // Aliquoting
        if (r.contains("aliquot")) {
            return "SAMPLE".equalsIgnoreCase(module);
        }
        // Disposal Officer
        if (r.contains("disposal")) {
            return "DISPOSAL".equalsIgnoreCase(module);
        }
        return false;
    }

    /**
     * TR-03: Project role → module/action permission matrix. Project access does
     * NOT bypass department storage/inventory restrictions.
     */
    private boolean projectRoleAllowsModuleAction(String role, String module, String action) {
        switch (role) {
        case ROLE_PI:
            // Approve, lock, reject project records; view assigned project modules
            return "READ".equalsIgnoreCase(action) || "VALIDATE".equalsIgnoreCase(action)
                    || "APPROVE".equalsIgnoreCase(action);
        case ROLE_PROJECT_COORD:
            // Full project workflow (no storage/inventory unless separately assigned)
            return !"STORAGE".equalsIgnoreCase(module) && !"INVENTORY".equalsIgnoreCase(module);
        case ROLE_DATA_MANAGER:
            // Data analysis, dashboards, exports, reporting only
            return "REPORTING".equalsIgnoreCase(module) && !isWriteAction(action);
        default:
            return false;
        }
    }

    private boolean isAdminAction(String action) {
        return "DELETE".equalsIgnoreCase(action) || "ADMIN".equalsIgnoreCase(action)
                || "USER_MANAGEMENT".equalsIgnoreCase(action);
    }

    private boolean isWriteAction(String action) {
        return "CREATE".equalsIgnoreCase(action) || "UPDATE".equalsIgnoreCase(action)
                || "DELETE".equalsIgnoreCase(action);
    }
}
