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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.systemusermodule.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.systemusermodule.dao.SystemUserModuleDAO;
import org.openelisglobal.systemusermodule.valueholder.SystemUserModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Component
@Transactional
@Qualifier(value = "SystemUserModuleDAO")
public class SystemUserModuleDAOImpl extends BaseDAOImpl<SystemUserModule, String> implements SystemUserModuleDAO {

    public SystemUserModuleDAOImpl() {
        super(SystemUserModule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemUserModule systemUserModule) throws LIMSRuntimeException {
        try {
            SystemUserModule sysUserModule = entityManager.unwrap(Session.class).get(SystemUserModule.class,
                    systemUserModule.getId());
            if (sysUserModule != null) {
                PropertyUtils.copyProperties(systemUserModule, sysUserModule);
            } else {
                systemUserModule.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserModule getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserModule> getAllPermissionModules() throws LIMSRuntimeException {
        List<SystemUserModule> list;
        try {
            String sql = "from SystemUserModule";
            Query<SystemUserModule> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserModule.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserModule getAllSystemModules()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserModule> getAllPermissionModulesByAgentId(int systemUserId) throws LIMSRuntimeException {
        List<SystemUserModule> list;
        try {
            String sql = "from SystemUserModule s where s.systemUser.id = :param";
            Query<SystemUserModule> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserModule.class);
            query.setParameter("param", systemUserId);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserModule getAllSystemUserModulesBySystemUserId()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserModule> getPageOfPermissionModules(int startingRecNo) throws LIMSRuntimeException {
        List<SystemUserModule> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from SystemUserModule s order by s.systemUser.id";
            Query<SystemUserModule> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserModule.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserModule getPageOfSystemUserModules()", e);
        }

        return list;
    }

    public SystemUserModule readSystemUserModule(String idString) {
        SystemUserModule sysUserModule = null;
        try {
            sysUserModule = entityManager.unwrap(Session.class).get(SystemUserModule.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Gender readSystemUserModule(idString)", e);
        }

        return sysUserModule;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPermissionModuleCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateSystemUserModuleExists(SystemUserModule systemUserModule) throws LIMSRuntimeException {
        try {

            List<SystemUserModule> list = new ArrayList<>();

            String sql = "from SystemUserModule s where s.systemUser.id = :param and s.systemModule.id = :param2"
                    + " and s.id != :param3";
            Query<SystemUserModule> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserModule.class);
            query.setParameter("param", systemUserModule.getSystemUser().getId());
            query.setParameter("param2", systemUserModule.getSystemModule().getId());

            String systemUserModuleId = "0";
            if (!StringUtil.isNullorNill(systemUserModule.getId())) {
                systemUserModuleId = systemUserModule.getId();
            }
            query.setParameter("param3", systemUserModuleId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateSystemUserModuleExists()", e);
        }
    }

    @Override
    public boolean doesUserHaveAnyModules(int userId) throws LIMSRuntimeException {
        List<SystemUserModule> userModuleList = getAllPermissionModulesByAgentId(userId);
        return userModuleList.size() > 0;
    }
}
