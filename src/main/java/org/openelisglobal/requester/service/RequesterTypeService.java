package org.openelisglobal.requester.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.requester.valueholder.RequesterType;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SAMPLE_REQUESTER_VIEW')")
public interface RequesterTypeService extends BaseObjectService<RequesterType, String> {
    RequesterType getRequesterTypeByName(String typeName);
}
