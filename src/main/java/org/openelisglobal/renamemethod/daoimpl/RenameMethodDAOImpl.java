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
package org.openelisglobal.renamemethod.daoimpl;

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
import org.openelisglobal.renamemethod.dao.RenameMethodDAO;
import org.openelisglobal.renamemethod.valueholder.RenameMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class RenameMethodDAOImpl extends BaseDAOImpl<RenameMethod, String> implements RenameMethodDAO {

    public RenameMethodDAOImpl() {
        super(RenameMethod.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RenameMethod> getMethods(String filter) throws LIMSRuntimeException {
        List<RenameMethod> list = new Vector<>();
        try {
            String sql = "from Method m where upper(m.methodName) like upper(:param) and m.isActive='Y' order by"
                    + " upper(m.methodName)";
            Query<RenameMethod> query = entityManager.unwrap(Session.class).createQuery(sql, RenameMethod.class);
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
    public List<RenameMethod> getAllInActiveMethods() {
        String sql = "from Method m where m.isActive = 'N'";

        try {
            Query<RenameMethod> query = entityManager.unwrap(Session.class).createQuery(sql, RenameMethod.class);
            List<RenameMethod> sections = query.list();
            return sections;
        } catch (HibernateException e) {
            handleException(e, "getAllInActiveMethods");
        }
        return null;
    }

    // bugzilla 1482
    @Override
    public boolean duplicateMethodExists(RenameMethod method) throws LIMSRuntimeException {
        try {

            List<RenameMethod> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from Method t where trim(lower(t.methodName)) = :param and t.id != :param2";
            Query<RenameMethod> query = entityManager.unwrap(Session.class).createQuery(sql, RenameMethod.class);
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
