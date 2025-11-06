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
package org.openelisglobal.systemuser.daoimpl;

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
import org.openelisglobal.systemuser.dao.SystemUserDAO;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class SystemUserDAOImpl extends BaseDAOImpl<SystemUser, String> implements SystemUserDAO {

    public SystemUserDAOImpl() {
        super(SystemUser.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemUser systemUser) throws LIMSRuntimeException {
        try {
            SystemUser sysUser = entityManager.unwrap(Session.class).get(SystemUser.class, systemUser.getId());

            if (sysUser != null) {
                PropertyUtils.copyProperties(systemUser, sysUser);
            } else {
                systemUser.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUser getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUser> getAllSystemUsers() throws LIMSRuntimeException {
        List<SystemUser> list;
        try {
            String sql = "from SystemUser";
            Query<SystemUser> query = entityManager.unwrap(Session.class).createQuery(sql, SystemUser.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUser getAllSystemUsers()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUser> getPageOfSystemUsers(int startingRecNo) throws LIMSRuntimeException {
        List<SystemUser> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from SystemUser s order by s.lastName, s.firstName";
            Query<SystemUser> query = entityManager.unwrap(Session.class).createQuery(sql, SystemUser.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUser getPageOfSystemUsers()", e);
        }

        return list;
    }

    public SystemUser readSystemUser(String idString) {
        SystemUser su = null;
        try {
            su = entityManager.unwrap(Session.class).get(SystemUser.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUser readSystemUser()", e);
        }

        return su;
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSystemUserCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 1482
    @Override
    public boolean duplicateSystemUserExists(SystemUser systemUser) throws LIMSRuntimeException {
        try {

            List<SystemUser> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from SystemUser t where trim(lower(t.lastName)) = :param and trim(lower(t.firstName)) ="
                    + " :param2 and t.id != :id";
            Query<SystemUser> query = entityManager.unwrap(Session.class).createQuery(sql, SystemUser.class);
            query.setParameter("param", systemUser.getLastName().toLowerCase().trim());
            query.setParameter("param2", systemUser.getFirstName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated yet
            String sysUserId = "0";
            if (!StringUtil.isNullorNill(systemUser.getId())) {
                sysUserId = systemUser.getId();
            }
            query.setParameter("id", Integer.parseInt(sysUserId));

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateSystemUserExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SystemUser getDataForLoginUser(String userName) throws LIMSRuntimeException {
        List<SystemUser> list;
        try {
            String sql = "from SystemUser where login_Name = :name";
            Query<SystemUser> query = entityManager.unwrap(Session.class).createQuery(sql, SystemUser.class);
            query.setParameter("name", userName);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUser getDataForLoginUser()", e);
        }

        return list.size() > 0 ? list.get(0) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemUser getUserById(String userId) throws LIMSRuntimeException {
        try {
            SystemUser sysUser = entityManager.unwrap(Session.class).get(SystemUser.class, userId);
            return sysUser;
        } catch (RuntimeException e) {
            handleException(e, "getUserById");
        }

        return null;
    }
}
