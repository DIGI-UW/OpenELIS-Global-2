package org.openelisglobal.systemmodule.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.security.access.prepost.PreAuthorize;

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
    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    SystemModule getSystemModuleByName(String name);
}
