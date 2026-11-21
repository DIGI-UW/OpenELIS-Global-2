package org.openelisglobal.systemuser.service;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnifiedSystemUserServiceImpl implements UnifiedSystemUserService {

    @Autowired
    SystemUserService systemUserService;
    @Autowired
    LoginUserService loginService;
    @Autowired
    UserRoleService userRoleService;

    @Autowired
    private RoleService roleService;

    /** Apply the same existing-account boundary to saves and deactivations. */
    @Override
    @Transactional(readOnly = true)
    public boolean canManageAccount(String callerId, String systemUserId, Integer loginUserId) {
        Role administrator = roleService.getRoleByName(Constants.ROLE_GLOBAL_ADMIN);
        if (administrator == null) {
            administrator = roleService.getRoleByName("Admin");
        }
        if (administrator == null) {
            return false;
        }
        LoginUser login = loginUserId == null ? null : loginService.get(loginUserId);
        if (loginUserId != null && login == null) {
            return false;
        }
        SystemUser owner = login == null ? null : systemUserService.getDataForLoginUser(login.getLoginName());
        if (owner != null && !GenericValidator.isBlankOrNull(systemUserId) && !owner.getId().equals(systemUserId)) {
            return false;
        }
        String roleId = administrator.getId();
        if (userRoleService.getRoleIdsForUser(callerId).contains(roleId)) {
            return true;
        }
        if (login != null && ("Y".equals(login.getIsAdmin()) || "admin".equals(login.getLoginName()))) {
            return false;
        }
        return (GenericValidator.isBlankOrNull(systemUserId)
                || !userRoleService.getRoleIdsForUser(systemUserId).contains(roleId))
                && (owner == null || !userRoleService.getRoleIdsForUser(owner.getId()).contains(roleId));
    }

    @Override
    @Transactional
    public void deleteData(List<UserRole> userRoles, List<SystemUser> systemUsers, List<LoginUser> loginUsers,
            String sysUserId) {
        userRoleService.deleteAll(userRoles);

        for (SystemUser systemUser : systemUsers) {
            // we're not going to actually delete them to preserve auditing
            systemUser = systemUserService.get(systemUser.getId());
            systemUser.setSysUserId(sysUserId);
            systemUserService.delete(systemUser);
        }

        loginService.deleteAll(loginUsers);
    }
}
