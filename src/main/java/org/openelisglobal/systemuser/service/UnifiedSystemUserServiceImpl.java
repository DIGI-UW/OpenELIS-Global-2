package org.openelisglobal.systemuser.service;

import java.util.List;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
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
