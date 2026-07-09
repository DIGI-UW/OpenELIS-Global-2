package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NceActionLog;

public interface NceActionLogService extends BaseObjectService<NceActionLog, Integer> {

    List<NceActionLog> getNceActionLogByNceId(Integer nceId) throws LIMSRuntimeException;
}
