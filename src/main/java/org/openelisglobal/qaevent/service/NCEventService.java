package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NCEventService extends BaseObjectService<NcEvent, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId);
}
