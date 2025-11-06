package org.openelisglobal.audittrail.dao;

import java.util.List;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public interface HistoryDAO extends BaseDAO<History, String> {

    List<History> getHistoryByRefIdAndRefTableId(String Id, String Table) throws LIMSRuntimeException;

    List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException;
}
