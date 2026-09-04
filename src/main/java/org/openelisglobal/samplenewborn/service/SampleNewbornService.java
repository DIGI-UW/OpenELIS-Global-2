package org.openelisglobal.samplenewborn.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.samplenewborn.valueholder.SampleNewborn;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
public interface SampleNewbornService extends BaseObjectService<SampleNewborn, String> {
}
