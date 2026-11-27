package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroIncomingItem;

public interface MicroIncomingItemDAO extends BaseDAO<MicroIncomingItem, String> {
    List<MicroIncomingItem> forCase(String caseId);

    MicroIncomingItem findDelivery(String caseId, String testId, String key);

    MicroIncomingItem withCase(String id);
}
