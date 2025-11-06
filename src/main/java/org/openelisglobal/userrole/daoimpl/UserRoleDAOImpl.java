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
package org.openelisglobal.userrole.daoimpl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.userrole.dao.UserRoleDAO;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserRoleDAOImpl extends BaseDAOImpl<UserRole, UserRolePK> implements UserRoleDAO {

    public UserRoleDAOImpl() {
        super(UserRole.class);
    }

    @Override
    @Transactional
    public List<String> getRoleIdsForUser(String userId) throws LIMSRuntimeException {
        List<String> userRoles;

        try {
            String sql = "select cast(role_id AS varchar) from system_user_role where system_user_id = :userId";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("userId", Integer.parseInt(userId));
            userRoles = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserRoleDAOImpl getUserRolesForUser()", e);
        }
        return userRoles;
    }

    @Override
    @Transactional
    public boolean userInRole(String userId, String roleName) throws LIMSRuntimeException {
        boolean inRole;
        try {
            String sql = "select count(*) from system_user_role sur " + "join system_role as sr on sr.id = sur.role_id "
                    + "where sur.system_user_id = :userId and sr.name = :roleName";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("userId", Integer.parseInt(userId));
            query.setParameter("roleName", roleName);
            int result = ((BigInteger) query.uniqueResult()).intValue();

            inRole = result != 0;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserRoleDAOImpl userInRole()", e);
        }

        return inRole;
    }

    @Override
    @Transactional
    public boolean userInRole(String userId, Collection<String> roleNames) throws LIMSRuntimeException {
        boolean inRole;

        try {
            String sql = "select count(*) from system_user_role sur " + "join system_role as sr on sr.id = sur.role_id "
                    + "where sur.system_user_id = :userId and sr.name in (:roleNames)";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("userId", Integer.parseInt(userId));
            query.setParameterList("roleNames", roleNames);
            int result = ((BigInteger) query.uniqueResult()).intValue();

            inRole = result != 0;
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserRoleDAOImpl userInRole()", e);
        }

        return inRole;
    }

    @Override
    public void deleteLabUnitRoleMap(LabUnitRoleMap roleMap) {
        try {
            entityManager.unwrap(Session.class).delete(roleMap);
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserRoleDAOImpl userInRole()", e);
        }
    }

    @Override
    public List<String> getUserIdsForRole(String roleName) {
        List<String> userIds = new ArrayList<>();
        try {
            String sql = "select cast(system_user_id AS varchar) from system_user_role sur join system_role as sr on sr.id = sur.role_id where sr.name = :roleName";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("roleName", roleName);

            userIds = query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserRoleDAOImpl userInRole()", e);
        }
        return userIds;
    }
}
