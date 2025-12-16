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
import org.openelisglobal.medlab.dao.EquipmentUsageLogDAO;
import org.openelisglobal.medlab.valueholder.EquipmentUsageLog;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** DAO implementation for EquipmentUsageLog entity operations. */
@Component
@Transactional
public class EquipmentUsageLogDAOImpl extends BaseDAOImpl<EquipmentUsageLog, Integer> implements EquipmentUsageLogDAO {

    public EquipmentUsageLogDAOImpl() {
        super(EquipmentUsageLog.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageLog> getUsageLogsByAnalyzerAndDateRange(Integer analyzerId, Date startDate,
            Date endDate) {
        try {
            String sql = "FROM EquipmentUsageLog eul WHERE eul.analyzerId = :analyzerId "
                    + "AND eul.usageDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY eul.usageDate DESC, eul.startTime DESC";
            Query<EquipmentUsageLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EquipmentUsageLog.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getUsageLogsByAnalyzerAndDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageLog> getUsageLogsByOperatorAndDateRange(Integer operatorId, Date startDate,
            Date endDate) {
        try {
            String sql = "FROM EquipmentUsageLog eul WHERE eul.operatorId = :operatorId "
                    + "AND eul.usageDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY eul.usageDate DESC, eul.startTime DESC";
            Query<EquipmentUsageLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EquipmentUsageLog.class);
            query.setParameter("operatorId", operatorId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getUsageLogsByOperatorAndDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageLog> getActiveSessions() {
        try {
            String sql = "FROM EquipmentUsageLog eul WHERE eul.endTime IS NULL "
                    + "ORDER BY eul.usageDate DESC, eul.startTime DESC";
            Query<EquipmentUsageLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EquipmentUsageLog.class);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getActiveSessions()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageLog> getErrorLogsByDateRange(Date startDate, Date endDate) {
        try {
            String sql = "FROM EquipmentUsageLog eul WHERE eul.errorOccurred = true "
                    + "AND eul.usageDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY eul.usageDate DESC, eul.startTime DESC";
            Query<EquipmentUsageLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EquipmentUsageLog.class);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getErrorLogsByDateRange()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EquipmentUsageLog> getMaintenanceLogsByAnalyzer(Integer analyzerId, Date startDate, Date endDate) {
        try {
            String sql = "FROM EquipmentUsageLog eul WHERE eul.analyzerId = :analyzerId "
                    + "AND eul.maintenanceDone = true " + "AND eul.usageDate BETWEEN :startDate AND :endDate "
                    + "ORDER BY eul.usageDate DESC, eul.startTime DESC";
            Query<EquipmentUsageLog> query = entityManager.unwrap(Session.class).createQuery(sql,
                    EquipmentUsageLog.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getMaintenanceLogsByAnalyzer()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalSamplesProcessedByAnalyzer(Integer analyzerId, Date startDate, Date endDate) {
        try {
            String sql = "SELECT COALESCE(SUM(eul.samplesProcessed), 0) FROM EquipmentUsageLog eul "
                    + "WHERE eul.analyzerId = :analyzerId " + "AND eul.usageDate BETWEEN :startDate AND :endDate";
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
            query.setParameter("analyzerId", analyzerId);
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in getTotalSamplesProcessedByAnalyzer()", e);
        }
    }
}
