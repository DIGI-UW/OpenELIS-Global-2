package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.LotStatusHistory;

public interface LotStatusHistoryDAO extends BaseDAO<LotStatusHistory, String> {

    List<LotStatusHistory> findByLotId(String lotId);
}
