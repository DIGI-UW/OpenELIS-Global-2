package org.openelisglobal.storage.dao;

import java.util.UUID;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.storage.valueholder.StorageLocationPrintHistory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class StorageLocationPrintHistoryDAOImpl extends BaseDAOImpl<StorageLocationPrintHistory, UUID>
        implements StorageLocationPrintHistoryDAO {

    public StorageLocationPrintHistoryDAOImpl() {
        super(StorageLocationPrintHistory.class);
    }
}
