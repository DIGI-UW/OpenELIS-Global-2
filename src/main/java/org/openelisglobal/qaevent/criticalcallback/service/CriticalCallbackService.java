package org.openelisglobal.qaevent.criticalcallback.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;

public interface CriticalCallbackService extends BaseObjectService<CriticalCallback, String> {

    List<CriticalCallback> getByAnalysisId(String analysisId);
}
