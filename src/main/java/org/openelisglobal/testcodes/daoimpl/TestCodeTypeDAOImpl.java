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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.testcodes.daoimpl;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testcodes.dao.TestCodeTypeDAO;
import org.openelisglobal.testcodes.valueholder.TestCodeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestCodeTypeDAOImpl extends BaseDAOImpl<TestCodeType, String> implements TestCodeTypeDAO {

    public TestCodeTypeDAOImpl() {
        super(TestCodeType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCodeType getTestCodeTypeByName(String name) throws LIMSRuntimeException {
        String sql = "from TestCodeType et where et.schemaName = :name";

        try {
            Query<TestCodeType> query = entityManager.unwrap(Session.class).createQuery(sql, TestCodeType.class);
            query.setParameter("name", name);
            TestCodeType et = query.uniqueResult();
            return et;
        } catch (HibernateException e) {
            handleException(e, "getTestCodeTypeByName");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TestCodeType getTestCodeTypeById(String id) throws LIMSRuntimeException {
        String sql = "from TestCodeType et where et.id = :id";

        try {
            Query<TestCodeType> query = entityManager.unwrap(Session.class).createQuery(sql, TestCodeType.class);
            query.setParameter("id", id);
            TestCodeType et = query.uniqueResult();
            return et;
        } catch (HibernateException e) {
            handleException(e, "getTestCodeTypeByName");
        }
        return null;
    }
}
