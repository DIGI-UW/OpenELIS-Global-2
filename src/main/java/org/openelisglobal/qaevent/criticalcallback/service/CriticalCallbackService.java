package org.openelisglobal.qaevent.criticalcallback.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackDetailResponse;
import org.openelisglobal.qaevent.criticalcallback.bean.CallbackSummaryResponse;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;

public interface CriticalCallbackService extends BaseObjectService<CriticalCallback, String> {

    List<CriticalCallback> getByAnalysisId(String analysisId);

    /**
     * Compliance summary over released critical results in the window. When the
     * CALLBACK indicator is disabled, returns {@code enabled=false} with zero
     * counts before running any query.
     */
    CallbackSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);

    /**
     * Released critical results in the window with their latest callback attempt
     * (null callback fields = never logged). Empty when the indicator is disabled.
     */
    CallbackDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize);

    /**
     * The subset of the given result ids that already have at least one callback
     * row — the durable backing for the Results-Entry "needs callback" banner.
     */
    List<String> getLoggedResultIds(Collection<String> resultIds);
}
