package org.openelisglobal.systemmodule.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
public interface SystemModuleParamService extends BaseObjectService<SystemModuleParam, String> {
}
