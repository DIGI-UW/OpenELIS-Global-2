package org.openelisglobal.testresultsview.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultsview.valueholder.ClientResultsViewBean;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
public interface ClientResultsViewInfoService extends BaseObjectService<ClientResultsViewBean, Integer> {
}
