package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.labunit.valueholder.LabUnitImportLog;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for Lab Unit Import Log operations.
 */
public interface LabUnitImportLogDAO extends BaseDAO<LabUnitImportLog, String> {

    List<LabUnitImportLog> getImportLogsByLabUnit(String labUnitId);

    List<LabUnitImportLog> getRecentImportLogs();

    void createImportLog(LabUnitImportLog importLog);
}