package org.openelisglobal.qaevent.criticalcallback.dao;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;

public interface CriticalCallbackDAO extends BaseDAO<CriticalCallback, String> {

    /** All callback attempts for an analysis, newest first. */
    List<CriticalCallback> getByAnalysisId(String analysisId);

    /** Distinct result ids among the given ones that have a callback row. */
    List<String> getLoggedResultIds(Collection<String> resultIds);
}
