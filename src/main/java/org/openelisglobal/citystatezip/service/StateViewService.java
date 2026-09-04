package org.openelisglobal.citystatezip.service;

import org.openelisglobal.citystatezip.valueholder.StateView;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface StateViewService extends BaseObjectService<StateView, String> {
}
