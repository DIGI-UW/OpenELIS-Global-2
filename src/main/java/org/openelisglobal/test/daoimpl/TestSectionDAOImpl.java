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
package org.openelisglobal.test.daoimpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.test.dao.TestSectionDAO;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TestSectionDAOImpl extends BaseDAOImpl<TestSection, String> implements TestSectionDAO {

    public TestSectionDAOImpl() {
        super(TestSection.class);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestSection testSection) throws LIMSRuntimeException {
        try {
            TestSection testSec = entityManager.unwrap(Session.class).get(TestSection.class, testSection.getId());
            if (testSec != null) {
                PropertyUtils.copyProperties(testSection, testSec);
            } else {
                testSection.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllTestSections() throws LIMSRuntimeException {
        List<TestSection> list = null;
        try {
            String sql = "from TestSection";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            list = query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getAllTestSections()", e);
        }
        return list;
    }

    /**
     * Get all the test sections assigned to this specific user
     *
     * @param sysUserId the user system id
     * @return list of tests
     */
    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllTestSectionsBySysUserId(int sysUserId, List<String> sectionIds)
            throws LIMSRuntimeException {
        List<TestSection> list = new ArrayList<>();

        String sql = "";

        try {
            if (sectionIds.isEmpty()) {
                return list;
            }

            sql = "from TestSection where id in (:ids)";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setParameterList("ids", sectionIds);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getAllTestSectionsBySysUserId()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getPageOfTestSections(int startingRecNo) throws LIMSRuntimeException {
        List<TestSection> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from TestSection t order by t.organization.organizationName, t.testSectionName";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getPageOfTestSections()", e);
        }

        return list;
    }

    public TestSection readTestSection(String idString) {
        TestSection ts = null;
        try {
            ts = entityManager.unwrap(Session.class).get(TestSection.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection readCity()", e);
        }

        return ts;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getTestSections(String filter) throws LIMSRuntimeException {
        List<TestSection> list;
        try {
            String sql = "from TestSection t where upper(t.testSectionName) like upper(:param) order by"
                    + " upper(t.testSectionName)";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setParameter("param", filter + "%");
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getTestSections(String filter)", e);
        }

        return list;
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getTestSectionsBySysUserId(String filter, int sysUserId, List<String> sectionIdList)
            throws LIMSRuntimeException {
        List<TestSection> list = new ArrayList<>();
        String sql = "";

        try {
            if (sectionIdList.size() > 0) {
                sql = "from TestSection t where upper(t.testSectionName) like upper(:param) and t.id in"
                        + " (:sectionIdList) order by upper(t.testSectionName)";
            } else {
                return list;
            }

            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setParameter("param", filter + "%");
            query.setParameterList("sectionIdList", sectionIdList);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getTestSectionsBySysUserId(String filter)", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionByName(TestSection testSection) throws LIMSRuntimeException {
        try {
            String sql = "from TestSection t where t.testSectionName = :param";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setParameter("param", testSection.getTestSectionName());

            List<TestSection> list = query.list();

            if (!list.isEmpty()) {
                return list.get(0);
            }

            return null;

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestSection getTestSectionByName()", e);
        }
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestSectionCount() throws LIMSRuntimeException {
        return getCount();
    }

    @Override
    public boolean duplicateTestSectionExists(TestSection testSection) throws LIMSRuntimeException {
        try {

            String sql = "from TestSection t where trim(lower(t.testSectionName)) = :name and t.id != :id";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);

            query.setParameter("name", testSection.getTestSectionName().toLowerCase().trim());

            String testSectionId = "0";
            if (!StringUtil.isNullorNill(testSection.getId())) {
                testSectionId = testSection.getId();
            }
            query.setParameter("id", Integer.parseInt(testSectionId));

            List<TestSection> list = query.list();

            return !list.isEmpty();

        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateTestSectionExists()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllActiveTestSections() {
        String sql = "from TestSection t where t.isActive = 'Y' order by t.sortOrderInt";

        try {
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);

            List<TestSection> sections = query.list();
            return sections;
        } catch (HibernateException e) {
            handleException(e, "getAllActiveTestSections");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSection> getAllInActiveTestSections() {
        String sql = "from TestSection t where t.isActive = 'N' order by t.sortOrderInt";

        try {
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            List<TestSection> sections = query.list();
            return sections;
        } catch (HibernateException e) {
            handleException(e, "getAllInActiveTestSections");
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionByName(String testSection) throws LIMSRuntimeException {
        try {
            String sql = "from TestSection t where t.testSectionName = :name order by t.sortOrderInt";
            Query<TestSection> query = entityManager.unwrap(Session.class).createQuery(sql, TestSection.class);
            query.setParameter("name", testSection);

            List<TestSection> list = query.list();
            if (!list.isEmpty()) {
                return list.get(0);
            }

        } catch (RuntimeException e) {
            handleException(e, "getTestSectionByName");
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TestSection getTestSectionById(String testSectionId) {
        try {
            TestSection testSection = entityManager.unwrap(Session.class).get(TestSection.class, testSectionId);
            return testSection;
        } catch (HibernateException e) {
            handleException(e, "getTestSectionById");
        }
        return null;
    }
}
