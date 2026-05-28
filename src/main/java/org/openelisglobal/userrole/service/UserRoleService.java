package org.openelisglobal.userrole.service;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Spring Security login pipeline (CustomUserDetailsService.loadUserByUsername — no auth context exists), session setup, LoginPageController — read methods are public; write methods guarded by PRIV_USER_ROLE_MANAGE")
public interface UserRoleService extends BaseObjectService<UserRole, UserRolePK> {

    List<Integer> getRoleIdsForUser(String userId);

    boolean userInRole(String userId, String roleName);

    boolean userInRole(String userId, Collection<String> roleNames);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    void saveOrUpdateUserLabUnitRoles(UserLabUnitRoles labRoles);

    UserLabUnitRoles getUserLabUnitRoles(String userId);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    void deleteLabUnitRoleMap(LabUnitRoleMap roleMap);

    @PreAuthorize("hasAuthority('PRIV_USER_ROLE_MANAGE')")
    List<UserLabUnitRoles> getAllUserLabUnitRoles();

    List<String> getUserIdsForRole(String roleName);
}
