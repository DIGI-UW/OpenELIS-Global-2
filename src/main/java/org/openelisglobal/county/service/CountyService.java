package org.openelisglobal.county.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.county.valueholder.County;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface CountyService extends BaseObjectService<County, String> {
}
