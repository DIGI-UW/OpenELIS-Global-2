package org.openelisglobal.role.service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for ensuring users with Global Administrator role have
 * access to all lab functionality. Uses a hybrid approach: 1. Assigns "Lab Unit
 * Roles" grouping for role hierarchy access 2. Assigns "AllLabUnits" with all
 * lab unit roles for UI visibility 3. Frontend permission logic provides
 * automatic bypass for Global Administrators
 *
 * IMPORTANT: Only users with "Global Administrator" role get automatic full lab
 * access. This includes the "admin" account - it must have Global Administrator
 * role to get lab access.
 */
@Service
public class AdminDefaultRolesService {

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private UserService userService;

    /**
     * Ensures all users with Global Administrator role have comprehensive lab
     * access. This provides access to all notebook roles and lab functionality
     * through: 1. Lab Unit Roles grouping assignment (role hierarchy) 2.
     * AllLabUnits assignment with all lab unit roles (UI visibility)
     *
     * Only users with Global Administrator role get automatic full lab access.
     */
    @PostConstruct
    @Transactional
    public void ensureGlobalAdministratorsHaveLabAccess() {
        try {
            Role labUnitRolesGroup = roleService.getRoleByName(Constants.LAB_ROLES_GROUP);
            if (labUnitRolesGroup == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "ensureGlobalAdministratorsHaveLabAccess",
                        "Lab Unit Roles grouping not found, cannot assign default roles");
                return;
            }

            List<String> globalAdminUserIds = userRoleService.getUserIdsForRole(Constants.ROLE_GLOBAL_ADMIN);

            if (globalAdminUserIds.isEmpty()) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "ensureGlobalAdministratorsHaveLabAccess",
                        "No Global Administrator users found. Users must have Global Administrator role to get automatic lab access.");
                return;
            }

            int assignedCount = 0;
            for (String userId : globalAdminUserIds) {
                SystemUser user = systemUserService.get(userId);
                if (user != null) {
                    boolean needsRoleAssignment = !userRoleService.userInRole(user.getId(), Constants.LAB_ROLES_GROUP);
                    boolean needsLabUnitAssignment = !hasAllLabUnitsAssignment(user);

                    if (needsRoleAssignment || needsLabUnitAssignment) {
                        assignComprehensiveLabAccess(user, labUnitRolesGroup, needsRoleAssignment,
                                needsLabUnitAssignment);
                        assignedCount++;
                    }
                }
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "ensureGlobalAdministratorsHaveLabAccess", String
                    .format("Successfully assigned Lab Unit Roles to %d Global Administrator users", assignedCount));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "ensureGlobalAdministratorsHaveLabAccess",
                    "Failed to assign lab roles to Global Administrator users: " + e.getMessage());
        }
    }

    /**
     * Check if user has AllLabUnits assignment with lab unit roles
     */
    private boolean hasAllLabUnitsAssignment(SystemUser user) {
        try {
            var userLabUnitRoles = userService.getUserLabUnitRoles(user.getId());
            if (userLabUnitRoles == null || userLabUnitRoles.getLabUnitRoleMap() == null) {
                return false;
            }

            return userLabUnitRoles.getLabUnitRoleMap().stream()
                    .anyMatch(roleMap -> "AllLabUnits".equals(roleMap.getLabUnit()) && roleMap.getRoles() != null
                            && !roleMap.getRoles().isEmpty());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "hasAllLabUnitsAssignment", String.format(
                    "Error checking AllLabUnits assignment for user %s: %s", user.getLoginName(), e.getMessage()));
            return false;
        }
    }

    /**
     * Assigns comprehensive lab access to a user using hybrid approach: 1. Lab Unit
     * Roles grouping (for role hierarchy) 2. AllLabUnits with all lab unit roles
     * (for UI visibility)
     */
    private void assignComprehensiveLabAccess(SystemUser user, Role labUnitRolesGroup, boolean assignRoleHierarchy,
            boolean assignAllLabUnits) {
        try {
            if (assignRoleHierarchy) {
                UserRole userRole = new UserRole();
                UserRolePK userRolePK = new UserRolePK();
                userRolePK.setSystemUserId(user.getId());
                userRolePK.setRoleId(labUnitRolesGroup.getId());
                userRole.setCompoundId(userRolePK);
                userRole.setUserName(user.getLoginName());
                userRole.setRoleName(Constants.LAB_ROLES_GROUP);
                userRole.setSysUserId("1");

                userRoleService.insert(userRole);
                LogEvent.logInfo(this.getClass().getSimpleName(), "assignComprehensiveLabAccess",
                        String.format("Assigned Lab Unit Roles hierarchy to user: %s", user.getLoginName()));
            }

            if (assignAllLabUnits) {
                assignAllLabUnitsRoles(user);
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), "assignComprehensiveLabAccess",
                    String.format("Successfully assigned comprehensive lab access to user: %s (%s)",
                            user.getLoginName(), user.getDisplayName()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "assignComprehensiveLabAccess", String.format(
                    "Failed to assign comprehensive lab access to user %s: %s", user.getLoginName(), e.getMessage()));
        }
    }

    /**
     * Assigns AllLabUnits with all available lab unit roles for UI visibility
     */
    private void assignAllLabUnitsRoles(SystemUser user) {
        try {
            Role labUnitRolesGroup = roleService.getRoleByName(Constants.LAB_ROLES_GROUP);
            if (labUnitRolesGroup == null) {
                LogEvent.logError(this.getClass().getSimpleName(), "assignAllLabUnitsRoles",
                        "Lab Unit Roles grouping not found");
                return;
            }

            List<Role> childRoles = roleService.getReferencingRoles(labUnitRolesGroup);
            if (childRoles == null || childRoles.isEmpty()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "assignAllLabUnitsRoles",
                        "No child roles found for Lab Unit Roles grouping");
                return;
            }

            Set<String> allLabUnitRoleIds = childRoles.stream().map(Role::getId).collect(Collectors.toSet());

            Map<String, Set<String>> allLabUnitsMap = new HashMap<>();
            allLabUnitsMap.put("AllLabUnits", allLabUnitRoleIds);

            userService.saveUserLabUnitRoles(user, allLabUnitsMap, "1");

            LogEvent.logInfo(this.getClass().getSimpleName(), "assignAllLabUnitsRoles",
                    String.format("Assigned %d lab unit roles to AllLabUnits for user: %s", allLabUnitRoleIds.size(),
                            user.getLoginName()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "assignAllLabUnitsRoles", String
                    .format("Failed to assign AllLabUnits roles to user %s: %s", user.getLoginName(), e.getMessage()));
        }
    }
}
