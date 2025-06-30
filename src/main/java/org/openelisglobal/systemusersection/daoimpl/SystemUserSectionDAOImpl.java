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
package org.openelisglobal.systemusersection.daoimpl;

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
import org.openelisglobal.systemusersection.dao.SystemUserSectionDAO;
import org.openelisglobal.systemusersection.valueholder.SystemUserSection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Component
@Transactional
public class SystemUserSectionDAOImpl extends BaseDAOImpl<SystemUserSection, String> implements SystemUserSectionDAO {

    public SystemUserSectionDAOImpl() {
        super(SystemUserSection.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(SystemUserSection systemUserSection) throws LIMSRuntimeException {
        try {
            SystemUserSection sysUserSection = entityManager.unwrap(Session.class).get(SystemUserSection.class,
                    systemUserSection.getId());
            if (sysUserSection != null) {
                PropertyUtils.copyProperties(systemUserSection, sysUserSection);
            } else {
                systemUserSection.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserSection getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserSection> getAllSystemUserSections() throws LIMSRuntimeException {
        List<SystemUserSection> list;
        try {
            String sql = "from SystemUserSection";
            Query<SystemUserSection> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserSection.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserSection getAllSystemModules()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserSection> getAllSystemUserSectionsBySystemUserId(int systemUserId)
            throws LIMSRuntimeException {
        List<SystemUserSection> list;
        try {
            String sql = "from SystemUserSection s where s.systemUser.id = :param";
            Query<SystemUserSection> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserSection.class);
            query.setParameter("param", systemUserId);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserSection getAllSystemUserSectionsBySystemUserId()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemUserSection> getPageOfSystemUserSections(int startingRecNo) throws LIMSRuntimeException {
        List<SystemUserSection> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from SystemUserSection s ";
            Query<SystemUserSection> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserSection.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserSection getPageOfSystemUserSections()", e);
        }

        return list;
    }

    public SystemUserSection readSystemUserSection(String idString) {
        SystemUserSection sysUserSection = null;
        try {
            sysUserSection = entityManager.unwrap(Session.class).get(SystemUserSection.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in SystemUserSection readSystemUserSection(idString)", e);
        }

        return sysUserSection;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSystemUserSectionCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateSystemUserSectionExists(SystemUserSection systemUserSection) throws LIMSRuntimeException {
        try {

            List<SystemUserSection> list = new ArrayList<>();

            String sql = "from SystemUserSection s where s.systemUser.id = :param and s.testSection.id = :param2"
                    + " and s.id != :param3";
            Query<SystemUserSection> query = entityManager.unwrap(Session.class).createQuery(sql,
                    SystemUserSection.class);
            query.setParameter("param", systemUserSection.getSystemUser().getId());
            query.setParameter("param2", systemUserSection.getTestSection().getId());

            String systemUserSectionId = "0";
            if (!StringUtil.isNullorNill(systemUserSection.getId())) {
                systemUserSectionId = systemUserSection.getId();
            }
            query.setParameter("param3", systemUserSectionId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateSystemUserSectionExists()", e);
        }
    }
}
