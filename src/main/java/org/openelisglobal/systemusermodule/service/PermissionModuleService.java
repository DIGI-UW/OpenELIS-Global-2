package org.openelisglobal.systemusermodule.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "CustomFormAuthenticationSuccessHandler (session setup post-login), UserModuleService (request auth) — read methods are public; write methods guarded by PRIV_ROLE_MANAGE")
public interface PermissionModuleService<T extends PermissionModule> extends BaseObjectService<T, String> {

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    void getData(T permissionModule);

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<T> getAllPermissionModules();

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    Integer getTotalPermissionModuleCount();

    @PreAuthorize("hasAuthority('PRIV_ROLE_MANAGE')")
    List<T> getPageOfPermissionModules(int startingRecNo);

    // Called during auth to resolve what modules a user/role can access
    List<T> getAllPermissionModulesByAgentId(int systemUserId);

    boolean doesUserHaveAnyModules(int userId);

    Set<String> getAllPermittedPagesFromAgentId(int roleId);
}
