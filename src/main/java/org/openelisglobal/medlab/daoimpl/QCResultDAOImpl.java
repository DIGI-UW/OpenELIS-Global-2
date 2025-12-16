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

import java.sql.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.medlab.dao.QCResultDAO;
import org.openelisglobal.medlab.valueholder.QCResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** DAO implementation for QCResult entity operations. */
@Component
@Transactional
public class QCResultDAOImpl extends BaseDAOImpl<QCResult, Integer> implements QCResultDAO {

    public QCResultDAOImpl() {
        super(QCResult.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getQCResultsByTestAndDateRange(Integer testId, Date startDate, Date endDate) {
        try {
            String sql = "FROM QCResult qc WHERE qc.testId = :testId "
                    + "AND qc.resultDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY qc.resultDate ASC, qc.resultTime ASC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("testId", testId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQCResultsByTestAndDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getQCResultsByTestAndLevel(Integer testId, QCResult.QCLevel qcLevel) {
        try {
            String sql = "FROM QCResult qc WHERE qc.testId = :testId AND qc.qcLevel = :qcLevel "
                    + "ORDER BY qc.resultDate DESC, qc.resultTime DESC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("testId", testId);
            query.setParameter("qcLevel", qcLevel);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQCResultsByTestAndLevel()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getQCResultsByLotNumber(String lotNumber) {
        try {
            String sql = "FROM QCResult qc WHERE qc.lotNumber = :lotNumber "
                    + "ORDER BY qc.resultDate DESC, qc.resultTime DESC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("lotNumber", lotNumber);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQCResultsByLotNumber()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getFailedQCResultsByDateRange(Date startDate, Date endDate) {
        try {
            String sql = "FROM QCResult qc WHERE qc.passFail = :passFail "
                    + "AND qc.resultDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY qc.resultDate DESC, qc.resultTime DESC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("passFail", QCResult.PassFail.FAIL);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getFailedQCResultsByDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getLatestQCResultsForTest(Integer testId, QCResult.QCLevel qcLevel, int count) {
        try {
            String sql = "FROM QCResult qc WHERE qc.testId = :testId AND qc.qcLevel = :qcLevel "
                    + "ORDER BY qc.resultDate DESC, qc.resultTime DESC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("testId", testId);
            query.setParameter("qcLevel", qcLevel);
            query.setMaxResults(count);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getLatestQCResultsForTest()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getQCResultsByAnalyzerAndDateRange(Integer analyzerId, Date startDate, Date endDate) {
        try {
            String sql = "FROM QCResult qc WHERE qc.analyzerId = :analyzerId "
                    + "AND qc.resultDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY qc.resultDate ASC, qc.resultTime ASC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getQCResultsByAnalyzerAndDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCResult> getCalibrationRecordsByAnalyzer(Integer analyzerId, Date startDate, Date endDate) {
        try {
            String sql = "FROM QCResult qc WHERE qc.analyzerId = :analyzerId " + "AND qc.isCalibration = true "
                    + "AND qc.resultDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY qc.resultDate DESC, qc.resultTime DESC";
            Query<QCResult> query = entityManager.unwrap(Session.class).createQuery(sql, QCResult.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getCalibrationRecordsByAnalyzer()", e);
        }
    }
}
