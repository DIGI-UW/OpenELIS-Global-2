package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.NceHistory;

public interface NceHistoryDAO extends BaseDAO<NceHistory, Integer> {

    List<NceHistory> findByNceId(Integer nceId);
}
