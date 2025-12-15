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

import java.sql.Timestamp;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.medlab.dao.QualityCheckDAO;
import org.openelisglobal.medlab.valueholder.QualityCheck;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DAO implementation for QualityCheck entity operations.
 */
@Component
@Transactional
public class QualityCheckDAOImpl extends BaseDAOImpl<QualityCheck, Integer> implements QualityCheckDAO {

    public QualityCheckDAOImpl() {
        super(QualityCheck.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityCheck> getQualityChecksBySampleItemId(Integer sampleItemId) {
        try {
            String sql = "FROM QualityCheck qc WHERE qc.sampleItemId = :sampleItemId ORDER BY qc.checkDate DESC";
            Query<QualityCheck> query = entityManager.unwrap(Session.class).createQuery(sql, QualityCheck.class);
            query.setParameter("sampleItemId", sampleItemId);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQualityChecksBySampleItemId()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityCheck> getQualityChecksByStatus(QualityCheck.OverallStatus status) {
        try {
            String sql = "FROM QualityCheck qc WHERE qc.overallStatus = :status ORDER BY qc.checkDate DESC";
            Query<QualityCheck> query = entityManager.unwrap(Session.class).createQuery(sql, QualityCheck.class);
            query.setParameter("status", status);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQualityChecksByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityCheck> getQualityChecksByDateRange(Timestamp startDate, Timestamp endDate) {
        try {
            String sql = "FROM QualityCheck qc WHERE qc.checkDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY qc.checkDate DESC";
            Query<QualityCheck> query = entityManager.unwrap(Session.class).createQuery(sql, QualityCheck.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQualityChecksByDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QualityCheck> getRejectedQualityChecksByDateRange(Timestamp startDate, Timestamp endDate) {
        try {
            String sql = "FROM QualityCheck qc WHERE qc.overallStatus = :status "
                    + "AND qc.checkDate BETWEEN :startDate AND :endDate ORDER BY qc.checkDate DESC";
            Query<QualityCheck> query = entityManager.unwrap(Session.class).createQuery(sql, QualityCheck.class);
            query.setParameter("status", QualityCheck.OverallStatus.REJECTED);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getRejectedQualityChecksByDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QualityCheck getLatestQualityCheckForSample(Integer sampleItemId) {
        try {
            String sql = "FROM QualityCheck qc WHERE qc.sampleItemId = :sampleItemId " + "ORDER BY qc.checkDate DESC";
            Query<QualityCheck> query = entityManager.unwrap(Session.class).createQuery(sql, QualityCheck.class);
            query.setParameter("sampleItemId", sampleItemId);
            query.setMaxResults(1);
            List<QualityCheck> results = query.list();
            return results.isEmpty() ? null : results.get(0);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLatestQualityCheckForSample()", e);
        }
    }
}
