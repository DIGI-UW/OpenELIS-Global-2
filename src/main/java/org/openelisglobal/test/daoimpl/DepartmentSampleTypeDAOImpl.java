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

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.test.dao.DepartmentSampleTypeDAO;
import org.openelisglobal.test.valueholder.DepartmentSampleType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DepartmentSampleTypeDAOImpl extends BaseDAOImpl<DepartmentSampleType, String>
        implements DepartmentSampleTypeDAO {

    public DepartmentSampleTypeDAOImpl() {
        super(DepartmentSampleType.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSampleType> getByTestSectionId(String testSectionId) throws LIMSRuntimeException {
        String sql = "FROM DepartmentSampleType dst WHERE dst.testSectionId = :testSectionId";
        try {
            Query<DepartmentSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DepartmentSampleType.class);
            query.setParameter("testSectionId", Integer.parseInt(testSectionId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getByTestSectionId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSampleType> getBySampleTypeId(String sampleTypeId) throws LIMSRuntimeException {
        String sql = "FROM DepartmentSampleType dst WHERE dst.sampleTypeId = :sampleTypeId";
        try {
            Query<DepartmentSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DepartmentSampleType.class);
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getBySampleTypeId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSampleType> getActiveByTestSectionId(String testSectionId) throws LIMSRuntimeException {
        String sql = "FROM DepartmentSampleType dst " + "WHERE dst.testSectionId = :testSectionId "
                + "AND dst.isActive = true " + "ORDER BY dst.sortOrder";
        try {
            Query<DepartmentSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DepartmentSampleType.class);
            query.setParameter("testSectionId", Integer.parseInt(testSectionId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getActiveByTestSectionId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentSampleType getByTestSectionAndSampleType(String testSectionId, String sampleTypeId)
            throws LIMSRuntimeException {
        String sql = "FROM DepartmentSampleType dst " + "WHERE dst.testSectionId = :testSectionId "
                + "AND dst.sampleTypeId = :sampleTypeId";
        try {
            Query<DepartmentSampleType> query = entityManager.unwrap(Session.class).createQuery(sql,
                    DepartmentSampleType.class);
            query.setParameter("testSectionId", Integer.parseInt(testSectionId));
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            List<DepartmentSampleType> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getByTestSectionAndSampleType()", e);
        }
    }

    @Override
    @Transactional
    public void deleteAllForTestSection(String testSectionId) throws LIMSRuntimeException {
        String sql = "DELETE FROM DepartmentSampleType dst WHERE dst.testSectionId = :testSectionId";
        try {
            Query<?> query = entityManager.unwrap(Session.class).createQuery(sql);
            query.setParameter("testSectionId", Integer.parseInt(testSectionId));
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO deleteAllForTestSection()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSampleTypeIdsForTestSection(String testSectionId) throws LIMSRuntimeException {
        String sql = "SELECT dst.sampleTypeId FROM DepartmentSampleType dst WHERE dst.testSectionId = :testSectionId";
        try {
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("testSectionId", Integer.parseInt(testSectionId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getSampleTypeIdsForTestSection()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTestSectionIdsForSampleType(String sampleTypeId) throws LIMSRuntimeException {
        String sql = "SELECT dst.testSectionId FROM DepartmentSampleType dst WHERE dst.sampleTypeId = :sampleTypeId";
        try {
            Query<String> query = entityManager.unwrap(Session.class).createQuery(sql, String.class);
            query.setParameter("sampleTypeId", Integer.parseInt(sampleTypeId));
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DepartmentSampleTypeDAO getTestSectionIdsForSampleType()", e);
        }
    }
}
