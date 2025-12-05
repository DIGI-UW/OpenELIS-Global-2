package org.openelisglobal.inventory.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.inventory.valueholder.LotStatusHistory;

public interface LotStatusHistoryService extends BaseObjectService<LotStatusHistory, String> {

    List<LotStatusHistory> findByLotId(String lotId);
}
