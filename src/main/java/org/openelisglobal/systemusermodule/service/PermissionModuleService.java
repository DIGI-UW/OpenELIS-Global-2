package org.openelisglobal.systemusermodule.service;

import java.util.List;
import java.util.Set;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    List<T> getAllPermissionModulesByAgentId(int systemUserId);

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    boolean doesUserHaveAnyModules(int userId);

    @PreAuthorize("hasAuthority('PRIV_ROLE_VIEW')")
    Set<String> getAllPermittedPagesFromAgentId(int roleId);
}
