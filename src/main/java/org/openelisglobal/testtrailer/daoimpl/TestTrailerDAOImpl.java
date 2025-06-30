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
package org.openelisglobal.testtrailer.daoimpl;

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
import org.openelisglobal.testtrailer.dao.TestTrailerDAO;
import org.openelisglobal.testtrailer.valueholder.TestTrailer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author diane benz
 */
@Component
@Transactional
public class TestTrailerDAOImpl extends BaseDAOImpl<TestTrailer, String> implements TestTrailerDAO {

    public TestTrailerDAOImpl() {
        super(TestTrailer.class);
    }

    // @Override
    // public void deleteData(List testTrailers) throws LIMSRuntimeException {
    // // add to audit trail
    // try {
    //
    // for (int i = 0; i < testTrailers.size(); i++) {
    // TestTrailer data = (TestTrailer) testTrailers.get(i);
    //
    // TestTrailer oldData = readTestTrailer(data.getId());
    // TestTrailer newData = new TestTrailer();
    //
    // String sysUserId = data.getSysUserId();
    // String event = IActionConstants.AUDIT_TRAIL_DELETE;
    // String tableName = "TEST_TRAILER";
    // auditDAO.saveHistory(newData, oldData, sysUserId, event, tableName);
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "AuditTrail deleteData()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer AuditTrail
    // deleteData()", e);
    // }
    //
    // try {
    // for (int i = 0; i < testTrailers.size(); i++) {
    // TestTrailer data = (TestTrailer) testTrailers.get(i);
    // // bugzilla 2206
    // data = readTestTrailer(data.getId());
    // entityManager.unwrap(Session.class).delete(data);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "deleteData()", e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer deleteData()", e);
    // }
    // }

    // @Override
    // public boolean insertData(TestTrailer testTrailer) throws
    // LIMSRuntimeException {
    // try {
    // // bugzilla 1482 throw Exception if record already exists
    // if (duplicateTestTrailerExists(testTrailer)) {
    // throw new LIMSDuplicateRecordException(
    // "Duplicate record exists for " + testTrailer.getTestTrailerName());
    // }
    //
    // String id = (String) entityManager.unwrap(Session.class).save(testTrailer);
    // testTrailer.setId(id);
    //
    // // bugzilla 1824 inserts will be logged in history table
    //
    // String sysUserId = testTrailer.getSysUserId();
    // String tableName = "TEST_TRAILER";
    // auditDAO.saveNewHistory(testTrailer, sysUserId, tableName);
    //
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "insertData()", e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer insertData()", e);
    // }
    //
    // return true;
    // }
    //
    // @Override
    // public void updateData(TestTrailer testTrailer) throws LIMSRuntimeException {
    // // bugzilla 1482 throw Exception if record already exists
    // try {
    // if (duplicateTestTrailerExists(testTrailer)) {
    // throw new LIMSDuplicateRecordException(
    // "Duplicate record exists for " + testTrailer.getTestTrailerName());
    // }
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "updateData()", e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer updateData()", e);
    // }
    //
    // TestTrailer oldData = readTestTrailer(testTrailer.getId());
    // TestTrailer newData = testTrailer;
    //
    // // add to audit trail
    // try {
    //
    // String sysUserId = testTrailer.getSysUserId();
    // String event = IActionConstants.AUDIT_TRAIL_UPDATE;
    // String tableName = "TEST_TRAILER";
    // auditDAO.saveHistory(newData, oldData, sysUserId, event, tableName);
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "AuditTrail updateData()",
    // e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer AuditTrail
    // updateData()", e);
    // }
    //
    // try {
    // entityManager.unwrap(Session.class).merge(testTrailer);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove old(testTrailer);
    // // entityManager.unwrap(Session.class).refresh // CSL remove
    // old(testTrailer);
    // } catch (RuntimeException e) {
    // // bugzilla 2154
    // LogEvent.logError("TestTrailerDAOImpl", "updateData()", e.toString());
    // throw new LIMSRuntimeException("Error in TestTrailer updateData()", e);
    // }
    // }

    @Override
    @Transactional(readOnly = true)
    public void getData(TestTrailer testTrailer) throws LIMSRuntimeException {
        try {
            TestTrailer uom = entityManager.unwrap(Session.class).get(TestTrailer.class, testTrailer.getId());
            // entityManager.unwrap(Session.class).flush(); // CSL remove old
            // entityManager.unwrap(Session.class).clear(); // CSL remove old
            if (uom != null) {
                PropertyUtils.copyProperties(testTrailer, uom);
            } else {
                testTrailer.setId(null);
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer getData()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getAllTestTrailers() throws LIMSRuntimeException {
        List<TestTrailer> list;
        try {
            String sql = "from TestTrailer";
            Query<TestTrailer> query = entityManager.unwrap(Session.class).createQuery(sql, TestTrailer.class);
            // query.setMaxResults(10);
            // query.setFirstResult(3);
            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer getAllTestTrailers()", e);
        }

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getPageOfTestTrailers(int startingRecNo) throws LIMSRuntimeException {
        List<TestTrailer> list;
        try {
            // calculate maxRow to be one more than the page size
            int endingRecNo = startingRecNo
                    + (Integer.parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"))
                            + 1);

            // bugzilla 1399
            String sql = "from TestTrailer t order by t.testTrailerName";
            Query<TestTrailer> query = entityManager.unwrap(Session.class).createQuery(sql, TestTrailer.class);
            query.setFirstResult(startingRecNo - 1);
            query.setMaxResults(endingRecNo - 1);

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer getPageOfTestTrailers()", e);
        }

        return list;
    }

    public TestTrailer readTestTrailer(String idString) {
        TestTrailer tr = null;
        try {
            tr = entityManager.unwrap(Session.class).get(TestTrailer.class, idString);
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer readTestTrailer()", e);
        }

        return tr;
    }

    @Override
    @Transactional(readOnly = true)
    public TestTrailer getTestTrailerByName(TestTrailer testTrailer) throws LIMSRuntimeException {
        try {
            String sql = "from TestTrailer t where t.testTrailerName = :param";
            Query<TestTrailer> query = entityManager.unwrap(Session.class).createQuery(sql, TestTrailer.class);
            query.setParameter("param", testTrailer.getTestTrailerName());

            List<TestTrailer> list = query.list();
            TestTrailer t = null;
            if (list.size() > 0) {
                t = list.get(0);
            }

            return t;

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer getTestTrailerByName()", e);
        }
    }

    // this is for autocomplete
    @Override
    @Transactional(readOnly = true)
    public List<TestTrailer> getTestTrailers(String filter) throws LIMSRuntimeException {
        List<TestTrailer> list;
        try {
            String sql = "from TestTrailer t where upper(t.testTrailerName) like upper(:param) order by"
                    + " upper(t.testTrailerName)";
            Query<TestTrailer> query = entityManager.unwrap(Session.class).createQuery(sql, TestTrailer.class);
            query.setParameter("param", filter + "%");

            list = query.list();
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in TestTrailer getTestTrailers(String filter)", e);
        }
        return list;
    }

    // bugzilla 1411
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestTrailerCount() throws LIMSRuntimeException {
        return getCount();
    }

    // bugzilla 1482
    @Override
    public boolean duplicateTestTrailerExists(TestTrailer testTrailer) throws LIMSRuntimeException {
        try {

            List<TestTrailer> list = new ArrayList<>();

            // not case sensitive hemolysis and Hemolysis are considered
            // duplicates
            String sql = "from TestTrailer t where trim(lower(t.testTrailerName)) = :param and t.id != :param2";
            Query<TestTrailer> query = entityManager.unwrap(Session.class).createQuery(sql, TestTrailer.class);
            query.setParameter("param", testTrailer.getTestTrailerName().toLowerCase().trim());

            // initialize with 0 (for new records where no id has been generated
            // yet
            String testTrailerId = "0";
            if (!StringUtil.isNullorNill(testTrailer.getId())) {
                testTrailerId = testTrailer.getId();
            }
            query.setParameter("param2", testTrailerId);

            list = query.list();
            if (list.size() > 0) {
                return true;
            } else {
                return false;
            }

        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in duplicateTestTrailerExists()", e);
        }
    }
}
