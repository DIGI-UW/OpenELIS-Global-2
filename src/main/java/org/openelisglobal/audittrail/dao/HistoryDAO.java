package org.openelisglobal.audittrail.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public interface HistoryDAO extends BaseDAO<History, String> {

    List<History> getHistoryByRefIdAndRefTableId(String Id, String Table) throws LIMSRuntimeException;

    List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException;

    List<History> getSystemEventHistory(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId, int page, int pageSize)
            throws LIMSRuntimeException;

    long getSystemEventHistoryCount(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId)
            throws LIMSRuntimeException;
}
