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
import java.math.BigInteger;
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
import org.openelisglobal.systemusermodule.dao.RoleModuleDAO;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** */
@Component
@Transactional
@Qualifier("RoleModuleDAO")
public class RoleModuleDAOImpl extends BaseDAOImpl<RoleModule, String> implements RoleModuleDAO {

    public RoleModuleDAOImpl() {
        super(RoleModule.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(RoleModule systemUserModule) throws LIMSRuntimeException {
        try {
            RoleModule sysUserModule = entityManager.unwrap(Session.class).get(RoleModule.class,
                    systemUserModule.getId());
            if (sysUserModule != null) {
                PropertyUtils.copyProperties(systemUserModule, sysUserModule);
            } else {
                systemUserModule.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in RoleModule getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getAllPermissionModules() throws LIMSRuntimeException {
        List<RoleModule> list;
        try {
            String sql = "from RoleModule";
            Query<RoleModule> query = entityManager.unwrap(Session.class).createQuery(sql, RoleModule.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in RoleModule getAllSystemModules()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getAllPermissionModulesByAgentId(int systemUserId) throws LIMSRuntimeException {
        List<RoleModule> list;
        try {
            String sql = "from RoleModule s where s.role.id = :param";
            Query<RoleModule> query = entityManager.unwrap(Session.class).createQuery(sql, RoleModule.class);
            query.setParameter("param", systemUserId);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in RoleModule getAllRoleModulesBySystemUserId()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleModule> getPageOfPermissionModules(int startingRecNo) throws LIMSRuntimeException {
        List<RoleModule> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from RoleModule s order by s.role.id";
            Query<RoleModule> query = entityManager.unwrap(Session.class).createQuery(sql, RoleModule.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in RoleModule getPageOfRoleModules()", e);
        }

        return list;
    }

    public RoleModule readRoleModule(String idString) {
        RoleModule sysUserModule;
        try {
            sysUserModule = entityManager.unwrap(Session.class).get(RoleModule.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Gender readRoleModule(idString)", e);
        }

        return sysUserModule;
    }

    @Override
    @Transactional(readOnly = true)
    public RoleModule getRoleModuleByRoleAndModuleId(String roleId, String moduleId) {
        String sql = "From RoleModule rm where rm.systemModule.id = :moduleId and rm.role.id = :roleId";

        try {
            Query<RoleModule> query = entityManager.unwrap(Session.class).createQuery(sql, RoleModule.class);
            query.setParameter("moduleId", Integer.parseInt(moduleId));
            query.setParameter("roleId", Integer.parseInt(roleId));
            List<RoleModule> modules = query.list();
            return modules.isEmpty() ? new RoleModule() : modules.get(0);
        } catch (HibernateException e) {
            handleException(e, "getRoleModuleByRoleAndModuleId");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalPermissionModuleCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateRoleModuleExists(RoleModule roleModule) throws LIMSRuntimeException {

        try {

            List<RoleModule> list;

            String sql = "from RoleModule s where s.role.id = :param and s.systemModule.id = :param2 and s.id !="
                    + " :param3";
            Query<RoleModule> query = entityManager.unwrap(Session.class).createQuery(sql, RoleModule.class);
            query.setParameter("param", Integer.parseInt(roleModule.getRole().getId()));
            query.setParameter("param2", Integer.parseInt(roleModule.getSystemModule().getId()));

            String systemUserModuleId = "0";
            if (!StringUtil.isNullorNill(roleModule.getId())) {
                systemUserModuleId = roleModule.getId();
            }
            query.setParameter("param3", Integer.parseInt(systemUserModuleId));

            list = query.list();

            return list.size() > 0;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateRoleModuleExists()", e);
        }
    }

    @Override
    @Transactional
    public boolean doesUserHaveAnyModules(int userId) throws LIMSRuntimeException {
        try {
            String sql = "select count(*) from system_user_role where system_user_id = :userId";
            Query query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("userId", userId);
            int roleCount = ((BigInteger) query.uniqueResult()).intValue();
            return roleCount > 0;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in doesUserHaveAnyModules(int)", e);
        }
    }
}
