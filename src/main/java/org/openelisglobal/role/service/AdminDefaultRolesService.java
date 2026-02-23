package org.openelisglobal.role.service;

import jakarta.annotation.PostConstruct;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for ensuring admin user has essential default roles. Uses
 * the role hierarchy system by assigning admin to "Lab Unit Roles" grouping,
 * which automatically grants access to all notebook and lab functionality.
 */
@Service
public class AdminDefaultRolesService {

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserRoleService userRoleService;

    /**
     * Ensures admin user has the "Lab Unit Roles" grouping role assigned. This
     * provides access to all notebook roles and lab functionality through the role
     * hierarchy system.
     */
    @PostConstruct
    @Transactional
    public void ensureAdminHasDefaultRoles() {
        try {
            SystemUser adminUser = systemUserService.getDataForLoginUser("admin");
            if (adminUser == null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "ensureAdminHasDefaultRoles",
                        "Admin user not found, skipping default role assignment");
                return;
            }

            Role labUnitRolesGroup = roleService.getRoleByName("Lab Unit Roles");
            if (labUnitRolesGroup == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "ensureAdminHasDefaultRoles",
                        "Lab Unit Roles grouping not found, cannot assign default roles to admin");
                return;
            }

            if (userRoleService.userInRole(adminUser.getId(), "Lab Unit Roles")) {
                LogEvent.logDebug(this.getClass().getSimpleName(), "ensureAdminHasDefaultRoles",
                        "Admin user already has Lab Unit Roles grouping, skipping assignment");
                return;
            }

            UserRole userRole = new UserRole();
            UserRolePK userRolePK = new UserRolePK();
            userRolePK.setSystemUserId(adminUser.getId());
            userRolePK.setRoleId(labUnitRolesGroup.getId());
            userRole.setCompoundId(userRolePK);
            userRole.setUserName(adminUser.getLoginName());
            userRole.setRoleName("Lab Unit Roles");
            userRole.setSysUserId("1");

            userRoleService.insert(userRole);

            LogEvent.logInfo(this.getClass().getSimpleName(), "ensureAdminHasDefaultRoles",
                    "Successfully assigned Lab Unit Roles grouping to admin user - admin now has access to all notebook and lab functionality");

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "ensureAdminHasDefaultRoles",
                    "Failed to assign default roles to admin user: " + e.getMessage());
        }
    }
}
