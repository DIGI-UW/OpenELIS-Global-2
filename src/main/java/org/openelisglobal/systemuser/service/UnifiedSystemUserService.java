package org.openelisglobal.systemuser.service;

import java.util.List;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UnifiedSystemUserService {

    @PreAuthorize("hasAuthority('PRIV_USER_MANAGE')")
    void deleteData(List<UserRole> userRoles, List<SystemUser> systemUsers, List<LoginUser> loginUsers,
            String sysUserId);
}
