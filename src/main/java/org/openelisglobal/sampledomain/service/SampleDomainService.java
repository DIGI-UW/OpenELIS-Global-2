package org.openelisglobal.sampledomain.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampledomain.valueholder.SampleDomain;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
public interface SampleDomainService extends BaseObjectService<SampleDomain, String> {
}
