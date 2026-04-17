package org.openelisglobal.analysisqaevent.service;

import org.openelisglobal.analysisqaevent.valueholder.AnalysisQaEvent;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
public interface AnalysisQaEventService extends BaseObjectService<AnalysisQaEvent, String> {
}
