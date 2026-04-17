package org.openelisglobal.datasubmission.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.datasubmission.valueholder.DataResource;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
public interface DataResourceService extends BaseObjectService<DataResource, String> {
}
