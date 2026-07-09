package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.qaevent.valueholder.NceActionLog;

public interface NceActionLogDAO extends BaseDAO<NceActionLog, Integer> {

    List<NceActionLog> getNceActionLogByNceId(Integer nceId) throws LIMSRuntimeException;
}
