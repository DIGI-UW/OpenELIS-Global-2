package org.openelisglobal.sampleqaeventaction.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleqaeventaction.valueholder.SampleQaEventAction;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
public interface SampleQaEventActionService extends BaseObjectService<SampleQaEventAction, String> {
}
