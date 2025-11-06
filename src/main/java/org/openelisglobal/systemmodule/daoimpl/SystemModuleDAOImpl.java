/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.systemmodule.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.systemmodule.dao.SystemModuleDAO;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Component
@Transactional
public class SystemModuleDAOImpl extends BaseDAOImpl<SystemModule, String> implements SystemModuleDAO {

    public SystemModuleDAOImpl() {
        super(SystemModule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemModule systemModule) throws LIMSRuntimeException {
        try {
            SystemModule sysModule = entityManager.unwrap(Session.class).get(SystemModule.class, systemModule.getId());
            if (sysModule != null) {
                PropertyUtils.copyProperties(systemModule, sysModule);
            } else {
                systemModule.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModule getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModule> getAllSystemModules() throws LIMSRuntimeException {
        List<SystemModule> list;
        try {
            String sql = "from SystemModule";
            Query<SystemModule> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModule.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModule getAllSystemModules()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemModule> getPageOfSystemModules(int startingRecNo) throws LIMSRuntimeException {
        List<SystemModule> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from SystemModule s order by s.systemModuleName";
            Query<SystemModule> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModule.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModule getPageOfSystemModules()", e);
        }

        return list;
    }

    public SystemModule readSystemModule(String idString) {
        SystemModule sysModule;
        try {
            sysModule = entityManager.unwrap(Session.class).get(SystemModule.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemModule readSystemModule(idString)", e);
        }

        return sysModule;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSystemModuleCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemModule getSystemModuleByName(String name) throws LIMSRuntimeException {
        String sql = "From SystemModule sm where sm.systemModuleName = :name";

        try {
            Query<SystemModule> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModule.class);
            query.setParameter("name", name);
            SystemModule module = query.uniqueResult();
            return module;
        } catch (HibernateException e) {
            handleException(e, "getSystemModuleByName");
        }
        return null;
    }

    @Override
    public boolean duplicateSystemModuleExists(SystemModule systemModule) throws LIMSRuntimeException {
        try {

            List<SystemModule> list;

            String sql = "from SystemModule s where trim(s.systemModuleName) = :moduleName and s.id != :moduleId";
            Query<SystemModule> query = entityManager.unwrap(Session.class).createQuery(sql, SystemModule.class);
            query.setParameter("moduleName", systemModule.getSystemModuleName().trim());

            String systemModuleId = "0";
            if (!StringUtil.isNullorNill(systemModule.getId())) {
                systemModuleId = systemModule.getId();
            }
            query.setParameter("moduleId", Integer.parseInt(systemModuleId));

            list = query.list();

            return list.size() > 0;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateSystemModuleExists()", e);
        }
    }
}
