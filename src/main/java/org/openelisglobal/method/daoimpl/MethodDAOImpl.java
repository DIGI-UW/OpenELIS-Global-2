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
package org.openelisglobal.method.daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.method.dao.MethodDAO;
import org.openelisglobal.method.valueholder.Method;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class MethodDAOImpl extends BaseDAOImpl<Method, String> implements MethodDAO {

    public MethodDAOImpl() {
        super(Method.class);
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<Method> getMethods(String filter) throws LIMSRuntimeException {
        List<Method> list = new Vector<>();
        try {
            String sql = "from Method m where upper(m.methodName) like upper(:param) and m.isActive='Y' order by"
                    + " upper(m.methodName)";
            Query<Method> query = entityManager.unwrap(Session.class).createQuery(sql, Method.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Method getMethods(String filter)", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getAllActiveMethods() {
        String sql = "from Method m where m.isActive = 'Y'";

        try {
            Query<Method> query = entityManager.unwrap(Session.class).createQuery(sql, Method.class);
            List<Method> sections = query.list();
            return sections;
        } catch (HibernateException e) {
            handleException(e, "getAllActiveMethods");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getAllInActiveMethods() {
        String sql = "from Method m where m.isActive = 'N'";

        try {
            Query<Method> query = entityManager.unwrap(Session.class).createQuery(sql, Method.class);
            List<Method> sections = query.list();
            return sections;
        } catch (HibernateException e) {
            handleException(e, "getAllInActiveMethods");
        }
        return null;
    }

    // bugzilla 1482
    @Override
    public boolean duplicateMethodExists(Method method) throws LIMSRuntimeException {
        try {

            List<Method> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Method t where trim(lower(t.methodName)) = :param AND t.id != CAST(:param2 AS integer)";
            Query<Method> query = entityManager.unwrap(Session.class).createQuery(sql, Method.class);
            query.setParameter("param", method.getMethodName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated yet
            String methodId = "0";
            if (!StringUtil.isNullorNill(method.getId())) {
                methodId = method.getId();
            }
            query.setParameter("param2", methodId);

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateMethodExists()", e);
        }
    }
}
