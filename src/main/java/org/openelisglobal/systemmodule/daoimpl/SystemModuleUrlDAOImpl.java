package org.openelisglobal.systemmodule.daoimpl;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.URLUtil;
import org.openelisglobal.systemmodule.dao.SystemModuleUrlDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class SystemModuleUrlDAOImpl extends BaseDAOImpl<SystemModuleUrl, String> implements SystemModuleUrlDAO {

    public SystemModuleUrlDAOImpl() {
        super(SystemModuleUrl.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModuleUrl> getByRequest(HttpServletRequest request) {
        String pathNoSuffix = URLUtil.getReourcePathFromRequest(request);

        List<SystemModuleUrl> sysModUrls = getByUrlPath(pathNoSuffix);

        return sysModUrls;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModuleUrl> getByUrlPath(String urlPath) {
        List<SystemModuleUrl> list;
        try {
            String sql = "From SystemModuleUrl smu where smu.urlPath = :urlPath";
            Query<SystemModuleUrl> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModuleUrl.class);
            query.setParameter("urlPath", urlPath);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModuleUrl getByUrlPath()", e);
        }

        return list;
    }

    @Override
    public SystemModuleUrl getByModuleAndUrl(String moduleId, String urlPath) {
        SystemModuleUrl moduleUrl = null;
        if (GenericValidator.isBlankOrNull(moduleId)) {
            return moduleUrl;
        }
        try {
            String sql = "From SystemModuleUrl smu where smu.urlPath = :urlPath AND smu.systemModule ="
                    + " :systemModuleId";
            Query<SystemModuleUrl> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModuleUrl.class);
            query.setParameter("urlPath", urlPath);
            query.setParameter("systemModuleId", Integer.parseInt(moduleId));
            moduleUrl = query.getResultStream().findFirst().orElse(null);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModuleUrl getByUrlPath()", e);
        }

        return moduleUrl;
    }
}
