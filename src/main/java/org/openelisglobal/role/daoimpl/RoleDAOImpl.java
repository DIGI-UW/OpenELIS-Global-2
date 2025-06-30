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
package org.openelisglobal.role.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.role.dao.RoleDAO;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class RoleDAOImpl extends BaseDAOImpl<Role, String> implements RoleDAO {

    public RoleDAOImpl() {
        super(Role.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Role role) throws LIMSRuntimeException {
        try {
            Role tmpRole = entityManager.unwrap(Session.class).get(Role.class, role.getId());
            if (tmpRole != null) {
                PropertyUtils.copyProperties(role, tmpRole);
            } else {
                role.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Role getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() throws LIMSRuntimeException {
        List<Role> list = null;
        try {
            String sql = "from Role";
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            list = query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllRoles");
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllActiveRoles() throws LIMSRuntimeException {
        List<Role> list = null;
        try {
            String sql = "from Role r where r.active = true";
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            list = query.list();
        } catch (HibernateException e) {
            handleException(e, "getAllActiveRoles");
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getPageOfRoles(int startingRecNo) throws LIMSRuntimeException {
        List<Role> list = null;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from Role r order by r.id";
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Role getPageOfRoles()", e);
        }

        return list;
    }

    public Role readRole(String idString) {
        Role recoveredRole = null;
        try {
            recoveredRole = entityManager.unwrap(Session.class).get(Role.class, idString);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Role readRole()", e);
        }

        return recoveredRole;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getReferencingRoles(Role role) throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(role.getId())) {
            return new ArrayList<>();
        }

        List<Role> list = null;
        try {
            String sql = "from Role where grouping_parent = :parent";
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            query.setParameter("parent", Integer.parseInt(role.getId()));

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Role getReferencingRoles()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleByName(String name) throws LIMSRuntimeException {
        String sql = "from Role r where trim(r.name) = :name";

        try {
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            query.setParameter("name", name);
            Role role = query.setMaxResults(1).uniqueResult();
            return role;
        } catch (HibernateException e) {
            handleException(e, "getRoleByName");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(String roleId) throws LIMSRuntimeException {
        String sql = "from Role r where r.id = :id";

        try {
            Query<Role> query = entityManager.unwrap(Session.class).createQuery(sql, Role.class);
            query.setParameter("id", Integer.parseInt(roleId));
            Role role = query.uniqueResult();
            return role;
        } catch (HibernateException e) {
            handleException(e, "getRoleById");
        }
        return null;
    }
}
