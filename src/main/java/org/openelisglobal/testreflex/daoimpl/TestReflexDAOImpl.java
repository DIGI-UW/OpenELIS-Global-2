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
package org.openelisglobal.testreflex.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.dao.TestReflexDAO;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz 11/17/2007 instead of printing StackTrace log error
 */
@Component
@Transactional
public class TestReflexDAOImpl extends BaseDAOImpl<TestReflex, String> implements TestReflexDAO {

    public TestReflexDAOImpl() {
        super(TestReflex.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestReflex testReflex) throws LIMSRuntimeException {
        try {
            TestReflex tr = entityManager.unwrap(Session.class).get(TestReflex.class, testReflex.getId());
            if (tr != null) {
                PropertyUtils.copyProperties(testReflex, tr);
            } else {
                testReflex.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getAllTestReflexs() throws LIMSRuntimeException {
        List<TestReflex> list = null;
        try {
            String sql = "from TestReflex t order by t.testAnalyte.analyte.analyteName";
            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            // query.setMaxResults(10);
            // query.setFirstResult(3);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getAllTestReflexs()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getPageOfTestReflexs(int startingRecNo) throws LIMSRuntimeException {
        List<TestReflex> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399 - still need to figure out how to sort (3rd sort
            // column) for dictionary values - requires further step of getting
            // value from dictionary table before sorting
            String sql = "from TestReflex t order by t.testAnalyte.analyte.analyteName";
            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getPageOfTestReflexs()", e);
        }

        return list;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.openelisglobal.testreflex.dao.TestReflexDAO#
     * getTestReflexesByTestResult
     * (org.openelisglobal.testreflex.valueholder.TestReflex,
     * org.openelisglobal.testresult.valueholder.TestResult)
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexesByTestResult(TestResult testResult) throws LIMSRuntimeException {
        try {
            String sql = "from TestReflex t where t.testResult.id = :testResultId";
            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            query.setParameter("testResultId", Integer.parseInt(testResult.getId()));

            List<TestReflex> list = query.list();

            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getTestReflexesByTestResult(TestResult testResult)", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.openelisglobal.testreflex.dao.TestReflexDAO#
     * getTestReflexesByTestResult
     * (org.openelisglobal.testreflex.valueholder.TestReflex,
     * org.openelisglobal.testresult.valueholder.TestResult)
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexesByTestResultAndTestAnalyte(TestResult testResult, TestAnalyte testAnalyte)
            throws LIMSRuntimeException {
        try {
            // bugzilla 1404 testResultId is mapped as testResult.id now
            String sql = "from TestReflex t where t.testResult.id = :param and t.testAnalyte.id = :param2";
            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            query.setParameter("param", testResult.getId());
            query.setParameter("param2", testAnalyte.getId());

            List<TestReflex> list = query.list();

            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getTestReflexesByTestResult(TestResult testResult)", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.openelisglobal.testreflex.dao.TestReflexDAO#
     * getTestReflexesByTestResult
     * (org.openelisglobal.testreflex.valueholder.TestReflex,
     * org.openelisglobal.testresult.valueholder.TestResult) bugzilla 1798 find out
     * if a test with a parent was reflexed or linked
     */
    @Override
    public boolean isReflexedTest(Analysis analysis) throws LIMSRuntimeException {
        try {
            List<TestReflex> list = null;

            if (analysis.getParentAnalysis() != null && analysis.getParentResult() != null) {
                String sql = "from TestReflex t where t.testResult.id = :param and t.testAnalyte.analyte.id ="
                        + " :param2 and t.test.id = :param3 and t.addedTest.id = :param4";
                Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("param", analysis.getParentResult().getTestResult().getId());
                query.setParameter("param2", analysis.getParentResult().getAnalyte().getId());
                query.setParameter("param3", analysis.getParentAnalysis().getTest().getId());
                query.setParameter("param4", analysis.getTest().getId());

                list = query.list();
            }

            return list != null && list.size() > 0;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex getTestReflexesByTestResult(TestResult testResult)", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexsByTestResultAnalyteTest(String testResultId, String analyteId, String testId)
            throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(testResultId) && !GenericValidator.isBlankOrNull(analyteId)
                && !GenericValidator.isBlankOrNull(testId)) {
            try {
                List<TestReflex> list = null;

                String sql = "from TestReflex t where t.testResult.id = :testResultId and t.testAnalyte.analyte.id ="
                        + " :analyteId and t.test.id = :testId";
                Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("testResultId", Integer.parseInt(testResultId));
                query.setParameter("analyteId", Integer.parseInt(analyteId));
                query.setParameter("testId", Integer.parseInt(testId));

                list = query.list();

                return list;
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException(
                        "Error in TestReflex getTestReflexByTestResultAnalyteTest(String testResultId, String"
                                + " analyteId, String testId)",
                        e);
            }
        }
        return new ArrayList<>();
    }

    public TestReflex readTestReflex(String idString) {
        TestReflex tr = null;
        try {
            tr = entityManager.unwrap(Session.class).get(TestReflex.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestReflex readTestReflex()", e);
        }
        return tr;
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestReflexCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 1482
    @Override
    public boolean duplicateTestReflexExists(TestReflex testReflex) throws LIMSRuntimeException {
        try {

            List<TestReflex> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from TestReflex t where t.test.localizedTestName = :localizedTestNameId and "
                    + "trim(lower(t.testAnalyte.analyte.analyteName)) = :analyteName and "
                    + "t.testResult.id = :resultId and " + "t.addedTest.localizedTestName = :addedTestNameId "
                    + "and t.id != :testId";

            if (testReflex.getActionScriptlet() != null) {
                sql = sql + " or trim(lower(t.actionScriptlet.scriptletName)) = :scriptletName ";
            }
            if (testReflex.getRelation() != null) {
                sql = sql + " and t.relation = :relation";
            }

            if (testReflex.getNonDictionaryValue() != null) {
                sql = sql + " and t.nonDictionaryValue = :nonDictionaryValue";
            }

            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            query.setParameter("localizedTestNameId",
                    Integer.parseInt(testReflex.getTest().getLocalizedTestName().getId()));
            query.setParameter("analyteName",
                    testReflex.getTestAnalyte().getAnalyte().getAnalyteName().toLowerCase().trim());
            query.setParameter("resultId", Integer.parseInt(testReflex.getTestResult().getId()));
            query.setParameter("addedTestNameId", testReflex.getAddedTest() == null ? -1
                    : Integer.parseInt(testReflex.getAddedTest().getLocalizedTestName().getId()));

            String testReflexId = StringUtil.isNullorNill(testReflex.getId()) ? "0" : testReflex.getId();

            if (testReflex.getActionScriptlet() != null) {
                query.setParameter("scriptletName",
                        testReflex.getActionScriptlet().getScriptletName().toLowerCase().trim());
            }
            if (testReflex.getRelation() != null) {
                query.setParameter("relation", testReflex.getRelation().toString());
            }

            if (testReflex.getNonDictionaryValue() != null) {
                query.setParameter("nonDictionaryValue", testReflex.getNonDictionaryValue());
            }
            query.setParameter("testId", Integer.parseInt(testReflexId));

            list = query.list();

            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateTestReflexExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getTestReflexsByTestAndFlag(String testId, String flag) throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(testId)) {
            return new ArrayList<>();
        }

        List<TestReflex> reflexList = null;

        try {
            Query<TestReflex> query;
            if (GenericValidator.isBlankOrNull(flag)) {
                String sql = "from TestReflex tr where tr.testResult.test.id = :id";
                query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("id", Integer.parseInt(testId));
            } else {
                String sql = "from TestReflex tr where tr.testResult.test.id = :id and tr.flags = :flag";
                query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("id", Integer.parseInt(testId));
                query.setParameter("flag", flag);
            }
            reflexList = query.list();

        } catch (RuntimeException e) {
            handleException(e, "getTestReflexsByTestAndFlag()");
        }

        return reflexList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestReflex> getFlaggedTestReflexesByTestResult(TestResult testResult, String flag)
            throws LIMSRuntimeException {
        if (GenericValidator.isBlankOrNull(flag)) {
            return new ArrayList<>();
        }

        try {
            String sql = "from TestReflex t where t.testResult.id = :testResultId and t.flags = :flag";
            Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
            query.setParameter("testResultId", Integer.parseInt(testResult.getId()));
            query.setParameter("flag", flag);
            List<TestReflex> list = query.list();
            return list;
        } catch (RuntimeException e) {
            handleException(e, "getFlaggedTestReflexesByTestResult");
        }

        return null;
    }

    @Override
    public List<TestReflex> getTestReflexsByAnalyteAndTest(String analyteId, String testId)
            throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(analyteId) && !GenericValidator.isBlankOrNull(testId)) {
            try {
                List<TestReflex> list = null;

                String sql = "from TestReflex t where t.testAnalyte.analyte.id = :analyteId and t.test.id = :testId";
                Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("analyteId", Integer.parseInt(analyteId));
                query.setParameter("testId", Integer.parseInt(testId));

                list = query.list();

                return list;
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException(
                        "Error in TestReflex getTestReflexByTestResultAnalyteTest(String testResultId, String"
                                + " analyteId, String testId)",
                        e);
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId) throws LIMSRuntimeException {
        if (!GenericValidator.isBlankOrNull(testAnalyteId)) {
            try {
                List<TestReflex> list = null;

                String sql = "from TestReflex t where t.testAnalyte.id = :testAnalyteId";
                Query<TestReflex> query = entityManager.unwrap(Session.class).createQuery(sql, TestReflex.class);
                query.setParameter("testAnalyteId", Integer.parseInt(testAnalyteId));
                list = query.list();
                return list;
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error in getTestReflexsByTestAnalyteId(String testAnalyteId)", e);
            }
        }
        return new ArrayList<>();
    }
}
