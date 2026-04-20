package org.openelisglobal.qaevent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.qaevent.valueholder.NcEvent;

public interface NCEventService extends BaseObjectService<NcEvent, Integer> {

    List<NcEvent> findByNCENumberOrLabOrderId(String nceNumber, String labOrderId);
}
