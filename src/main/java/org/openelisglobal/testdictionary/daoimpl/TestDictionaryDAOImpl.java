/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.testdictionary.daoimpl;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.testdictionary.dao.TestDictionaryDAO;
import org.openelisglobal.testdictionary.valueholder.TestDictionary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TestDictionaryDAOImpl extends BaseDAOImpl<TestDictionary, String> implements TestDictionaryDAO {

    public TestDictionaryDAOImpl() {
        super(TestDictionary.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TestDictionary getTestDictionaryForTestId(String testId) throws LIMSRuntimeException {
        String sql = "FROM TestDictionary td where td.testId = :testId";
        try {
            Query<TestDictionary> query = entityManager.unwrap(Session.class).createQuery(sql, TestDictionary.class);
            query.setParameter("testId", Integer.parseInt(testId));
            TestDictionary testDictionary = query.uniqueResult();
            return testDictionary;
        } catch (HibernateException e) {
            handleException(e, "getTestDictionaryForTestId");
        }
        return null;
    }
}
