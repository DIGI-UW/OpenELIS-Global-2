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
package org.openelisglobal.medlab.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.medlab.dao.MedLabTestRequirementsDAO;
import org.openelisglobal.medlab.valueholder.MedLabTestRequirements;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for MedLabTestRequirements entity operations.
 */
@Component
@Transactional
public class MedLabTestRequirementsDAOImpl extends BaseDAOImpl<MedLabTestRequirements, Integer>
        implements MedLabTestRequirementsDAO {

    public MedLabTestRequirementsDAOImpl() {
        super(MedLabTestRequirements.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByTestId(Integer testId) {
        try {
            String sql = "FROM MedLabTestRequirements r WHERE r.testId = :testId AND r.isActive = true";
            Query<MedLabTestRequirements> query = entityManager.unwrap(Session.class).createQuery(sql,
                    MedLabTestRequirements.class);
            query.setParameter("testId", testId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getRequirementsByTestId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedLabTestRequirements getRequirementsByTestAndSampleType(Integer testId, Integer typeOfSampleId) {
        try {
            String sql = "FROM MedLabTestRequirements r WHERE r.testId = :testId "
                    + "AND r.typeOfSampleId = :typeOfSampleId AND r.isActive = true";
            Query<MedLabTestRequirements> query = entityManager.unwrap(Session.class).createQuery(sql,
                    MedLabTestRequirements.class);
            query.setParameter("testId", testId);
            query.setParameter("typeOfSampleId", typeOfSampleId);
            List<MedLabTestRequirements> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getRequirementsByTestAndSampleType()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getActiveRequirements() {
        try {
            String sql = "FROM MedLabTestRequirements r WHERE r.isActive = true ORDER BY r.testId";
            Query<MedLabTestRequirements> query = entityManager.unwrap(Session.class).createQuery(sql,
                    MedLabTestRequirements.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getActiveRequirements()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByTestIds(List<Integer> testIds) {
        if (testIds == null || testIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String sql = "FROM MedLabTestRequirements r WHERE r.testId IN :testIds AND r.isActive = true";
            Query<MedLabTestRequirements> query = entityManager.unwrap(Session.class).createQuery(sql,
                    MedLabTestRequirements.class);
            query.setParameterList("testIds", testIds);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getRequirementsByTestIds()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedLabTestRequirements> getRequirementsByDepartment(Integer departmentId) {
        try {
            String sql = "FROM MedLabTestRequirements r WHERE r.departmentId = :departmentId AND r.isActive = true";
            Query<MedLabTestRequirements> query = entityManager.unwrap(Session.class).createQuery(sql,
                    MedLabTestRequirements.class);
            query.setParameter("departmentId", departmentId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getRequirementsByDepartment()", e);
        }
    }
}
