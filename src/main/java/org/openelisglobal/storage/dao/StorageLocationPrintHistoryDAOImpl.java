package org.openelisglobal.storage.dao;

import java.util.UUID;
import org.openelisglobal.common.dao.BaseDAOImpl;
import org.openelisglobal.storage.valueholder.StorageLocationPrintHistory;
import org.springframework.stereotype.Component;

@Component
public class StorageLocationPrintHistoryDAOImpl extends BaseDAOImpl<StorageLocationPrintHistory, UUID>
        implements StorageLocationPrintHistoryDAO {
    
    @Override
    protected Class<StorageLocationPrintHistory> getEntityClass() {
        return StorageLocationPrintHistory.class;
    }
}
