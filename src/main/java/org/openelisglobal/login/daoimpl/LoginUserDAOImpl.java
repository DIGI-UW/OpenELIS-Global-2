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
package org.openelisglobal.login.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.login.dao.LoginUserDAO;
import org.openelisglobal.login.valueholder.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hung Nguyen (Hung.Nguyen@health.state.mn.us)
 */
@Component
@Transactional
public class LoginUserDAOImpl extends BaseDAOImpl<LoginUser, Integer> implements LoginUserDAO {

    public LoginUserDAOImpl() {
        super(LoginUser.class);
    }

    @Override
    public boolean duplicateLoginNameExists(LoginUser login) throws LIMSRuntimeException {
        try {

            List<LoginUser> list = new ArrayList<>();

            String sql = "from LoginUser l where trim(lower(l.loginName)) = :loginName and l.id != :loginId";
            Query<LoginUser> query = entityManager.unwrap(Session.class).createQuery(sql, LoginUser.class);
            query.setParameter("loginName", login.getLoginName().toLowerCase().trim());

            Integer loginId = 0;
            if (null != login.getId()) {
                loginId = login.getId();
            }

            query.setParameter("loginId", loginId);

            list = query.list();

            return list.size() > 0;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateLoginNameExists()", e);
        }
    }

    /**
     * Get the password expiration day
     *
     * @param login the login object
     * @return type integer the password expiration day
     */
    @Override
    @Transactional
    public int getPasswordExpiredDayNo(LoginUser login) throws LIMSRuntimeException {
        int retVal = 0;
        try {
            String sql = "SELECT \n" + "                    floor(current_date-password_expired_dt)*-1 as cnt\n"
                    + "                FROM Login_User l where l.LOGIN_NAME = :loginName ";
            Object obj = entityManager.unwrap(Session.class).createNativeQuery(sql)
                    .setParameter("loginName", login.getLoginName()).uniqueResult();
            if (obj != null) {
                retVal = (int) Float.parseFloat(obj.toString());
            }
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getPasswordExpiredDayNo()", e);
        }

        return retVal;
    }

    /**
     * Get the system user id
     *
     * @param login the login object
     * @return type integer the system user id
     */
    @Override
    @Transactional
    public int getSystemUserId(LoginUser login) throws LIMSRuntimeException {
        int retVal = 0;
        try {
            String sql = "SELECT id from System_User su Where su.login_name = :loginName and su.is_active='Y'";
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("loginName", login.getLoginName());
            Object obj = query.uniqueResult();

            if (obj != null) {
                retVal = Integer.parseInt(obj.toString());
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getSystemUserId()", e);
        } finally {
        }

        return retVal;
    }
}
