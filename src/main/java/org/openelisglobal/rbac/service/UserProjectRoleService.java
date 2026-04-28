package org.openelisglobal.rbac.service;

import java.util.List;
import org.openelisglobal.userrole.valueholder.UserProjectRole;

public interface UserProjectRoleService {

    List<UserProjectRole> getProjectRolesForUser(String systemUserId);

    List<String> getProjectIdsForUser(String systemUserId);

    List<String> getRoleNamesForUser(String systemUserId);

    boolean hasProjectRole(String systemUserId, String projectId, String roleName);

    void saveProjectRole(UserProjectRole projectRole);

    void revokeProjectRole(String systemUserId, String projectId, String roleName);
}
