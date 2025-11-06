package org.openelisglobal.systemmodule.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.systemmodule.dao.SystemModuleUrlDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemModuleUrlServiceImpl extends AuditableBaseObjectServiceImpl<SystemModuleUrl, String>
        implements SystemModuleUrlService {
    @Autowired
    protected SystemModuleUrlDAO baseObjectDAO;

    SystemModuleUrlServiceImpl() {
        super(SystemModuleUrl.class);
    }

    @Override
    protected SystemModuleUrlDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModuleUrl> getByRequest(HttpServletRequest request) {
        return baseObjectDAO.getByRequest(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModuleUrl> getByUrlPath(String urlPath) {
        return getBaseObjectDAO().getByUrlPath(urlPath);
    }

    @Override
    public SystemModuleUrl getByModuleAndUrl(String moduleId, String urlPath) {
        return getBaseObjectDAO().getByModuleAndUrl(moduleId, urlPath);
    }
}
