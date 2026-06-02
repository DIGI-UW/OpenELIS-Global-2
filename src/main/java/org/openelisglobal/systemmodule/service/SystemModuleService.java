package org.openelisglobal.systemmodule.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "PluginPermissionService (analyzer registration, all users), URL auth resolution — getSystemModuleByName is public; admin methods guarded by PRIV_SYSTEM_CONFIGURE")
public interface SystemModuleService extends BaseObjectService<SystemModule, String> {

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void getData(SystemModule systemModule);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    Integer getTotalSystemModuleCount();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SystemModule> getPageOfSystemModules(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<SystemModule> getAllSystemModules();

    // Called during URL/request auth resolution — not admin-only
    SystemModule getSystemModuleByName(String name);
}
