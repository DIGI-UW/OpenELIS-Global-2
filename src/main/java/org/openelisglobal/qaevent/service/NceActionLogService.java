package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NceActionLogService extends BaseObjectService<NceActionLog, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NCE_VIEW')")
    List<NceActionLog> getNceActionLogByNceId(Integer nceId) throws LIMSRuntimeException;
}
