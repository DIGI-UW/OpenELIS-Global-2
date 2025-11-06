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
package org.openelisglobal.testresult.daoimpl;

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
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.dao.TestResultDAO;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TestResultDAOImpl extends BaseDAOImpl<TestResult, String> implements TestResultDAO {

    public TestResultDAOImpl() {
        super(TestResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestResult testResult) throws LIMSRuntimeException {
        try {
            TestResult tr = entityManager.unwrap(Session.class).get(TestResult.class, testResult.getId());
            if (tr != null) {
                PropertyUtils.copyProperties(testResult, tr);
            } else {
                testResult.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getAllTestResults() throws LIMSRuntimeException {
        List<TestResult> list;
        try {
            String sql = "from TestResult";
            Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getAllTestResults()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getPageOfTestResults(int startingRecNo) throws LIMSRuntimeException {
        List<TestResult> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            String sql = "from TestResult t order by t.test.description";
            Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getPageOfTestResults()", e);
        }

        return list;
    }

    public TestResult readTestResult(String idString) {
        TestResult tr;
        try {
            tr = entityManager.unwrap(Session.class).get(TestResult.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult readTestResult()", e);
        }

        return tr;
    }

    @Override
    @Transactional(readOnly = true)
    public TestResult getTestResultById(TestResult testResult) throws LIMSRuntimeException {
        TestResult newTestResult;
        try {
            newTestResult = entityManager.unwrap(Session.class).get(TestResult.class, testResult.getId());
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getTestResultById()", e);
        }

        return newTestResult;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getTestResultsByTestAndResultGroup(TestAnalyte testAnalyte) throws LIMSRuntimeException {
        List<TestResult> list = new ArrayList<>();
        try {
            if (testAnalyte.getId() != null && testAnalyte.getResultGroup() != null) {
                // bugzilla 1845 added testResult sortOrder
                String sql = "from TestResult t where t.test = :testId and t.resultGroup = :resultGroup order by"
                        + " t.sortOrder";
                Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);

                query.setParameter("testId", Integer.parseInt(testAnalyte.getTest().getId()));
                query.setParameter("resultGroup", Integer.parseInt(testAnalyte.getResultGroup()));

                list = query.list();
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getTestResultByTestAndResultGroup()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getAllActiveTestResultsPerTest(Test test) throws LIMSRuntimeException {
        if (test == null || (test.getId() == null) || (test.getId().length() == 0)) {
            return null;
        }

        List<TestResult> list;
        try {
            String sql = "from TestResult t where t.test = :testId and t.isActive = true order by t.resultGroup,"
                    + " t.id asc";
            Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);
            query.setParameter("testId", Integer.parseInt(test.getId()));

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestResult getAllActiveTestResultsPerTest()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public TestResult getTestResultsByTestAndDictonaryResult(String testId, String result) throws LIMSRuntimeException {
        if (StringUtil.isInteger(result)) {
            List<TestResult> list;
            try {
                String sql = "from TestResult t where  t.testResultType in ('D','M','Q') and t.test = :testId and"
                        + " t.value = :testValue";
                Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);
                query.setParameter("testId", Integer.parseInt(testId));
                query.setParameter("testValue", result);

                list = query.list();

                if (list != null && !list.isEmpty()) {
                    return list.get(0);
                }

            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException(
                        "Error in TestResult getTestResultsByTestAndDictonaryResult(String testId, String" + " result)",
                        e);
            }
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getActiveTestResultsByTest(String testId) throws LIMSRuntimeException {
        List<TestResult> list;
        try {
            String sql = "from TestResult t where  t.test = :testId and t.isActive = true";
            Query<TestResult> query = entityManager.unwrap(Session.class).createQuery(sql, TestResult.class);
            query.setParameter("testId", Integer.parseInt(testId));

            list = query.list();

            return list;
        } catch (RuntimeException e) {
            handleException(e, "getActiveTestResultsByTest");
        }

        return null;
    }
}
