package org.openelisglobal.userrole.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;

public interface UserRoleService extends BaseObjectService<UserRole, UserRolePK> {

    List<String> getRoleIdsForUser(String userId);

    /**
     * Union of permitted module names across the user's roles — the same set
     * {@code ModuleAuthenticationInterceptor} authorizes pages against. Role-agent
     * resolution only: the USER mode of {@code permissions.agent} takes a different
     * branch in the interceptor and is set in no deployment.
     */
    Set<String> getAllPermittedPagesForUser(String userId);

    boolean userInRole(String userId, String roleName);

    boolean userInRole(String userId, Collection<String> roleNames);

    void saveOrUpdateUserLabUnitRoles(UserLabUnitRoles labRoles);

    UserLabUnitRoles getUserLabUnitRoles(String userId);

    void deleteLabUnitRoleMap(LabUnitRoleMap roleMap);

    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    List<String> getUserIdsForRole(String roleName);
}
