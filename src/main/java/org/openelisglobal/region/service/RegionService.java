package org.openelisglobal.region.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.region.valueholder.Region;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_PATIENT_VIEW')")
public interface RegionService extends BaseObjectService<Region, String> {
}
