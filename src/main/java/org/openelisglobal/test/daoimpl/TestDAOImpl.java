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
package org.openelisglobal.test.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.stream.Collectors;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TestDAOImpl extends BaseDAOImpl<Test, String> implements TestDAO {

    public TestDAOImpl() {
        super(Test.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Test test) throws LIMSRuntimeException {
        try {
            Test testClone = entityManager.unwrap(Session.class).get(Test.class, test.getId());
            if (testClone != null) {
                PropertyUtils.copyProperties(test, testClone);
            } else {
                test.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTests(boolean onlyTestsFullySetup) throws LIMSRuntimeException {
        List<Test> list = new Vector<>();
        try {
            String sql = "from Test Order by description";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            list = query.list();
            // list = filterOnlyFullSetup(onlyTestsFullySetup, list);

        } catch (RuntimeException e) {
            handleException(e, "getAllTests()");
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllActiveTests(boolean onlyTestsFullySetup) throws LIMSRuntimeException {
        List<Test> list = new Vector<>();
        try {
            String sql = "from Test WHERE is_Active = 'Y' Order by description";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            list = query.list();
            // list = filterOnlyFullSetup(onlyTestsFullySetup, list);

        } catch (RuntimeException e) {
            handleException(e, "getAllActiveTests()");
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllActiveOrderableTests() throws LIMSRuntimeException {
        try {
            String sql = "from Test t WHERE t.isActive = 'Y'  and t.orderable = true Order by t.description";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            List<Test> list = query.list();

            return list;
        } catch (RuntimeException e) {
            handleException(e, "getAllActiveOrderableTests()");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfTests(int startingRecNo) throws LIMSRuntimeException {
        List<Test> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from Test t order by t.testSection.testSectionName";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getPageOfTests()", e);
        }

        return list;
    }

    // bugzilla 2371
    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfSearchedTests(int startingRecNo, String searchString) throws LIMSRuntimeException {
        List<Test> list;
        String wildCard = "*";
        String newSearchStr;
        String sql;

        try {
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);
            int wCdPosition = searchString.indexOf(wildCard);

            if (wCdPosition == -1) // no wild card looking for exact match
            {
                newSearchStr = searchString.toLowerCase().trim();
                sql = "from Test t  where trim(lower (t.description)) = :param  order by"
                        + " t.testSection.testSectionName";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                sql = "from Test t where trim(lower (t.description)) like :param  order by"
                        + " t.testSection.testSectionName";
            }
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", newSearchStr);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getPageOfSearchedTests()", e);
        }

        return list;
    }

    public Test readTest(String idString) {
        Test test;
        try {
            test = entityManager.unwrap(Session.class).get(Test.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test readTest()", e);
        }

        return test;
    }

    // this is for autocomplete
    // bugzilla 2291 added onlyTestsFullySetup
    @Override
    @Transactional(readOnly = true)
    public List<Test> getTests(String filter, boolean onlyTestsFullySetup) throws LIMSRuntimeException {
        List<Test> list;
        try {
            String sql = "from Test t where (upper(t.localizedTestName.english) like upper(:param) or"
                    + " upper(t.localizedTestName.french) like upper(:param)) and t.isActive='Y'";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", filter + "%");
            list = query.list();

            // list = filterOnlyFullSetup(onlyTestsFullySetup, list);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTests(String filter)", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByName(String testName) throws LIMSRuntimeException {
        String sql = "from Test t where (t.localizedTestName.english = :testName or t.localizedTestName.french ="
                + " :testName)";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("testName", testName);

            return query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTestByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException {
        String sql = "from Test t where (t.localizedTestName.english = :testName or t.localizedTestName.french ="
                + " :testName) and t.isActive='Y'";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("testName", testName);

            return query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTestByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getActiveTestsByPanelName(String panelName) throws LIMSRuntimeException {
        String sql = "SELECT t.* from test t JOIN panel_item pi ON t.id = pi.test_id JOIN panel p ON p.id ="
                + " pi.panel_id WHERE p.name = :panelName and t.is_active='Y'";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createNativeQuery(sql, Test.class);
            query.setParameter("panelName", panelName);

            return query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e.toString(), e);
            throw new LIMSRuntimeException("Error in Test getTestByPanelName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException {
        String sql;
        if (Locale.ENGLISH.equals(locale)) {
            sql = "from Test t where t.localizedTestName.english = :testName and t.isActive='Y'";
        } else {
            sql = "from Test t where t.localizedTestName.french = :testName and t.isActive='Y'";
        }
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("testName", testName);

            List<Test> list = query.list();
            Test t = null;

            if (!list.isEmpty()) {
                t = list.get(0);
            }

            return t;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTestByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException {
        String sql;
        if (Locale.ENGLISH.equals(locale)) {
            sql = "from Test t where t.localizedTestName.english = :testName";
        } else {
            sql = "from Test t where t.localizedTestName.french = :testName";
        }
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("testName", testName);

            List<Test> list = query.list();
            Test t = null;

            if (!list.isEmpty()) {
                t = list.get(0);
            }

            return t;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTestByName()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Test getActiveTestById(Integer testId) throws LIMSRuntimeException {
        List<Test> list = null;

        try {
            String sql = "from Test t where t.id = :testId and t.isActive='Y'";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("testId", testId);

            list = query.list();

        } catch (HibernateException e) {
            handleException(e, "getActiveTestById");
        }

        return (list != null && list.size() > 0) ? list.get(0) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestById(Test test) throws LIMSRuntimeException {
        Test returnTest;
        try {
            returnTest = entityManager.unwrap(Session.class).get(Test.class, test.getId());
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Test getTestById()", e);
        }

        return returnTest;
    }

    // this is for selectdropdown
    @Override
    @Transactional(readOnly = true)
    public List<Method> getMethodsByTestSection(String filter) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.testSection = :param";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", filter);

            List<Test> list = query.list();
            List<Method> methods = new ArrayList<>();

            for (int i = 0; i < list.size(); i++) {
                Test t = list.get(i);
                Method method = t.getMethod();
                if (!methods.contains(method)) {
                    methods.add(method);
                }
            }

            return methods;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Method getMethodsByTestSection(String filter)", e);
        }
    }

    // this is for selectdropdown
    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSection(String filter) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.testSection = :param";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", Integer.parseInt(filter));

            List<Test> list = query.list();
            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Method getTestsByTestSection(String filter)", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSectionId(String id) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.testSection.id = :id";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("id", Integer.parseInt(id));

            List<Test> list = query.list();
            return list;

        } catch (RuntimeException e) {
            handleException(e, "getTestsByTestSectionId");
        }

        return null;
    }

    // this is for selectdropdown
    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByMethod(String filter) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.method = :param";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", filter);

            List<Test> list = query.list();
            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Method getTestsByMethod(String filter)", e);
        }
    }

    // this is for selectdropdown
    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSectionAndMethod(String filter, String filter2) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.testSection = :param1 and t.method = :param2";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param1", filter);
            query.setParameter("param2", filter2);

            List<Test> list = query.list();
            return list;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Method getTestsByMethod(String filter)", e);
        }
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 2371
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedTestCount(String searchString) throws LIMSRuntimeException {
        String wildCard = "*";
        String newSearchStr;
        String sql;
        Integer count = null;

        try {
            int wCdPosition = searchString.indexOf(wildCard);
            if (wCdPosition == -1) // no wild card looking for exact match
            {
                newSearchStr = searchString.toLowerCase().trim();
                sql = "select count (*) from Test t  where trim(lower (t.description)) = :param  ";
            } else {
                newSearchStr = searchString.replace(wildCard, "%").toLowerCase().trim();
                sql = "select count (*) from Test t where trim(lower (t.description)) like :param ";
            }
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("param", newSearchStr);

            List<Long> results = query.list();

            if (results != null && results.get(0) != null) {
                if (results.get(0) != null) {
                    count = results.get(0).intValue();
                }
            }

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestDaoImpl getTotalSearchedTestCount()", e);
        }

        return count;
    }

    @Override
    public boolean duplicateTestExists(Test test) throws LIMSRuntimeException {
        try {

            List<Test> list = new ArrayList();

            if (test.getIsActive().equalsIgnoreCase("Y")) {
                // not case sensitive hemolysis and Hemolysis are considered
                // duplicates
                String sql = "from Test t where (trim(lower(t.description)) = :description and t.isActive='Y' and"
                        + " t.id != :testId)";
                Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);

                // initialize with 0 (for new records where no id has been
                // generated yet
                String testId = "0";
                if (!StringUtil.isNullorNill(test.getId())) {
                    testId = test.getId();
                }
                query.setParameter("testId", Integer.parseInt(testId));
                query.setParameter("description", test.getDescription().toLowerCase().trim());

                list = query.list();
            }

            return !list.isEmpty();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateTestExists()", e);
        }
    }

    // bugzilla 2443
    @Override
    @Transactional(readOnly = true)
    public Integer getNextAvailableSortOrderByTestSection(Test test) throws LIMSRuntimeException {
        Integer result = null;

        try {

            List<Test> list;
            Test testWithHighestSortOrder;

            String sql = "from Test t where t.testSection = :param and t.sortOrder is not null order by"
                    + " t.sortOrder desc";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("param", test.getTestSection());

            list = query.list();
            // entityManager.unwrap(Session.class).flush(); // CSL remove old
            if (list.size() > 0) {
                testWithHighestSortOrder = list.get(0);
                if (testWithHighestSortOrder != null
                        && !StringUtil.isNullorNill(testWithHighestSortOrder.getSortOrder())) {
                    result = (Integer.parseInt(testWithHighestSortOrder.getSortOrder()) + 1);
                }
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getNextAvailableSortOrderByTestSection()", e);
        }
        return result;
    }

    /**
     * @see org.openelisglobal.test.dao.TestDAO#getAllOrderBy(java.lang.String) Read
     *      all entities from the database sorted by an appropriate property
     */
    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllOrderBy(String columnName) throws LIMSRuntimeException {
        List<Test> entities;
        try {
            if (!StringUtil.isJavaIdentifier(columnName)) {
                throw new IllegalArgumentException("\"" + columnName + "\" is not valid syntax for a column name");
            }
            // I didn't manage to get a query parameter to be used as a column
            // name to sort by (because ORDER BY "my_column" is not valid SQL).
            // so I had to generate the HQL manually, but only after the above
            // check.
            String hql = "from Test t where t.isActive='Y' ORDER BY " + columnName;
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(hql, Test.class);
            entities = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getAllOrderBy()", e);
        }

        return entities;
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestById(String testId) throws LIMSRuntimeException {
        String sql = "From Test t where t.id = :id";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("id", Integer.parseInt(testId));

            Test test = query.uniqueResult();
            return test;
        } catch (HibernateException e) {
            handleException(e, "getTestById");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByDescription(String description) {
        String sql = "From Test t where t.description = :description";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("description", description);

            Test test = query.uniqueResult();
            return test;
        } catch (HibernateException e) {
            handleException(e, "getTestByDescription");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByGUID(String guid) {
        String sql = "From Test t where t.guid = :guid";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("guid", guid);

            Test test = query.uniqueResult();
            return test;
        } catch (HibernateException e) {
            handleException(e, "getTestByGUID");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByLoincCode(String loincCode) {
        if (loincCode == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getTestsByLoincCode", "loincCode is null");
        }
        String sql = "From Test t where t.loinc = :loinc";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("loinc", loincCode);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getTestsByLoincCode");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getActiveTestsByLoinc(String loincCode) {
        String sql = "From Test t where t.loinc = :loinc and t.isActive='Y'";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameter("loinc", loincCode);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getActiveTestsByLoinc");
        }

        return null;
    }

    @Override
    public List<Test> getActiveTestsByLoinc(String[] loincCodes) {
        String sql = "From Test t where t.loinc IN (:loinc) and t.isActive='Y'";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameterList("loinc", loincCodes);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getActiveTestsByLoinc");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTestsByDictionaryResult() {
        String sql = "From Test t where t.id in (select tr.test from TestResult tr where tr.testResultType in"
                + " ('D','M','C')) ORDER BY t.description asc";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getAllTestsByDictionaryResult");
        }
        return null;
    }

    @Override
    public List<Test> getTestsByTestSectionIds(List<Integer> ids) throws LIMSRuntimeException {
        try {
            String sql = "from Test t where t.testSection.id IN (:ids) and t.isActive='Y'";
            Query<Test> query = entityManager.unwrap(Session.class).createQuery(sql, Test.class);
            query.setParameterList("ids", ids);

            List<Test> list = query.list();
            return list;

        } catch (RuntimeException e) {
            handleException(e, "getTestsByTestSectionId");
        }

        return null;
    }

    @Override
    public List<Test> getTbTestByMethod(String method) throws LIMSRuntimeException {
        List<Integer> methodIds = Arrays.asList(method.split(",")).stream().map(e -> Integer.parseInt(e))
                .collect(Collectors.toList());
        String sql = "SELECT t.* From test t JOIN tb_method_test tm ON t.id = tm.test_id where tm.method_id in"
                + " (:method) and t.is_active='Y' ORDER BY t.name";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createNativeQuery(sql, Test.class);
            query.setParameter("method", methodIds);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getTbTestByMethod");
        }

        return null;
    }

    @Override
    public List<Test> getTbTest() throws LIMSRuntimeException {
        String sql = "SELECT t.* From test t JOIN test_section ts ON t.test_section_id = ts.id where"
                + " t.is_active='Y' AND ts.name = 'TB' ORDER BY t.name";
        try {
            Query<Test> query = entityManager.unwrap(Session.class).createNativeQuery(sql, Test.class);
            List<Test> tests = query.list();
            return tests;
        } catch (HibernateException e) {
            handleException(e, "getTbTest");
        }

        return null;
    }

    @Override
    public List<Panel> getTbPanelsByMethod(String method) throws LIMSRuntimeException {
        List<Integer> methodIds = Arrays.asList(method.split(",")).stream().map(e -> Integer.parseInt(e))
                .collect(Collectors.toList());

        String sql = "SELECT p.* From panel p JOIN tb_method_panel tm ON p.id = tm.panel_id where tm.method_id"
                + " in (:method) and p.is_active='Y' ORDER BY p.name";
        try {
            Query<Panel> query = entityManager.unwrap(Session.class).createNativeQuery(sql, Panel.class);
            query.setParameter("method", methodIds);
            List<Panel> panels = query.list();
            return panels;
        } catch (HibernateException e) {
            handleException(e, "getTbPanelByMethod");
        }

        return null;
    }
}
