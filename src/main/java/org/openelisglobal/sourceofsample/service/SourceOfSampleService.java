package org.openelisglobal.sourceofsample.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sourceofsample.valueholder.SourceOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
public interface SourceOfSampleService extends BaseObjectService<SourceOfSample, String> {
}
