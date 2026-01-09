package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labunit.valueholder.LabUnitImportLog;

/**
 * DAO interface for Lab Unit Import Log operations.
 */
public interface LabUnitImportLogDAO extends BaseDAO<LabUnitImportLog, String> {

    List<LabUnitImportLog> getImportLogsByLabUnit(String labUnitId);

    List<LabUnitImportLog> getRecentImportLogs();

    void createImportLog(LabUnitImportLog importLog);
}