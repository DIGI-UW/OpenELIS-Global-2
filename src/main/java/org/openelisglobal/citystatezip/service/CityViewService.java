package org.openelisglobal.citystatezip.service;

import org.openelisglobal.citystatezip.valueholder.CityView;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface CityViewService extends BaseObjectService<CityView, String> {
}
