package org.openelisglobal.systemmodule.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

public interface SystemModuleUrlService extends BaseObjectService<SystemModuleUrl, String> {
    List<SystemModuleUrl> getByUrlPath(String urlPath);

    List<SystemModuleUrl> getByRequest(HttpServletRequest request);

    SystemModuleUrl getByModuleAndUrl(String moduleId, String urlPath);
}
