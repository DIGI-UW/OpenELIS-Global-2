package org.openelisglobal.systemuser.service;

import java.util.List;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.valueholder.UserRole;

public interface UnifiedSystemUserService {

    void deleteData(List<UserRole> userRoles, List<SystemUser> systemUsers, List<LoginUser> loginUsers,
            String sysUserId);
}
