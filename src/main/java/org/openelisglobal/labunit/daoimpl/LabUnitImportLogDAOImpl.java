package org.openelisglobal.labunit.daoimpl;

import jakarta.persistence.Query;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.labunit.dao.LabUnitImportLogDAO;
import org.openelisglobal.labunit.valueholder.LabUnitImportLog;
import org.springframework.stereotype.Repository;

/**
 * DAO implementation for Lab Unit Import Log operations.
 */
@Repository
public class LabUnitImportLogDAOImpl extends BaseDAOImpl<LabUnitImportLog, String> implements LabUnitImportLogDAO {

    public LabUnitImportLogDAOImpl() {
        super(LabUnitImportLog.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitImportLog> getImportLogsByLabUnit(String labUnitId) {
        try {
            Query query = entityManager.createQuery(
                    "FROM LabUnitImportLog WHERE labUnitId = :labUnitId ORDER BY importDate DESC",
                    LabUnitImportLog.class);
            query.setParameter("labUnitId", labUnitId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving import logs by lab unit", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LabUnitImportLog> getRecentImportLogs() {
        try {
            Query query = entityManager.createQuery("FROM LabUnitImportLog ORDER BY importDate DESC",
                    LabUnitImportLog.class);
            query.setMaxResults(10); // Get 10 most recent imports
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving recent import logs", e);
        }
    }

    @Override
    public void createImportLog(LabUnitImportLog importLog) {
        try {
            entityManager.persist(importLog);
        } catch (Exception e) {
            throw new RuntimeException("Error creating import log", e);
        }
    }
}