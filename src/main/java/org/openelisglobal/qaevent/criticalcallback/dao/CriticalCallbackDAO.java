package org.openelisglobal.qaevent.criticalcallback.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;

public interface CriticalCallbackDAO extends BaseDAO<CriticalCallback, String> {

    /** All callback attempts for an analysis, newest first. */
    List<CriticalCallback> getByAnalysisId(String analysisId);
}
