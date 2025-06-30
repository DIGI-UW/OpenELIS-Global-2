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
package org.openelisglobal.testanalyte.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.dao.TestAnalyteDAO;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TestAnalyteDAOImpl extends BaseDAOImpl<TestAnalyte, String> implements TestAnalyteDAO {

    public TestAnalyteDAOImpl() {
        super(TestAnalyte.class);
    }

    @Override
    @Transactional(readOnly = true)
    public TestAnalyte getData(TestAnalyte testAnalyte) throws LIMSRuntimeException {
        try {
            TestAnalyte anal = entityManager.unwrap(Session.class).get(TestAnalyte.class, testAnalyte.getId());
            if (anal != null) {
                PropertyUtils.copyProperties(testAnalyte, anal);
            } else {
                testAnalyte.setId(null);
            }

            return anal;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAnalyte> getAllTestAnalytes() throws LIMSRuntimeException {
        List<TestAnalyte> list;
        try {
            String sql = "from TestAnalyte";
            org.hibernate.query.Query<TestAnalyte> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestAnalyte.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte getAllTestAnalytes()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAnalyte> getPageOfTestAnalytes(int startingRecNo) throws LIMSRuntimeException {
        List<TestAnalyte> list;
        try {
            // if(startingRecNo < 1) {
            // startingRecNo = 1;
            // }
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            String sql = "from TestAnalyte t order by t.id";
            org.hibernate.query.Query<TestAnalyte> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestAnalyte.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte getPageOfTestAnalytes()", e);
        }

        return list;
    }

    public TestAnalyte readTestAnalyte(String idString) {
        TestAnalyte ta = null;
        try {
            ta = entityManager.unwrap(Session.class).get(TestAnalyte.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte readTestAnalyte()", e);
        }

        return ta;
    }

    // this is for autocomplete
    // TODO: need to convert to hibernate ( not in use??? )
    @Override
    @Transactional(readOnly = true)
    public List<TestAnalyte> getTestAnalytes(String filter) throws LIMSRuntimeException {

        return null;
        /*
         * try { DatabaseSession aSession = getSession(); ReadAllQuery query = new
         * ReadAllQuery(TestAnalyte.class); ExpressionBuilder builder = new
         * ExpressionBuilder(); Expression exp = null; if (filter != null) { exp =
         * builder.get("testAnalyteName").toUpperCase().like( filter.toUpperCase() +
         * "%"); } else { exp = builder.get("testAnalyteName").like(filter + "%"); }
         *
         * // Expression exp1 = builder.get("isActive").equal(true); // exp =
         * exp.and(exp1);
         *
         * query.setSelectionCriteria(exp);
         * query.addAscendingOrdering("testAnalyteName");
         *
         * LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
         * "This is query " + query.getSQLString()); List testAnalytes = (Vector)
         * aSession.executeQuery(query);
         *
         * LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
         * "This is size of list retrieved " + testAnalytes.size() + " " +
         * testAnalytes.get(0)); return testAnalytes;
         *
         * } catch (RuntimeException e) { throw new LIMSRuntimeException(
         * "Error in TestAnalyte getTestAnalytes(String filter)", e); }
         */
    }

    @Override
    @Transactional(readOnly = true)
    public TestAnalyte getTestAnalyteById(TestAnalyte testAnalyte) throws LIMSRuntimeException {
        TestAnalyte newTestAnalyte;
        try {
            newTestAnalyte = entityManager.unwrap(Session.class).get(TestAnalyte.class, testAnalyte.getId());
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte getTestAnalyteById()", e);
        }

        return newTestAnalyte;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAnalyte> getAllTestAnalytesPerTest(Test test) throws LIMSRuntimeException {
        List<TestAnalyte> list;

        if (test == null || StringUtil.isNullorNill(test.getId())) {
            return new ArrayList<>();
        }

        try {
            String sql = "from TestAnalyte t where t.test = :testId order by t.sortOrder asc";
            org.hibernate.query.Query<TestAnalyte> query = entityManager.unwrap(Session.class).createQuery(sql,
                    TestAnalyte.class);
            query.setParameter("testId", Integer.parseInt(test.getId()));

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestAnalyte getAllTestAnalytesPerTest()", e);
        }

        return list;
    }
}
