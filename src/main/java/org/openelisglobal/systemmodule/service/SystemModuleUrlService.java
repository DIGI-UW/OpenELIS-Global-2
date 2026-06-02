package org.openelisglobal.systemmodule.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

@CrossDomainService(callers = "Called during HTTP request processing to resolve URL-to-module mappings for auth — invoked before any user privilege context is established")
public interface SystemModuleUrlService extends BaseObjectService<SystemModuleUrl, String> {
    List<SystemModuleUrl> getByUrlPath(String urlPath);

    List<SystemModuleUrl> getByRequest(HttpServletRequest request);

    SystemModuleUrl getByModuleAndUrl(String moduleId, String urlPath);
}
