package org.openelisglobal.sampletracking.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampletracking.valueholder.SampleTracking;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
public interface SampleTrackingService extends BaseObjectService<SampleTracking, String> {
}
