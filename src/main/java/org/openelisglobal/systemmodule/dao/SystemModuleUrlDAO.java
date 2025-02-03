package org.openelisglobal.systemmodule.dao;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

public interface SystemModuleUrlDAO extends BaseDAO<SystemModuleUrl, String> {

    public List<SystemModuleUrl> getByRequest(HttpServletRequest request);

    public List<SystemModuleUrl> getByUrlPath(String urlPath);

    public SystemModuleUrl getByModuleAndUrl(String moduleId, String urlPath);
}
