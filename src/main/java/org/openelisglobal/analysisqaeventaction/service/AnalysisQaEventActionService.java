package org.openelisglobal.analysisqaeventaction.service;

import org.openelisglobal.analysisqaeventaction.valueholder.AnalysisQaEventAction;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
public interface AnalysisQaEventActionService extends BaseObjectService<AnalysisQaEventAction, String> {
}
